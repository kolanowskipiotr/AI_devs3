package pl.pko.ai.devs3.s04.e01

import io.circe.Error
import io.circe.parser.decode
import io.circe.syntax.*
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.substringBetween
import pl.pko.ai.devs3.agent.AgentAI
import pl.pko.ai.devs3.hq.model.{HQReportRequest, HQResponse}
import pl.pko.ai.devs3.llm.claude.ai.service.ClaudeService
import sttp.client3
import sttp.client3.*
import sttp.client3.asynchttpclient.monix.AsyncHttpClientMonixBackend
import sttp.client3.circe.{asJson, circeBodySerializer}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import java.nio.file.{Files, Paths, StandardOpenOption}
import java.util.Base64
import scala.annotation.tailrec
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.util.Right

/**
 * {{FLG:USEFULCLUE}}
 * {{FLG:TALKTOME}}
 */
case class BarbaraDescriptionAgentAI(lesson: String) extends AgentAI {

  override def endpoints: List[ServerEndpoint[Any, Future]] =
    List(
      endpoint
        .post
        .in("sync" / "agents" / "s04" / "e01" / "run")
        .in(header[String]("hq-api-key"))
        .in(header[String]("claude-ai-api-key"))
        .in(header[String]("groq-ai-api-key"))
        .in(header[String]("qdrant-ai-api-url"))
        .in(header[String]("qdrant-ai-api-key"))
        .in(header[String]("jina-ai-api-key"))
        .in(header[String]("neo4j-uri"))
        .in(header[String]("neo4j-user"))
        .in(header[String]("neo4j-password"))
        .out(jsonBody[HQResponse])
        .serverLogicSuccess((
                              hqApikey,
                              claudeApiKey,
                              groqApiKey,
                              qdrantApiUrl,
                              qdrantApiKey,
                              jinaApiKey,
                              neo4jUri,
                              neo4jUser,
                              neo4jPassword,
                            ) => Future {
          getDataAndSendToHQ(Context(
            hqApikey,
            claudeApiKey,
            groqApiKey,
            qdrantApiUrl,
            qdrantApiKey,
            jinaApiKey,
            neo4jUri,
            neo4jUser,
            neo4jPassword,
          ))
        })
    )

  private def getDataAndSendToHQ(context: Context): HQResponse = {
    Right(context)
      .flatMap(getStartData)
      .flatMap(ctx => extractFiles(ctx, ctx.startResponse))
      .flatMap(fixPhotos)
      .flatMap(createPeopleDescriptions)
      .flatMap(cacheContext)
      .flatMap(postAllReportToHQ)
    match {
      case Left(value) => value match
        case HttpError(body, _) => tryParseHQResponse(body.toString)
        case err => tryParseHQResponse(err.toString)
      case Right(value) => HQResponse(0, value.toString)
    }
  }

  private def createPeopleDescriptions(context: Context): Either[String, Context] = {
    val peopleDescriptions = context.files
      .map(file => {
        createPersonDescription(context, file)
      })
      .filter(_.isDefined)
      .map(_.get)

    Right(context.copy(
      descriptionsOfAllPeople = peopleDescriptions.flatMap(_.descriptionsOfAllPeople)
    ))
  }

  private def createPersonDescription(context: Context, file: File): Option[PeopleDescription] = {
    val systemPrompt =
      s"""
         |Przygotuj rysopisy wszystkich osób na zdjęciu w języku polskim. Skup się na znakach szczególnych. Opisz każdą osobę osobno.
         |Zduplikuj informacje zamiast podawać wspólną sekcję. Opisz każdą osobę osobno.
         |Zwracaj tylko informacje które jesteś w stanie ustalić.
         |Pomijaj informacje o elementach których nie można ustalić.
         |Używaj bazowych kolorów dla włsów takich jak: Blond, czarne, rude, etc.
         |Odpowiedź podaj w formie listy oddzielonej znakami spacji.
         |Skup się na znakach szczególnych.
         |Odpowiedź zwróci w formacie json:
         |{
         |	"_chainOfthoughts": "Opisz łańcucha swoich myśli",
         |	"descriptionsOfAllPeople": ["Opis pierwszej osoby na zdjęciu. Odzież informacje od przecinkami", "Opis kolejnej osoby"]
         |}
         |""".stripMargin
    AsyncHttpClientMonixBackend.resource()
      .use { backend => ClaudeService.sendImageAnalisePrompt(backend, context.claudeApiKey, file.base64, "PNG", systemPrompt) }
      .runSyncUnsafe()
      .body match {
      case Left(error) =>
        None
      case Right(response) =>
        decode[PeopleDescription](response.textResponse) match {
          case Right(description) =>
            Some(description)
          case Left(error) =>
            None
        }

    }
  }

  @tailrec
  private def fixPhotos(context: Context): Either[String, Context] = {
    val fileToAnalise: Option[File] = context.files
      .filter(file => file.isValidImage)
      .find(file => !file.fixesDeterminate)

    if (fileToAnalise.isDefined) {
      fixPhotos(context.updateFile(analizeFile(context, fileToAnalise.get)))
    } else {
      val fileToFix: Option[File] = context.files
        .filter(file => file.isValidImage)
        .find(file => !file.allFixesApplied)

      if (fileToFix.isDefined) {
        fixPhotos(applyFixes(context, fileToFix.get))
      } else {
        Right(context)
      }
    }
  }

  private def applyFixes(context: Context, file: File): Context = {
    val fixToApply = file.nextFix
    sendCommandToHq(context, s"$fixToApply ${file.name}").map { response =>
      extractFiles(context, Some(response.message))
    } match {
      case Left(_) =>
        context
      case Right(Left(_)) =>
        context
      case Right(Right(ctx)) =>
        ctx.updateFile(file.copy(fixesApplied = file.fixesApplied :+ fixToApply))
    }
  }

  private def analizeFile(context: Context, file: File): File = {
    val systemPrompt =
      s"""
         |Analyze the image and chose how to fix it to make it more readable.
         |<rules>
         |1. Available fixes are
         |- REPAIR = repairing a photo containing noise/glitches
         |- BRIGHTEN = brightening up a photo
         |- DARKEN = darkening a photo
         |- NONE = no fixes needed
         |2. Chose only fixes names
         |</rules>
         |
         |Response with tagged result like this:
         |<_chainOfThoughts>Put here your chain of thoughts</_chainOfThoughts>
         |<fixNames>Put here name of fixes that nied to be applied to the image, separated by semicolon characters ;</fixNames>
         |""".stripMargin
    AsyncHttpClientMonixBackend.resource()
      .use { backend => ClaudeService.sendImageAnalisePrompt(backend, context.claudeApiKey, file.base64, "PNG", systemPrompt) }
      .runSyncUnsafe()
      .body match {
      case Left(error) =>
        file.copy(fixesDeterminate = true)
      case Right(response) =>
        file.copy(
          fixesDeterminate = true,
          fixesToApply = extractValuesInTag(response.textResponse, "fixNames"),
        )
    }
  }

  private def extractFiles(context: Context, contentToAnalize: Option[String]): Either[String, Context] = {
    if (context.cached) {
      Right(context)
    } else {
      val prompt =
        s"""
           |Extract filenames from <content to extract file names> using various patterns.
           |Handles different filename formats including:
           |- Simple filenames (example.txt)
           |- Filenames with multiple extensions (archive.tar.gz)
           |- Path-like filenames (/path/to/file.txt or C:\\path\\to\\file.txt)
           |- Url-like filenames (https://centrala.ag3nts.org/dane/barbara/IMG_1444.PNG)
           |- Filenames with spaces and special characters
           |
           |<examples>
           |User: https://centrala.ag3nts.org/dane/barbara/IMG_1444.PNG
           |AI: <fileNames>IMG_1444.PNG</fileNames>
           |<examples>
           |
           |<content to extract file names>
           |${contentToAnalize.getOrElse("")}
           |</content to extract file names>
           |
           |Response with tagged result like this:
           |<_chainOfThoughts>Put here your chain of thoughts</_chainOfThoughts>
           |<fileNames>Put here your found filenames separated by semicolon characters ;</fileNames>
           |""".stripMargin
      AsyncHttpClientMonixBackend.resource()
        .use { backend => ClaudeService.sendPrompt(backend, context.claudeApiKey, prompt) }
        .runSyncUnsafe()
        .body match {
        case Left(error) => Left(error.toString)
        case Right(response) =>
          Right(
            context.addFiles(extractValuesInTag(response.textResponse, "fileNames")
              .map(fileName => File(
                name = fileName,
                base64 = imageUrlToBase64(fileName),
                fixesToApply = List.empty,
                fixesApplied = List.empty,
              ))
            ))
      }
    }
  }

  private def imageUrlToBase64(imageName: String): Option[String] = {
    val backend = HttpURLConnectionBackend()
    val response = basicRequest
      .get(uri"""https://centrala.ag3nts.org/dane/barbara/$imageName""")
      .response(asByteArray)
      .send(backend)

    response.body match {
      case Right(imageBytes) =>
        val base64String = Base64.getEncoder.encodeToString(imageBytes)
        Some(base64String)
      case Left(error) =>
        None
    }
  }

  private def extractValuesInTag(str: String, poiType: String): List[String] = {
    substringBetween(str, s"<$poiType>", s"</$poiType>")
      .replace("\n", "")
      .split(";")
      .filter(StringUtils.isNotBlank)
      .toList
  }

  private def getStartData(context: Context): Either[RequestError, Context] = {
    if (context.startResponse.isDefined) {
      Right(context)
    } else {
      sendCommandToHq(context, "START")
        .map(response => {
          context.copy(startResponse = Some(response.message))
        })
    }
  }

  private def cacheContext(context: Context): Either[String, Context] = {
    val directory = Paths.get("src/main/scala/pl/pko/ai/devs3/s04/e01/cache")
    val filePath = directory.resolve("context.json")

    try {
      Files.write(
        filePath,
        context.anonimized.asJson.noSpaces.getBytes,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      )
      Right(context)
    } catch {
      case e: Exception => Left(e.getMessage)
    }
  }

  private def postAllReportToHQ(context: Context): Either[String, Context] = {
    val responseMessages = context.descriptionsOfAllPeople.map(description => {
      sendCommandToHq(context, description)
        .map(response => {
          response.message
        }) match {
        case Left(value) => value.toString
        case Right(value) => value
      }
    })
    Right(context.copy(responseMessages = responseMessages).anonimized.replaceFilesContentWithPlaceholders)
  }

  private def sendCommandToHq(context: Context, command: String): Either[RequestError, HQResponse] = {
    val requestBody = HQReportRequest(
      task = "photos",
      apikey = context.hqApikey,
      answer = Some(command)
    )
    AsyncHttpClientMonixBackend.resource()
      .use { backend =>
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
      .runSyncUnsafe()
  }
}
