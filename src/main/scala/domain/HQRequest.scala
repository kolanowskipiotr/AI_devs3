package pl.pko.ai.devs3
package domain

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class HQRequest(
  task: String,
  apikey: String,
  answer: List[String],
)

object HQRequest {
  implicit val encoder: Encoder[HQRequest] = deriveEncoder[HQRequest]
  implicit val decoder: Decoder[HQRequest] = deriveDecoder[HQRequest]
}
