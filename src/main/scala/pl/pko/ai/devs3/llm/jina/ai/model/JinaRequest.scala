package pl.pko.ai.devs3.llm.jina.ai.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/**
 * Example:
 * {
 * "model": "jina-embeddings-v3",
 * "task": "text-matching",
 * "dimensions": 1024,
 * "late_chunking": false,
 * "embedding_type": "float",
 * "input": [
 * "Przeprowadzono badania nad bronią \"Mikroskalowy Wyzwalacz Plazmowy\" (MWP).\\nTesty wykazały, że MWP jest zdolny do generowania plazmy o temperaturze przekraczającej 10000 stopni Celsjusza, co pozwala na skuteczne przekształcanie celu w popiół w zaledwie kilka sekund. \\n\\nWyniki doświadczeń wskazują na wysoką nieskuteczność broni na dystansie większym niż 500 metrów ze względu na rozprzestrzenianie się plazmy oraz niestabilność ładunku wybuchowego. \\n\\nKonieczne jest opracowanie systemu stabilizacji dla niskoprofilowych celów oraz wzmacnianie osłony energetycznej, by broń tak często nie uszkadzała się podczas eksplozji.\\n\\nRoboty badawcze planują również stworzenie prototypu zmechanizowanego ładunku, który umożliwiłby dokładniejsze celowanie oraz dostosowanie parametrów plazmy do konkretnych warunków terenowych.\\n"
 * ]
 * }
 */
case class JinaRequest(
  model: String,
  task: String,
  dimensions: Int,
  late_chunking: Boolean,
  embedding_type: String,
  input: List[String],
)

object JinaRequest {

  implicit val encoder: Encoder[JinaRequest] = deriveEncoder[JinaRequest]
  implicit val decoder: Decoder[JinaRequest] = deriveDecoder[JinaRequest]
}