package pl.pko.ai.devs3.hq.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class HQResponse(
  code: Int,
  message: String,
)

object HQResponse {
  
  def systemError: HQResponse = HQResponse(Int.MaxValue, "System error")
  
  def success(message: String = "Success"): HQResponse = HQResponse(0, message)
  
  implicit val encoder: Encoder[HQResponse] = deriveEncoder[HQResponse]
  implicit val decoder: Decoder[HQResponse] = deriveDecoder[HQResponse]
}
