package pl.pko.ai.devs3.s05.e03.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/**
 * {
 * "code": 0,
 * "message": {
 * "signature": "605ac78ebfb160c8507f1591ebb8f634",
 * "timestamp": 1733766396,
 * "challenges": [
 * "https:\/\/rafal.ag3nts.org\/source0",
 * "https:\/\/rafal.ag3nts.org\/source1"
 * ]
 * }
 * }
 */
case class RafalsAPITimestampResponse(
                                       code: Int,
                                       message: RafalsAPITimestampResponseMessage,
                                     )

object RafalsAPITimestampResponse {
  
  def empty(): RafalsAPITimestampResponse = 
    RafalsAPITimestampResponse(0, RafalsAPITimestampResponseMessage("", 0, List.empty))

  implicit val encoder: Encoder[RafalsAPITimestampResponse] = deriveEncoder[RafalsAPITimestampResponse]
  implicit val decoder: Decoder[RafalsAPITimestampResponse] = deriveDecoder[RafalsAPITimestampResponse]
}

case class RafalsAPITimestampResponseMessage(
                                              signature: String,
                                              timestamp: Long,
                                              challenges: List[String],
                                            )

object RafalsAPITimestampResponseMessage {

  implicit val encoder: Encoder[RafalsAPITimestampResponseMessage] = deriveEncoder[RafalsAPITimestampResponseMessage]
  implicit val decoder: Decoder[RafalsAPITimestampResponseMessage] = deriveDecoder[RafalsAPITimestampResponseMessage]
}