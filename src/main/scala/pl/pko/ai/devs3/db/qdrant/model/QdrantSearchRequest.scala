package pl.pko.ai.devs3.db.qdrant.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/**
 * Example:
 * {
 * * "vector":  [
 * * 0.07999753
 * * ],
 * * "limit": 1,
 * * "with_payload": true
 * * }
 */
case class QdrantSearchRequest(
  vector: List[Double],
  limit: Int,
  with_payload: Boolean
)

object QdrantSearchRequest {
  
  implicit val encoder: Encoder[QdrantSearchRequest] = deriveEncoder[QdrantSearchRequest]
  implicit val decoder: Decoder[QdrantSearchRequest] = deriveDecoder[QdrantSearchRequest]
}


