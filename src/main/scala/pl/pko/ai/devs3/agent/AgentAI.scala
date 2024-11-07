package pl.pko.ai.devs3.agent

import org.slf4j.{Logger, LoggerFactory}
import pl.pko.ai.devs3.hq.model.HQResponse
import sttp.tapir.server.ServerEndpoint
import io.circe.parser.decode

import scala.concurrent.Future

trait AgentAI {

  protected val log: Logger = LoggerFactory.getLogger(getClass)

  val lesson: String

  def endpoints: List[ServerEndpoint[Any, Future]]

  protected def tryParseHQResponse(body: String): HQResponse =
    decode[HQResponse](body) match {
      case Right(hqResponse) => hqResponse
      case Left(err) =>
        log.error(s"Parsing of error body fail ${err.getMessage}", err)
        HQResponse.systemError
    }
}
