package pl.pko.ai.devs3.s03.e01

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class HQReportRequest(
  task: String,
  apikey: String,
  answer: Option[Map[String, String]] = None,
)

object HQReportRequest {
  implicit val encoder: Encoder[HQReportRequest] = deriveEncoder[HQReportRequest]
  implicit val decoder: Decoder[HQReportRequest] = deriveDecoder[HQReportRequest]
}

case class KeyWords(
  `2024-11-12_report-00-sektor_C4.txt`: String,
  `2024-11-12_report-01-sektor_A1.txt`: String,
  `2024-11-12_report-02-sektor_A3.txt`: String,
  `2024-11-12_report-03-sektor_A3.txt`: String,
  `2024-11-12_report-04-sektor_B2.txt`: String,
  `2024-11-12_report-05-sektor_C1.txt`: String,
  `2024-11-12_report-06-sektor_C2.txt`: String,
  `2024-11-12_report-07-sektor_C4.txt`: String,
  `2024-11-12_report-08-sektor_A1.txt`: String,
  `2024-11-12_report-09-sektor_C2.txt`: String,
)

object KeyWords {
  implicit val encoder: Encoder[KeyWords] = deriveEncoder[KeyWords]
  implicit val decoder: Decoder[KeyWords] = deriveDecoder[KeyWords]
}
