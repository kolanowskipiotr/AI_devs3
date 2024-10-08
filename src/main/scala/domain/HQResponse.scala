package pl.pko.ai.devs3
package domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class HQResponse(
  code: Int,
  message: String,
)

object HQResponse {
  implicit val encoder: Encoder[HQResponse] = deriveEncoder[HQResponse]
  implicit val decoder: Decoder[HQResponse] = deriveDecoder[HQResponse]
}
