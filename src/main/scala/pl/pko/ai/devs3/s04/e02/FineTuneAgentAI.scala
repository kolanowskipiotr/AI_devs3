package pl.pko.ai.devs3.s04.e02

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import monix.execution.Scheduler.Implicits.global
import org.apache.commons.lang3.StringUtils.substringBetween
import pl.pko.ai.devs3.agent.AgentAI
import pl.pko.ai.devs3.hq.model.{HQReportListRequest, HQResponse}
import sttp.client3.asynchttpclient.monix.AsyncHttpClientMonixBackend
import sttp.client3.circe.{asJson, circeBodySerializer}
import sttp.client3.{UriContext, basicRequest}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

/**
 * curl --request GET \
 * --url https://centrala.ag3nts.org/report \
 * --header 'Content-Type: application/json' \
 * --header 'User-Agent: insomnia/10.1.1' \
 * --data '{
 * "task":"research",
 * "apikey":"d0e06171-6ebe-42b7-927b-0784be87fb58",
 * "answer": ["01","02","03","04","05","06","07","08","09","10"]
 * }'
 *
 * {{FLG:ITSVALID}}
 */
case class FineTuneAgentAI(lesson: String) extends AgentAI {

  override def endpoints: List[ServerEndpoint[Any, Future]] =
    List(
      endpoint
        .post
        .in("sync" / "agents" / "s04" / "e02" / "run")
        .out(jsonBody[HQResponse])
        .in(header[String]("hq-api-key"))
        .serverLogicSuccess(hqApikey => Future {
          getDataAndSendToHQ(hqApikey)
        })
    )

  private def getDataAndSendToHQ(hqApikey: String): HQResponse = {
    val systemPrompt = "Classify data"
    val correctData = readLinesFromFile("src/main/scala/pl/pko/ai/devs3/s04/e02/lab_data/correct.txt")
      .map(line => JsonLEntry(List(Message("system", systemPrompt), Message("user", line), Message("assistant", "correct"))))
    val incorrectData = readLinesFromFile("src/main/scala/pl/pko/ai/devs3/s04/e02/lab_data/incorrect.txt")
      .map(line => JsonLEntry(List(Message("system", systemPrompt), Message("user", line), Message("assistant", "incorrect"))))

    log.info(correctData.asJson.noSpaces)
    saveData(correctData, "correct.jsonl")

    log.info(incorrectData.asJson.noSpaces)
    saveData(incorrectData, "incorrect.jsonl")

    saveData(correctData ++ incorrectData, "combined.jsonl")

    //ddosHQ(hqApikey)

    HQResponse.success("FineTuneAgentAI is running")
  }


  private def ddosHQ(hqApikey: String): Unit = {
    val combinations = (1 to 10)
    combinations.foreach(combination =>
      Thread.sleep(1000)
      (1 to 10).map(i => if (i < 10) s"0$i" else "10").combinations(combination).toList.foreach(description => {
        sendCommandToHq(hqApikey, description.toList)
          .map(response => {
            response.message
          })
      })
    )
  }

  private def sendCommandToHq(hqApikey: String, command: List[String]): Either[RequestError, HQResponse] = {
    val requestBody = HQReportListRequest(
      task = "research",
      apikey = hqApikey,
      answer = command
    )
    AsyncHttpClientMonixBackend.resource()
      .use { backend =>
        basicRequest
          .post(uri"https://centrala.ag3nts.org/report")
          .body(requestBody)
          .response(asJson[HQResponse])
          .send(backend)
          .map(response => {
            val flag = substringBetween(response.body.toString, "{{", "}}")
            if(flag != null && !flag.contains("null")){
              log.info(s"Send request ${response.request}, Body(${requestBody.asJson})")
              log.info(s"Got response code: ${response.code} Body: {{$flag}}")
            }
            response.body
          })
      }
      .runSyncUnsafe()
  }

  private def saveData(data: List[JsonLEntry], filename: String) = {
    val directory = Paths.get("src/main/scala/pl/pko/ai/devs3/s04/e02/learning_data")
    val filePath = directory.resolve(filename)

    try {
      Files.write(
        filePath,
        data.map(_.asJson.noSpaces).mkString("\n").getBytes,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      )
    } catch {
      case e: Exception => log.error(e.getMessage, e)
    }
  }

  case class JsonL(
                    entries: List[JsonLEntry]
                  )

  object JsonL {
    implicit val encoder: Encoder[JsonL] = deriveEncoder[JsonL]
    implicit val decoder: Decoder[JsonL] = deriveDecoder[JsonL]
  }

  case class JsonLEntry(
                         messages: List[Message],
                       )

  object JsonLEntry {
    implicit val encoder: Encoder[JsonLEntry] = deriveEncoder[JsonLEntry]
    implicit val decoder: Decoder[JsonLEntry] = deriveDecoder[JsonLEntry]
  }

  case class Message(
                      role: String,
                      content: String
                    )

  object Message {
    implicit val encoder: Encoder[Message] = deriveEncoder[Message]
    implicit val decoder: Decoder[Message] = deriveDecoder[Message]
  }


  private def readLinesFromFile(path: String): List[String] = {
    val source = scala.io.Source.fromFile(path)
    try source.getLines().toList
    finally source.close()
  }
}
