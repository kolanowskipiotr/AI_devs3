package pl.pko.ai.devs3.s03.e04.knowlage.system

import pl.pko.ai.devs3.s03.e04.Note

/**
 * Example of response from KnowlageSystem:
 * {
 * "code": 0,
 * "message": "KRAKOW LUBLIN WARSZAWA"
 * }
 */
case class KnowlageSystemResponse(
  code: Int,
  message: String,
)

object KnowlageSystemResponse {
  import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
  import io.circe.{Decoder, Encoder}

  implicit val encoder: Encoder[KnowlageSystemResponse] = deriveEncoder[KnowlageSystemResponse]
  implicit val decoder: Decoder[KnowlageSystemResponse] = deriveDecoder[KnowlageSystemResponse]
}
