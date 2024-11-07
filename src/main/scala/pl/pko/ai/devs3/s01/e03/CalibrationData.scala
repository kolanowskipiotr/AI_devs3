package pl.pko.ai.devs3.s01.e03

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/**
 * {
 * "apikey": "%PUT-YOUR-API-KEY-HERE%",
 * "description": "This is simple calibration data used for testing purposes. Do not use it in production environment!",
 * "copyright": "Copyright (C) 2238 by BanAN Technologies Inc.",
 * "test-data": [
 * {
 * "question": "21 + 94",
 * "answer": 115
 * },
 * {
 * "question": "2 + 82",
 * "answer": 84
 * },
 * {
 * "question": "93 + 44",
 * "answer": 137,
 * "test": {
 * "q": "What is the capital city of France?",
 * "a": "???"
 * }
 * },
 * ...
 * ]
 * }
 */

case class CalibrationData(
  apikey: String,
  description: String,
  copyright: String,
  `test-data`: List[CalibrationDataEntry]
)

case class CalibrationDataEntry(
  question: String,
  answer: Int,
  test: Option[TestData]
)

case class TestData(
  q: String,
  a: String
)

object CalibrationData {
  implicit val encoder: Encoder[CalibrationData] = deriveEncoder[CalibrationData]
  implicit val decoder: Decoder[CalibrationData] = deriveDecoder[CalibrationData]
}

object CalibrationDataEntry {
  implicit val encoder: Encoder[CalibrationDataEntry] = deriveEncoder[CalibrationDataEntry]
  implicit val decoder: Decoder[CalibrationDataEntry] = deriveDecoder[CalibrationDataEntry]
}

object TestData {
  implicit val encoder: Encoder[TestData] = deriveEncoder[TestData]
  implicit val decoder: Decoder[TestData] = deriveDecoder[TestData]
}
