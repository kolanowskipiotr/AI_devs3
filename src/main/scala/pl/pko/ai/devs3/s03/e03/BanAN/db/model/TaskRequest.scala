package pl.pko.ai.devs3.s03.e03.BanAN.db.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/**
 * {
 * "task": "database",
 * "apikey": "SECRET",
 * "query": "show tables"
 * }
 */
case class TaskRequest(
  task: String,
  apikey: String,
  query: String,
)

object TaskRequest {
  implicit val encoder: Encoder[TaskRequest] = deriveEncoder[TaskRequest]
  implicit val decoder: Decoder[TaskRequest] = deriveDecoder[TaskRequest]
}
