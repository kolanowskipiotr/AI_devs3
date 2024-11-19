package pl.pko.ai.devs3.s03.e02

import io.circe.Error
import io.circe.syntax.*
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import pl.pko.ai.devs3.agent.AgentAI
import pl.pko.ai.devs3.db.qdrant.QdrantClient
import pl.pko.ai.devs3.db.qdrant.model.QdrantDocument
import pl.pko.ai.devs3.hq.model.HQResponse
import pl.pko.ai.devs3.llm.jina.ai.JinaClient
import sttp.client3
import sttp.client3.*
import sttp.client3.asynchttpclient.monix.AsyncHttpClientMonixBackend
import sttp.client3.circe.{asJson, circeBodySerializer}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

case class VectorStoreAgentAI(lesson: String) extends AgentAI {

  override def endpoints: List[ServerEndpoint[Any, Future]] =
    List(
      endpoint
        .post
        .in("sync" / "agents" / "s03" / "e02" / "run")
        .in(header[String]("hq-api-key"))
        .in(header[String]("claude-ai-api-key"))
        .in(header[String]("groq-ai-api-key"))
        .in(header[String]("qdrant-ai-api-url"))
        .in(header[String]("qdrant-ai-api-key"))
        .in(header[String]("jina-ai-api-key"))
        .out(jsonBody[HQResponse])
        .serverLogicSuccess((hqApikey, claudeApiKey, groqApiKey, qdrantApiUrl, qdrantApiKey, jinaApiKey) => Future {
          getDataAndSendToHQ(Context(hqApikey, claudeApiKey, groqApiKey, qdrantApiUrl, qdrantApiKey, jinaApiKey))
        })
    )

  private def getDataAndSendToHQ(context: Context): HQResponse = {
    Right(context.copy(question = Some(Question(content = Some("W raporcie, z którego dnia znajduje się wzmianka o kradzieży prototypu broni?")))))
      .flatMap(readFiles)
      .flatMap(calculateEmbeddings)
      .flatMap(storeVectorsInQdrant)
      .flatMap(calculateVectorForQuestion)
      .flatMap(searchVectorDataBase)
      .flatMap(cacheContext)
      .flatMap(postReportToHQ)
    match {
      case Left(value) => value match
        case HttpError(body, _) => tryParseHQResponse(body.toString)
        case err => tryParseHQResponse(err.toString)
      case Right(value) => HQResponse(0, value.toString)
    }
  }

  private def searchVectorDataBase(context: Context): Either[RequestError, Context] = {
    if (context.answer.isDefined) {
      Right(context)
    } else {
      AsyncHttpClientMonixBackend.resource()
        .use { backend =>
          QdrantClient.searchByVector(backend, context.qdrantApiUrl, context.qdrantApiKey, context.question.get.vector.get)
            .map(response => {
              response.body match
                case Left(value) => Left(value)
                case Right(value) =>
                  Right(context.copy(
                    answer = Some(value.result.head.payload("fileName"))
                  ))
            })
        }
        .runSyncUnsafe()
    }
  }

  private def calculateVectorForQuestion(context: Context): Either[RequestError, Context] = {
    if (context.question.flatMap(_.vector).isDefined) {
      Right(context)
    } else {
      AsyncHttpClientMonixBackend.resource()
        .use { backend =>
          JinaClient.calculateEmbedings(backend, context.jinaApiKey, List(context.question.get.content.get))
            .map(response => {
              response.body match
                case Left(value) => Left(value)
                case Right(value) =>
                  Right(context.copy(
                    question = context.question.map(_.copy(vector = Some(value.data.head.embedding))
                    )))
            })
        }
        .runSyncUnsafe()
    }
  }

  private def storeVectorsInQdrant(context: Context): Either[RequestError, Context] = {
    if (context.cached) {
      Right(context)
    } else {
      AsyncHttpClientMonixBackend.resource()
        .use { backend =>
          QdrantClient.putDocuments(backend, context.qdrantApiUrl, context.qdrantApiKey, context.files.map(file => QdrantDocument(
              vector = file.embedding.get,
              payload = Map("fileName" -> file.fileName)
            )))
            .map(response => {
              response.body match
                case Left(value) => Left(value)
                case Right(value) => Right(context)
            })
        }
        .runSyncUnsafe()
    }
  }

  private def cacheContext(context: Context): Either[String, Context] = {
    val directory = Paths.get("src/main/scala/pl/pko/ai/devs3/s03/e02/cache")
    val filePath = directory.resolve("context.json")

    try {
      val secretValue = "SECRET"
      val contextWithoutSensitiveData = context.copy(
        hqApikey = secretValue,
        claudeApiKey = secretValue,
        groqApiKey = secretValue,
        qdrantApiUrl = secretValue,
        qdrantApiKey = secretValue,
        jinaApiKey = secretValue,
      )
      Files.write(
        filePath,
        contextWithoutSensitiveData.asJson.noSpaces.getBytes,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      )
      Right(context)
    } catch {
      case e: Exception => Left(e.getMessage)
    }
  }

  private def calculateEmbeddings(context: Context): Either[RequestError, Context] = {
    if (context.cached) {
      Right(context)
    } else {
      AsyncHttpClientMonixBackend.resource()
        .use { backend =>
          JinaClient.calculateEmbedings(backend, context.jinaApiKey, context.files.map(_.content))
            .map(response => {
              response.body match
                case Left(value) => Left(value)
                case Right(value) =>
                  Right(context.copy(
                    files = context.files.zip(value.data.map(_.embedding)).map {
                      case (file, embedding) => file.copy(embedding = Some(embedding))
                    }
                  ))
            })
        }
        .runSyncUnsafe()
    }
  }

  private def readFiles(context: Context): Either[String, Context] = {
    if (context.cached) {
      Right(context)
    } else {
      val filesPath = Paths.get("src/main/scala/pl/pko/ai/devs3/s03/e02/weapons_tests/do-not-share")
      val files = readFilesFromDirectory(filesPath)
      Right(context.copy(files = files))
    }
  }

  private def readFilesFromDirectory(directory: Path): List[Document] = {
    Files.list(directory).iterator().asScala.toList.map {
        case path if Files.isRegularFile(path) =>
          Some(Document(fileName = path.getFileName.toString, content = new String(Files.readAllBytes(path))))
        case _ => None
      }
      .filter(_.isDefined)
      .map(_.get)
  }

  private def postReportToHQ(context: Context): Either[RequestError, HQResponse] =
    AsyncHttpClientMonixBackend.resource()
      .use { backend => postReportToHQRequest(backend, context) }
      .runSyncUnsafe()

  private def postReportToHQRequest(backend: Backend, context: Context): Task[Either[RequestError, HQResponse]] = {
    val requestBody = HQReportRequest(
      task = "wektory",
      apikey = context.hqApikey,
      answer = context.answer.getOrElse("").replace(".txt", "").replace("_", "-")
    )
    basicRequest
      .post(uri"https://centrala.ag3nts.org/report")
      .body(requestBody)
      .response(asJson[HQResponse])
      .send(backend)
      .map(response => {
        log.info(s"Send request ${response.request}, Body(${requestBody.asJson})")
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response.body
      })
  }

}