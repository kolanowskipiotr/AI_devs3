package pl.pko.ai.devs3.s01e01

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class FirecrawlResponse(
  success: Boolean,
  data: FirecrawlData
)

object FirecrawlResponse {
  implicit val encoder: Encoder[FirecrawlResponse] = deriveEncoder[FirecrawlResponse]
  implicit val decoder: Decoder[FirecrawlResponse] = deriveDecoder[FirecrawlResponse]
}

case class FirecrawlData(
  markdown: String
)

object FirecrawlData {
  implicit val encoder: Encoder[FirecrawlData] = deriveEncoder[FirecrawlData]
  implicit val decoder: Decoder[FirecrawlData] = deriveDecoder[FirecrawlData]
}
