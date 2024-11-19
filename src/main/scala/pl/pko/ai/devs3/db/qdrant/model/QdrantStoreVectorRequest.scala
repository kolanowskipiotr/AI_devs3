package pl.pko.ai.devs3.db.qdrant.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/**
 * Example:
 * {
 * "points": [
 * {
 * "id": 1,
 * "vector": [0.05, 0.61, 0.76, 0.74],
 * "payload": {
 * "fileName": "Mars"
 * }
 * }
 * ]
 * }
 */
case class QdrantStoreVectorRequest(
  points: List[QdrantDocument],
)

object QdrantStoreVectorRequest {

  implicit val encoder: Encoder[QdrantStoreVectorRequest] = deriveEncoder[QdrantStoreVectorRequest]
  implicit val decoder: Decoder[QdrantStoreVectorRequest] = deriveDecoder[QdrantStoreVectorRequest]
}
