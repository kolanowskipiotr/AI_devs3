package pl.pko.ai.devs3.s02.e04

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class HQReportRequest(
  task: String,
  apikey: String,
  answer: FilesReport,
)

object HQReportRequest {
  implicit val encoder: Encoder[HQReportRequest] = deriveEncoder[HQReportRequest]
  implicit val decoder: Decoder[HQReportRequest] = deriveDecoder[HQReportRequest]
}

case class FilesReport(
  people: List[String],
  hardware: List[String],
)

object FilesReport {
  implicit val encoder: Encoder[FilesReport] = deriveEncoder[FilesReport]
  implicit val decoder: Decoder[FilesReport] = deriveDecoder[FilesReport]
}
