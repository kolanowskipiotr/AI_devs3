package pl.pko.ai.devs3.llm.jina.ai.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/**
 * Example:
 * {
 * "model": "jina-embeddings-v3",
 * "object": "list",
 * "usage": {
 * "total_tokens": 225,
 * "prompt_tokens": 225
 * },
 * "data": [
 * {
 * "object": "embedding",
 * "index": 0,
 * "embedding": [
 * 0.12738544,
 * 0.058744956,
 * -0.015922556,
 * 0.009768669
 * ]
 * },
 * {
 * "object": "embedding",
 * "index": 1,
 * "embedding": [
 * 0.12885645,
 * 0.06801974,
 * 0.0224968
 * ]
 * }
 * ]
 * }
 */
case class JinaResponse(
  model: String,
  `object`: String,
  usage: JinaUsage,
  data: List[JinaData],
)

object JinaResponse {

  implicit val encoder: Encoder[JinaResponse] = deriveEncoder[JinaResponse]
  implicit val decoder: Decoder[JinaResponse] = deriveDecoder[JinaResponse]
}

case class JinaUsage(
  total_tokens: Int,
  prompt_tokens: Int,
)

object JinaUsage {

  implicit val encoder: Encoder[JinaUsage] = deriveEncoder[JinaUsage]
  implicit val decoder: Decoder[JinaUsage] = deriveDecoder[JinaUsage]
}

case class JinaData(
  `object`: String,
  index: Int,
  embedding: List[Double],
)

object JinaData {

  implicit val encoder: Encoder[JinaData] = deriveEncoder[JinaData]
  implicit val decoder: Decoder[JinaData] = deriveDecoder[JinaData]
}
