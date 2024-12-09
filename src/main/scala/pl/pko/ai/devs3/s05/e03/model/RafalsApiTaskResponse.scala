package pl.pko.ai.devs3.s05.e03.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/**
 * {
 * "task": "Źródło wiedzy https:\/\/centrala.ag3nts.org\/dane\/arxiv-draft.html",
 * "data": [
 * "Rozwiń skrót BNW-01",
 * "Ile bitów danych przesłano w ramach eksperymentu?"
 * ]
 * }
 */
case class RafalsApiTaskResponse(
                                  task: String,
                                  data: List[String],
                                )

object RafalsApiTaskResponse {

  implicit val encoder: Encoder[RafalsApiTaskResponse] = deriveEncoder[RafalsApiTaskResponse]
  implicit val decoder: Decoder[RafalsApiTaskResponse] = deriveDecoder[RafalsApiTaskResponse]
}