package pl.pko.ai.devs3.s01e01

case class OllamaResponse(
  response: String
)

object OllamaResponse {
  import io.circe.{Decoder, Encoder}
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

  implicit val encoder: Encoder[OllamaResponse] = deriveEncoder[OllamaResponse]
  implicit val decoder: Decoder[OllamaResponse] = deriveDecoder[OllamaResponse]
}
