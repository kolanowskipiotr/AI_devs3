package pl.pko.ai.devs3.hq.api.agent

import io.circe
import io.circe.Error
import io.circe.generic.auto.*
import io.circe.parser.decode
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import pl.pko.ai.devs3.agent.AgentAI
import pl.pko.ai.devs3.hq.model.{HQReportListRequest, HQResponse}
import sttp.capabilities.WebSockets
import sttp.capabilities.monix.MonixStreams
import sttp.client3
import sttp.client3.*
import sttp.client3.asynchttpclient.monix.AsyncHttpClientMonixBackend
import sttp.client3.circe.{asJson, circeBodySerializer}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.Future

case class HQAPIAsyncAgentAI(lesson: String) extends AgentAI {

  override def endpoints: List[ServerEndpoint[Any, Future]] =
    List(
      endpoint
        .post
        .in("async" / "agents" / "hq-api-agent" / "send-data-to-hq")
        .in(header[String]("hq-api-key"))
        .out(jsonBody[HQResponse])
        .serverLogicSuccess(hqApiKey => Future {
          getDataAndSendToHQ(hqApiKey)
        })
    )

  private def getDataAndSendToHQ(hqApiKey: String): HQResponse = {
    val taskName = "POLIGON"

    val task = for {
      data <- doGetPoligonData()
      hqRequest = HQReportListRequest(taskName, hqApiKey, data.split("\n").toList)
      response <- doPostPoligonVeryfication(hqRequest)
    } yield response match {
      case Left(value) => value match {
        case HttpError(body, _) => tryParseHQResponse(body)
        case err => tryParseHQResponse(err.toString)
      }
      case Right(value) => value
    }

    task.runSyncUnsafe()
  }

  private def doGetPoligonData(): Task[String] =
    AsyncHttpClientMonixBackend.resource().use { backend =>
      getPoligonData(backend)
    }.map {
      case Left(error) => throw new RuntimeException(error)
      case Right(data) => data
    }

  private def doPostPoligonVeryfication(hqRequest: HQReportListRequest): Task[Either[ResponseException[String, Error], HQResponse]] =
    AsyncHttpClientMonixBackend.resource().use { backend =>
      postPoligonVeryfication(backend, hqRequest)
    }

  private def postPoligonVeryfication(backend: SttpBackend[Task, MonixStreams & WebSockets], hqRequest: HQReportListRequest): Task[Either[ResponseException[String, Error], HQResponse]] =
    basicRequest
      .body(hqRequest)
      .post(uri"https://poligon.aidevs.pl/verify")
      .response(asJson[HQResponse])
      .send(backend)
      .map(response => {
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response.body
      })

  private def getPoligonData(backend: SttpBackend[Task, MonixStreams & WebSockets]): Task[Either[String, String]] =
    basicRequest
      .get(uri"https://poligon.aidevs.pl/dane.txt")
      .response(asString)
      .send(backend)
      .map { response =>
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response.body
      }
}