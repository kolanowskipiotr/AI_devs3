package pl.pko.ai.devs3.hq.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class HQMapRequest(
  task: String,
  apikey: String,
  answer: Map[String, String],
)

object HQMapRequest {
  implicit val encoder: Encoder[HQMapRequest] = deriveEncoder[HQMapRequest]
  implicit val decoder: Decoder[HQMapRequest] = deriveDecoder[HQMapRequest]
}
