package pl.pko.ai.devs3.hq.api.agent

import io.circe
import io.circe.Error
import io.circe.generic.auto.*
import io.circe.parser.decode
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import pl.pko.ai.devs3.agent.AgentAI
import pl.pko.ai.devs3.hq.model.{HQRequest, HQResponse}
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

case class HQAPIAgentAI(lesson: String) extends AgentAI {

  override def endpoints: List[ServerEndpoint[Any, Future]] =
    List(
      endpoint
        .post
        .in("sync" / "agents" / "hq-api-agent" / "send-data-to-hq")
        .in(header[String]("hq-api-key"))
        .out(jsonBody[HQResponse])
        .serverLogicSuccess(hqApiKey => Future { 
          getDataAndSendToHQ(hqApiKey) 
        })
    )

  private def getDataAndSendToHQ(hqApiKey: String): HQResponse = {
    val taskName = "POLIGON"

    doGetPoligonData()
      .map(_.split("\n").toList)
      .map(data => HQRequest(taskName, hqApiKey, data))
      .flatMap(doPostPoligonVeryfication) match {
      case Left(value) => value match
        case HttpError(body, _) => tryParseHQResponse(body)
        case err => tryParseHQResponse(err.toString)
      case Right(value) => value
    }
  }

  private def doGetPoligonData(): Either[String, String] =
    AsyncHttpClientMonixBackend.resource()
      .use { getPoligonData }
      .runSyncUnsafe()

  private def doPostPoligonVeryfication(hqRequest: HQRequest): Either[ResponseException[String, Error], HQResponse] = {
    AsyncHttpClientMonixBackend.resource()
      .use { backend => postPoligonVeryfication(backend, hqRequest) }
      .runSyncUnsafe()
  }

  private def postResponseToHQ(backend: SttpBackend[Task, MonixStreams & WebSockets], hqRequest: HQResponse): Task[Either[ResponseException[String, Error], HQResponse]] =
    basicRequest
      .body(hqRequest)
      .post(uri"https://poligon.aidevs.pl/verify")
      .response(asJson[HQResponse])
      .send(backend)
      .map(response => {
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response.body
      })

  private def postPoligonVeryfication(backend: SttpBackend[Task, MonixStreams & WebSockets], hqRequest: HQRequest): Task[Either[ResponseException[String, Error], HQResponse]] =
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

  private def tryParseHQResponse(body: String) =
    decode[HQResponse](body) match {
      case Right(hqResponse) => hqResponse
      case Left(err) =>
        log.error(s"Parsing of error body fail ${err.getMessage}", err)
        HQResponse.systemError
    }

}