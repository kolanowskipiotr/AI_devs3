package pl.pko.ai.devs3.llm.groq.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import pl.pko.ai.devs3.llm.claude.ai.model.ClaudeResponse

/*
{
  "text": "Your transcribed text appears here...",
  "x_groq": {
    "id": "req_unique_id"
  }
}
 */
case class GroqResponse (
  text: String,
  x_groq: GroqResponseXGroq
)

object GroqResponse {

  implicit val encoder: Encoder[GroqResponse] = deriveEncoder[GroqResponse]
  implicit val decoder: Decoder[GroqResponse] = deriveDecoder[GroqResponse]
}

case class GroqResponseXGroq (
  id: String
)

object GroqResponseXGroq {

  implicit val encoder: Encoder[GroqResponseXGroq] = deriveEncoder[GroqResponseXGroq]
  implicit val decoder: Decoder[GroqResponseXGroq] = deriveDecoder[GroqResponseXGroq]
}
