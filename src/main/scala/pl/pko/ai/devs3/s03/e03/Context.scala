package pl.pko.ai.devs3.s03.e03

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.{Decoder, Encoder}
import pl.pko.ai.devs3.s03.e03.BanAN.db.model.{ShowTablesResponse, TableStructureResponse}

import java.nio.file.{Files, Paths}

case class Context(
  hqApikey: String,
  claudeApiKey: String,
  groqApiKey: String,
  qdrantApiUrl: String,
  qdrantApiKey: String,
  jinaApiKey: String,
  question: Option[Question] = None,
  readTableNames: Option[ShowTablesResponse] = None,
  readTableStructure: Option[List[TableStructureResponse]] = None,
  generatedSQL: Option[String] = None,
  generatedSQLResult: Option[List[String]] = None,
) {
  
  private def anonimized: Context =
    this.copy(
      hqApikey = "SECRET",
      claudeApiKey = "SECRET",
      groqApiKey = "SECRET",
      qdrantApiUrl = "SECRET",
      qdrantApiKey = "SECRET",
      jinaApiKey = "SECRET",
    )
}

object Context {

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
  embeding: Option[List[Double]] = None,
)

object Question {

  implicit val encoder: Encoder[Question] = deriveEncoder[Question]
  implicit val decoder: Decoder[Question] = deriveDecoder[Question]
}