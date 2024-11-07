package pl.pko.ai.devs3.s01.e02

case class IdentityCheckMessage(
 msgID: Int,
 text: String,
)

object IdentityCheckMessage {
  import io.circe.{Decoder, Encoder}
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

  implicit val encoder: Encoder[IdentityCheckMessage] = deriveEncoder[IdentityCheckMessage]
  implicit val decoder: Decoder[IdentityCheckMessage] = deriveDecoder[IdentityCheckMessage]
}
