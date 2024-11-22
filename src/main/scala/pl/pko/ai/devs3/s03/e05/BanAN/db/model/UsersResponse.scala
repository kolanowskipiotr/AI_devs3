package pl.pko.ai.devs3.s03.e05.BanAN.db.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/**
 * Example of a response model:
 * {
 * "reply": [
 * {
 * "id": "1",
 * "username": "Adrian",
 * "access_level": "user",
 * "is_active": "1",
 * "lastlog": "2023-06-12"
 * },
 * {
 * "id": "2",
 * "username": "Monika",
 * "access_level": "user",
 * "is_active": "1",
 * "lastlog": "2023-09-29"
 * },
 * (...)
 * ],
 * "error": "OK"
 * }
 */
case class UsersResponse(
  reply: List[User],
  error: String,
)

object UsersResponse {

  implicit val decoder: Decoder[UsersResponse] = deriveDecoder[UsersResponse]
  implicit val encoder: Encoder[UsersResponse] = deriveEncoder[UsersResponse]
}

case class User(
  id: String,
  username: String,
  access_level: String,
  is_active: String,
  lastlog: String,
)

object User {

  implicit val decoder: Decoder[User] = deriveDecoder[User]
  implicit val encoder: Encoder[User] = deriveEncoder[User]
}
