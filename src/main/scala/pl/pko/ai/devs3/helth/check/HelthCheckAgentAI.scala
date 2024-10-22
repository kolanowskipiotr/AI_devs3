package pl.pko.ai.devs3.helth.check

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import pl.pko.ai.devs3.agent.AgentAI
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.ServerEndpoint

import java.time.LocalDateTime
import scala.concurrent.Future

case class HelthCheckAgentAI(lesson: String) extends AgentAI:
  override def endpoints: List[ServerEndpoint[Any, Future]] =
    List(
      endpoint.get
        .in("helth-chack")
        .in(query[Option[String]]("service-name"))
        .out(jsonBody[HelthCheck])
        .serverLogicSuccess(serviceName => Future.successful(HelthCheck(serviceName.getOrElse("Unknown"))))
    )

case class HelthCheck(
    serviceName: String,
    serverTime: LocalDateTime = LocalDateTime.now()
)

object HelthCheck {
  implicit val encoder: Encoder[HelthCheck] = deriveEncoder[HelthCheck]
  implicit val decoder: Decoder[HelthCheck] = deriveDecoder[HelthCheck]
}
