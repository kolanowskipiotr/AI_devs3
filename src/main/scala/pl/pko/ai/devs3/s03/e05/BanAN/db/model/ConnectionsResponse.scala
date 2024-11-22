package pl.pko.ai.devs3.s03.e05.BanAN.db.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/**
 * Example of a response model:
 * {
 * "reply": [
 * {
 * "user1_id": "1",
 * "user2_id": "5"
 * },
 * (...)
 * {
 * "user1_id": "97",
 * "user2_id": "84"
 * }
 * ],
 * "error": "OK"
 * }
 */
case class ConnectionsResponse(
  reply: List[Connection],
  error: String,
)

object ConnectionsResponse {

  implicit val decoder: Decoder[ConnectionsResponse] = deriveDecoder[ConnectionsResponse]
  implicit val encoder: Encoder[ConnectionsResponse] = deriveEncoder[ConnectionsResponse]
}

case class Connection(
  user1_id: String,
  user2_id: String,
)

object Connection {

  implicit val decoder: Decoder[Connection] = deriveDecoder[Connection]
  implicit val encoder: Encoder[Connection] = deriveEncoder[Connection]
}
