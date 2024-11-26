package pl.pko.ai.devs3.hq.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class HQReportListRequest(
  task: String,
  apikey: String,
  answer: List[String],
)

object HQReportListRequest {
  implicit val encoder: Encoder[HQReportListRequest] = deriveEncoder[HQReportListRequest]
  implicit val decoder: Decoder[HQReportListRequest] = deriveDecoder[HQReportListRequest]
}
