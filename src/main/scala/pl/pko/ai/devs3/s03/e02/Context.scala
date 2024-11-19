package pl.pko.ai.devs3.s03.e02

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

import java.nio.file.{Files, Paths}
import io.circe.parser.decode

case class Context(
  hqApikey: String,
  claudeApiKey: String,
  groqApiKey: String,
  qdrantApiUrl: String,
  qdrantApiKey: String,
  jinaApiKey: String,
  question: Option[Question] = None,
  cached: Boolean = false,
  files: List[Document] = List.empty,
  answer: Option[String] = None,
)

object Context {

  def apply(hqApikey: String, claudeApiKey: String, groqApiKey: String, qdrantApiUrl: String, qdrantApiKey: String, jinaApiKey: String): Context = {
    val cacheFilePath = Paths.get("src/main/scala/pl/pko/ai/devs3/s03/e02/cache/context.json")
    if (Files.exists(cacheFilePath)) {
      val cachedContext = decode[Context](new String(Files.readAllBytes(cacheFilePath)))
      cachedContext match {
        case Right(context) => context.copy(
          cached = true,
          hqApikey = hqApikey,
          claudeApiKey = claudeApiKey,
          groqApiKey = groqApiKey,
          qdrantApiUrl = qdrantApiUrl,
          qdrantApiKey = qdrantApiKey,
          jinaApiKey = jinaApiKey,
        )
        case Left(_) => empty(hqApikey, claudeApiKey, groqApiKey, qdrantApiUrl, qdrantApiKey, jinaApiKey)
      }
    } else {
      empty(hqApikey, claudeApiKey, groqApiKey, qdrantApiUrl, qdrantApiKey, jinaApiKey)
    }
  }

  private def empty(hqApikey: String, claudeApiKey: String, groqApiKey: String, qdrantApiUrl: String, qdrantApiKey: String, jinaApiKey: String) = {
    new Context(hqApikey, claudeApiKey, groqApiKey, qdrantApiUrl, qdrantApiKey, jinaApiKey)
  }

  implicit val encoder: Encoder[Context] = deriveEncoder[Context]
  implicit val decoder: Decoder[Context] = deriveDecoder[Context]
}

case class Document(
  fileName: String,
  content: String,
  keywords: Option[String] = None,
  embedding: Option[List[Double]] = None,
)

object Document {

  implicit val encoder: Encoder[Document] = deriveEncoder[Document]
  implicit val decoder: Decoder[Document] = deriveDecoder[Document]
}


case class Question(
 content: Option[String] = None,
 vector: Option[List[Double]] = None,
)

object Question {

  implicit val encoder: Encoder[Question] = deriveEncoder[Question]
  implicit val decoder: Decoder[Question] = deriveDecoder[Question]
}