package pl.pko.ai.devs3.db.qdrant.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/**
 * Example:
 * {
 * "result": {
 * "operation_id": 0,
 * "status": "acknowledged"
 * },
 * "status": "ok",
 * "time": 0.00038778
 * }
 */
case class QdrantResponse(
  result: QdrantResult,
  status: String,
  time: Double,
)

object QdrantResponse {

  implicit val encoder: Encoder[QdrantResponse] = deriveEncoder[QdrantResponse]
  implicit val decoder: Decoder[QdrantResponse] = deriveDecoder[QdrantResponse]
}

case class QdrantResult(
  operation_id: Int,
  status: String,
)

object QdrantResult {

  implicit val encoder: Encoder[QdrantResult] = deriveEncoder[QdrantResult]
  implicit val decoder: Decoder[QdrantResult] = deriveDecoder[QdrantResult]
}
