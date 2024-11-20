package pl.pko.ai.devs3.s03.e03.BanAN.db.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/**
 * {
 * "reply": [
 * {
 * "dc_id": "4278"
 * },
 * {
 * "dc_id": "9294"
 * }
 * ],
 * "error": "OK"
 * }
 */
case class SelectResponse(
  reply: List[SelectResponseReply],
  error: String,
)

object SelectResponse {

  implicit val encoder: Encoder[SelectResponse] = deriveEncoder[SelectResponse]
  implicit val decoder: Decoder[SelectResponse] = deriveDecoder[SelectResponse]
}

case class SelectResponseReply(
  dc_id: String
)

object SelectResponseReply {

  implicit val encoder: Encoder[SelectResponseReply] = deriveEncoder[SelectResponseReply]
  implicit val decoder: Decoder[SelectResponseReply] = deriveDecoder[SelectResponseReply]
}
