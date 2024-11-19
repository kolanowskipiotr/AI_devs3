package pl.pko.ai.devs3.db.qdrant.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/**
 * {
 * "result": [
 * {
 * "id": "bde78227-e048-40a0-acf4-3e8f2f366694",
 * "version": 1,
 * "score": 0.5685324,
 * "payload": {
 * "fileName": "2024_02_21.txt"
 * }
 * }
 * ],
 * "status": "ok",
 * "time": 0.000667288
 * }
 */
case class SearchResponse(
  result: List[SearchResult],
  status: String,
  time: Double,
)

object SearchResponse {

  implicit val encoder: Encoder[SearchResponse] = deriveEncoder[SearchResponse]
  implicit val decoder: Decoder[SearchResponse] = deriveDecoder[SearchResponse]
}

case class SearchResult(
  id: String,
  version: Int,
  score: Double,
  payload: Map[String, String],
)

object SearchResult {

  implicit val encoder: Encoder[SearchResult] = deriveEncoder[SearchResult]
  implicit val decoder: Decoder[SearchResult] = deriveDecoder[SearchResult]
}
