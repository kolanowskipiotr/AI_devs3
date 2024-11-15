package pl.pko.ai.devs3.s02.e05

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.{Decoder, Encoder}
import pl.pko.ai.devs3.hq.model.HQResponse
import pl.pko.ai.devs3.s02.e01.HQReportRequest

import java.nio.file.Path

case class Context(
  claudeApiKey: String,
  groqApiKey: String,
  hqApikey: String,
)
object Context {

  implicit val encoder: Encoder[Context] = deriveEncoder[Context]
  implicit val decoder: Decoder[Context] = deriveDecoder[Context]
}
