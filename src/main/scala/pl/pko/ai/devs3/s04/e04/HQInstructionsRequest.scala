package pl.pko.ai.devs3.s04.e04

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/**
 * Example:
 * {
 * "instruction":"tutaj instrukcja gdzie poleciał dron"
 * }
 */
case class HQInstructionsRequest(instruction: String)

object HQInstructionsRequest {
  
  implicit val encoder: Encoder[HQInstructionsRequest] = deriveEncoder[HQInstructionsRequest]
  implicit val decoder: Decoder[HQInstructionsRequest] = deriveDecoder[HQInstructionsRequest]
}
