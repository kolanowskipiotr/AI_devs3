package pl.pko.ai.devs3
package domain.hq.api.agent

import domain.{Agent, HQRequest, HQResponse}

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.slf4j.LoggerFactory
import sttp.capabilities.WebSockets
import sttp.capabilities.monix.MonixStreams
import sttp.client3
import sttp.client3.asynchttpclient.monix.AsyncHttpClientMonixBackend
import sttp.client3.circe.asJson
import sttp.client3.{HttpError, Response, SttpBackend, UriContext, asString, basicRequest}
import sttp.shared.Identity
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.netty.sync.OxStreams
import io.circe.parser.decode
import io.circe.generic.auto.*

case class HQAPIAgent(lesson: String) extends Agent {

  private val logger = LoggerFactory.getLogger(getClass)

  override def endpoints: List[ServerEndpoint[OxStreams & WebSockets, Identity]] = {
    List(
      endpoint
        .post
        .in("agents" / "hq-api-agent" / "send-data-to-hq")
        .in(header[String]("hq-api-key"))
        .out(jsonBody[HQResponse])
        .handleSuccess(getDataAndSendToHQ)
    )
  }

  private def getDataAndSendToHQ(hqApiKey: String): HQResponse = {
    val taskName = "POLIGON"

    AsyncHttpClientMonixBackend.resource()
      .use {
        getPoligonData
      }
      .runSyncUnsafe()
      .map(data => data.split("\n").toList)
      .map(data => HQRequest(taskName, hqApiKey, data))
      .map(hqRequest => sendDataToHQ(hqRequest))
      .fold(
        err => {
          logger.error(err)
          HQResponse.systemError
        },
        identity
      )
  }

  private def sendDataToHQ(hqRequest: HQRequest): HQResponse = {

    import sttp.client3.circe.circeBodySerializer
    val r = basicRequest
      .body(hqRequest)
      .post(uri"https://poligon.aidevs.pl/verify")
      .response(asJson[HQResponse])

    AsyncHttpClientMonixBackend.resource()
      .use { backend => r.send(backend) }
      .runSyncUnsafe()
      .body match {
      case Left(value) =>
        decode[HQResponse](value.asInstanceOf[HttpError[String]].body) match {
          case Right(hqResponse) =>
            hqResponse
          case Left(err) =>
            logger.info(err.toString)
            HQResponse.systemError
        }
      case Right(value) =>
        value
    }
  }

  private def getPoligonData(backend: SttpBackend[Task, MonixStreams & WebSockets]) = {
    basicRequest
      .get(uri"https://poligon.aidevs.pl/dane.txt")
      .response(asString)
      .send(backend)
      .map { (response: Response[Either[String, String]]) =>
        logger.info(s"Got response code: ${response.code} Body: ${response.body}")
        response.body
      }
  }

}