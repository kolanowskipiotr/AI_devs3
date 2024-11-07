package pl.pko.ai.devs3.llm.claude.ai.model

import pl.pko.ai.devs3.llm.model.LLMResponse

case class ClaudeResponse(
  id: String,
  `type`: String,
  role: String,
  model: String,
  content: List[Content],
  stop_reason: String,
  stop_sequence: Option[String],
  usage: Usage
) extends LLMResponse {

  override def textResponse: String = content.map(_.text).mkString(" ")
}

case class Content(
  `type`: String,
  text: String
)

case class Usage(
  input_tokens: Int,
  output_tokens: Int
)

object Usage {
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.circe.{Decoder, Encoder}

  implicit val encoder: Encoder[Usage] = deriveEncoder[Usage]
  implicit val decoder: Decoder[Usage] = deriveDecoder[Usage]
}

object ClaudeResponse {
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.circe.{Decoder, Encoder}

  implicit val encoder: Encoder[ClaudeResponse] = deriveEncoder[ClaudeResponse]
  implicit val decoder: Decoder[ClaudeResponse] = deriveDecoder[ClaudeResponse]
}

object Content {
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.circe.{Decoder, Encoder}

  implicit val encoder: Encoder[Content] = deriveEncoder[Content]
  implicit val decoder: Decoder[Content] = deriveDecoder[Content]
}
