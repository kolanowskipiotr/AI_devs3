package pl.pko.ai.devs3.s02.e04

import io.circe.Error
import io.circe.syntax.*
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.apache.commons.lang3.StringUtils
import pl.pko.ai.devs3.agent.AgentAI
import pl.pko.ai.devs3.hq.model.HQResponse
import pl.pko.ai.devs3.llm.claude.ai.model.ClaudeResponse
import pl.pko.ai.devs3.llm.claude.ai.service.ClaudeService
import pl.pko.ai.devs3.llm.groq.model.GroqResponse
import pl.pko.ai.devs3.llm.groq.service.GroqService
import sttp.client3
import sttp.client3.*
import sttp.client3.asynchttpclient.monix.AsyncHttpClientMonixBackend
import sttp.client3.circe.{asJson, circeBodySerializer}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import java.nio.file.{Files, Path, Paths}
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

case class FilteringAndSortingAgentAI(lesson: String) extends AgentAI {

  override def endpoints: List[ServerEndpoint[Any, Future]] =
    List(
      endpoint
        .post
        .in("sync" / "agents" / "s02" / "e04" / "run")
        .in(header[String]("claude-ai-api-key"))
        .in(header[String]("groq-ai-api-key"))
        .in(header[String]("hq-api-key"))
        .out(jsonBody[HQResponse])
        .serverLogicSuccess((claudeApiKey, groqApiKey, hqApikey) => Future {
          getDataAndSendToHQ(claudeApiKey, groqApiKey, hqApikey)
        })
    )

  private def getDataAndSendToHQ(claudeApiKey: String, groqApiKey: String, hqApikey: String): HQResponse = {
    val context = Context.empty(claudeApiKey, groqApiKey, hqApikey)

    Right(context)
      .flatMap(transcribeFiles)
      .flatMap(categorizeFiles)
      .flatMap(postReportToHQ)
    match {
      case Left(value) => value match
        case HttpError(body, _) => tryParseHQResponse(body.toString)
        case err => tryParseHQResponse(err.toString)
      case Right(value) => HQResponse(0, value.toString)
    }
  }

  private def postReportToHQ(context: Context): Either[RequestError, HQResponse] =
    AsyncHttpClientMonixBackend.resource()
      .use { backend => postReportToHQRequest(backend, context) }
      .runSyncUnsafe()

  private def postReportToHQRequest(backend: Backend, context: Context): Task[Either[RequestError, HQResponse]] = {
    val requestBody = HQReportRequest(
      task = "kategorie",
      apikey = context.hqApikey,
      answer = {
        val people = context.files
          .filter(_.category.contains("PEOPLE"))
          .map(_.name)
          .sorted
        val hardware = context.files
          .filter(_.category.contains("HARDWARE"))
          .map(_.name)
          .sorted
        FilesReport(people, hardware)
      },
    )
    basicRequest
      .post(uri"https://centrala.ag3nts.org/report")
      .body(requestBody)
      .response(asJson[HQResponse])
      .send(backend)
      .map(response => {
        log.info(s"Send request ${response.request}, Body($requestBody)")
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response.body
      })
  }

  private def categorizeFiles(context: Context): Either[Error, Context] = {
    val categorizedContext = context.copy(files = context.files.map(fileEntry => categorizeFile(context, fileEntry)))
    categorizedContext.files.foreach(file => log.info(s"Categorized file: ${file.name} as ${file.category}"))
    Right(categorizedContext)
  }

  private def categorizeFile(context: Context, fileEntry: FileEntry): FileEntry = {
    val resultStart = """<result>"""
    val resultEnd = """</result>"""
    val prompt =
      s"""
         |You are a military intelligence analyst. Your task is to classify the following text into one of these categories:
         |
         |PEOPLE: ONLY information about suspicious/unknown individuals, specifically:
         |- Suspects who were caught or arrested
         |- Signs of unauthorized presence or footprints
         |- Evidence of suspicious activity by unknown persons
         |- Spotting of suspicious individuals
         |Do NOT include:
         |- Reports about own personnel or teammates
         |- Known friendly forces activities
         |- Routine patrol reports without suspicious findings
         |- General mentions of personnel
         |
         |HARDWARE: ONLY reports about physical hardware malfunctions that were repaired/fixed. This includes:
         |- Physical equipment that failed and was repaired
         |- Mechanical components that malfunctioned and were fixed
         |- Hardware showing degraded performance that was replaced
         |- Physical damage that was repaired
         |- Repairs of equipment that wasn't working according to specifications
         |Must involve ALL of these elements:
         |1. A physical component
         |2. An actual malfunction/failure/degradation
         |3. A repair/replacement that fixed the issue
         |Do NOT include:
         |- Software updates or fixes
         |- System upgrades
         |- Configuration changes
         |- Protocol implementations
         |- Hardware mentions without malfunctions
         |- Preventive measures
         |
         |IRRELEVANT: Everything else, including:
         |- Software updates and patches
         |- System upgrades
         |- Protocol implementations
         |- Configuration changes
         |- Reports about own personnel/team
         |- Routine maintenance
         |- Administrative notes
         |- General communications
         |- Training activities
         |- Weather reports
         |- Any hardware work that isn't fixing a malfunction
         |
         |Rules:
         |- If text contains both suspicious people AND hardware repairs, classify as PEOPLE
         |- Must have ALL three elements for HARDWARE: physical component + malfunction + repair
         |- Software-related issues are always IRRELEVANT, even if they fix performance problems
         |- System updates are always IRRELEVANT, even if they improve hardware performance
         |- When in doubt, classify as IRRELEVANT
         |- Preventive maintenance is IRRELEVANT
         |- Reports about known personnel are IRRELEVANT
         |
         |Text to classify:
         |${fileEntry.content.getOrElse("")}
         |
         |Provide your classification in this format:
         |${resultStart}HARDWARE/PEOPLE/IRRELEVANT$resultEnd
         |followed by a brief justification.
         |""".stripMargin
    AsyncHttpClientMonixBackend.resource()
      .use { backend => ClaudeService.sendPrompt(backend, context.claudeApiKey, prompt) }
      .runSyncUnsafe()
      .body match {
      case Left(response) => fileEntry
      case Right(response) => fileEntry.copy(category = Option(StringUtils.substringBetween(response.textResponse, resultStart, resultEnd)))
    }
  }

  private def transcribeFiles(context: Context): Either[Error, Context] = {
    val directory = Paths.get("src/main/scala/pl/pko/ai/devs3/s02/e04/files")
    val fileEntries = Files.list(directory).iterator().asScala.toList
      .filterNot(context.hasTranscription)
      .map { path =>
        path.toString.split("\\.").lastOption match {
          case Some("txt") => Some(FileEntry(
            name = path.getFileName.toString,
            content = Some(Files.readString(path)),
            category = None
          ))
          case Some("png") => Some(FileEntry(
            name = path.getFileName.toString,
            content = transcribeUsingClaude(context, path),
            category = None
          ))
          case Some("mp3") => Some(FileEntry(
            name = path.getFileName.toString,
            content = transcribeUsingGroq(context, path),
            category = None
          ))
          case _ => None
        }
      }
      .filterNot(_.isEmpty)
      .map(_.get)

    val allTranscriptions = context.files ++ fileEntries
    log.info(allTranscriptions.asJson.toString)
    Right(context.copy(files = allTranscriptions))
  }

  private def transcribeUsingClaude(context: Context, filePath: Path): Option[String] =
    AsyncHttpClientMonixBackend.resource()
      .use { backend => ClaudeService.sendTranscribeImagePrompt(backend, context.claudeApiKey, filePath) }
      .runSyncUnsafe()
      .body match {
      case Left(value) => None
      case Right(value) => Some(value.textResponse)
    }

  private def transcribeUsingGroq(context: Context, filePath: Path): Option[String] =
    AsyncHttpClientMonixBackend.resource()
      .use { backend => GroqService.sendAudioTranscription(backend, context.groqApiKey, filePath) }
      .runSyncUnsafe()
      .body match {
      case Left(value) => None
      case Right(value) => Some(value.text)
    }

}