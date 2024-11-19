package pl.pko.ai.devs3.db.qdrant.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}
import java.util.UUID

/**
 * {
 * "id": UUID,
 * "vector": [0.05, 0.61, 0.76, 0.74],
 * "payload": {
 * "fileName": "Mars"
 * }
 * }
 */
case class QdrantDocument(
  id: String = UUID.randomUUID().toString,
  vector: List[Double],
  payload: Map[String, String] = Map.empty
)

object QdrantDocument {

  implicit val encoder: Encoder[QdrantDocument] = deriveEncoder[QdrantDocument]
  implicit val decoder: Decoder[QdrantDocument] = deriveDecoder[QdrantDocument]
}
