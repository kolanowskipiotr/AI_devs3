package pl.pko.ai.devs3.s03.e01

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.{Decoder, Encoder}
import pl.pko.ai.devs3.hq.model.HQResponse
import pl.pko.ai.devs3.s02.e01.HQReportRequest

import java.nio.file.Path

case class Context(
  claudeApiKey: String,
  groqApiKey: String,
  hqApikey: String,
  reports: List[Document] = List.empty,
  facts: List[Document] = List.empty,
) {
  def getUnemptyFacts: List[Document] =
    facts
      .filter(_.content.trim.nonEmpty)
      .filter(_.content.trim.equals("entry deleted"))
}

case class Document(
  fileName: String,
  content: String,
  keywords: Option[String] = None,
)
