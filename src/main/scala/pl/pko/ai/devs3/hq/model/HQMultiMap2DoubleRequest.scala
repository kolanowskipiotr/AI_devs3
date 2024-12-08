package pl.pko.ai.devs3.hq.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class HQMultiMap2DoubleRequest(
  task: String,
  apikey: String,
  answer: Map[String, Map[String, Double]],
)

object HQMultiMap2DoubleRequest {
  implicit val encoder: Encoder[HQMultiMap2DoubleRequest] = deriveEncoder[HQMultiMap2DoubleRequest]
  implicit val decoder: Decoder[HQMultiMap2DoubleRequest] = deriveDecoder[HQMultiMap2DoubleRequest]
}
