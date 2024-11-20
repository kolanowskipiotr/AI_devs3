package pl.pko.ai.devs3.s03.e03.BanAN.db.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/**
 * {
 * "reply": [
 * {
 * "Tables_in_banan": "connections"
 * },
 * {
 * "Tables_in_banan": "correct_order"
 * },
 * {
 * "Tables_in_banan": "datacenters"
 * },
 * {
 * "Tables_in_banan": "users"
 * }
 * ],
 * "error": "OK"
 * }
 */
case class ShowTablesResponse(
  reply: List[TableResponse],
  error: String,
)

object ShowTablesResponse {

  implicit val encoder: Encoder[ShowTablesResponse] = deriveEncoder[ShowTablesResponse]
  implicit val decoder: Decoder[ShowTablesResponse] = deriveDecoder[ShowTablesResponse]
}

case class TableResponse(
  `Tables_in_banan`: String
)

object TableResponse {

  implicit val encoder: Encoder[TableResponse] = deriveEncoder[TableResponse]
  implicit val decoder: Decoder[TableResponse] = deriveDecoder[TableResponse]
}
