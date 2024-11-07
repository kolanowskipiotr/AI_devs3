package pl.pko.ai.devs3.s01.e02

case class IdentityCheckResponse(
  code: Int,
  message: String,
)

object IdentityCheckResponse {
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.circe.{Decoder, Encoder}

  implicit val encoder: Encoder[IdentityCheckResponse] = deriveEncoder[IdentityCheckResponse]
  implicit val decoder: Decoder[IdentityCheckResponse] = deriveDecoder[IdentityCheckResponse]
}
