package pl.pko.ai.devs3.s02.e05

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

case class HQReportRequest(
  task: String,
  apikey: String,
  answer: Answares = Answares(),
)

object HQReportRequest {
  implicit val encoder: Encoder[HQReportRequest] = deriveEncoder[HQReportRequest]
  implicit val decoder: Decoder[HQReportRequest] = deriveDecoder[HQReportRequest]
}

case class Answares(
  `01`: String = "Truskawka",
  `02`: String = "Kraków",
  `03`: String = "Hotel",
  `04`: String = "Pizza z ananasem",
  `05`: String = "Brave New World",
)

/**
 * 01=jakiego owocu użyto podczas pierwszej próby transmisji materii w czasie?
 * 02=Na rynku którego miasta wykonano testową fotografię użytą podczas testu przesyłania multimediów?
 * 03=Co Bomba chciał znaleźć w Grudziądzu?
 * 04=Resztki jakiego dania zostały pozostawione przez Rafała?
 * 05=Od czego pochodzą litery BNW w nazwie nowego modelu językowego?
 */
object Answares {
  implicit val encoder: Encoder[Answares] = deriveEncoder[Answares]
  implicit val decoder: Decoder[Answares] = deriveDecoder[Answares]
}
