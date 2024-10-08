package pl.pko.ai.devs3
package domain.helth.check

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import domain.Agent
import sttp.capabilities.WebSockets
import sttp.shared.Identity
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.netty.sync.OxStreams
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.generic.auto._

import java.time.LocalDateTime

case class HelthCheckAgent(lesson: String) extends Agent:
  override def endpoints: List[ServerEndpoint[OxStreams & WebSockets, Identity]] =
    List(
      endpoint
        .get
        .in("helth-chack")
        .in(query[Option[String]]("service-name"))
        .out(jsonBody[HelthCheck])
        .handleSuccess(serviceName => HelthCheck(serviceName.getOrElse("Unknown")))
    )

case class HelthCheck(
  serviceName: String,
  serverTime: LocalDateTime = LocalDateTime.now(),
)

object HelthCheck {
  implicit val encoder: Encoder[HelthCheck] = deriveEncoder[HelthCheck]
  implicit val decoder: Decoder[HelthCheck] = deriveDecoder[HelthCheck]
}
