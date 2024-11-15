package pl.pko.ai.devs3.s02.e05

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

case class ArticleAnalizerAgentAI(lesson: String) extends AgentAI {

  override def endpoints: List[ServerEndpoint[Any, Future]] =
    List(
      endpoint
        .post
        .in("sync" / "agents" / "s02" / "e05" / "run")
        .in(header[String]("claude-ai-api-key"))
        .in(header[String]("groq-ai-api-key"))
        .in(header[String]("hq-api-key"))
        .out(jsonBody[HQResponse])
        .serverLogicSuccess((claudeApiKey, groqApiKey, hqApikey) => Future {
          getDataAndSendToHQ(claudeApiKey, groqApiKey, hqApikey)
        })
    )

  private def getDataAndSendToHQ(claudeApiKey: String, groqApiKey: String, hqApikey: String): HQResponse = {
    val context = Context(claudeApiKey, groqApiKey, hqApikey)

    Right(context)
      //.flatMap(ctx => transcribeUsingGroq(ctx, Path.of("src/main/scala/pl/pko/ai/devs3/s02/e05/files/rafal_dyktafon.mp3")))
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
      task = "arxiv",
      apikey = context.hqApikey,
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

  private def transcribeUsingGroq(context: Context, filePath: Path): Either[ResponseException[String, Error], GroqResponse] =
    AsyncHttpClientMonixBackend.resource()
      .use { backend => GroqService.sendAudioTranscription(backend, context.groqApiKey, filePath) }
      .runSyncUnsafe()
      .body match {
      case Left(value) => Left(value)
      case Right(value) => Right(value)
    }

}