package pl.pko.ai.devs3.llm.ollama.model

case class OllamaResponse(
  response: String
)

object OllamaResponse {
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.circe.{Decoder, Encoder}

  implicit val encoder: Encoder[OllamaResponse] = deriveEncoder[OllamaResponse]
  implicit val decoder: Decoder[OllamaResponse] = deriveDecoder[OllamaResponse]
}
