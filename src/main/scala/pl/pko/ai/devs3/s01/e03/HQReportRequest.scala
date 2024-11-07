package pl.pko.ai.devs3.s01.e03

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class HQReportRequest(
  task: String,
  apikey: String,
  answer: CalibrationData,
)

object HQReportRequest {
  implicit val encoder: Encoder[HQReportRequest] = deriveEncoder[HQReportRequest]
  implicit val decoder: Decoder[HQReportRequest] = deriveDecoder[HQReportRequest]
}
