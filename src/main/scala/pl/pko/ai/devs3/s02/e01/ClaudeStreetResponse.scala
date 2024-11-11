package pl.pko.ai.devs3.s02.e01

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

//{"_chainOfThought": "Provide your chain of thought here", "streetName": "Put extracted street name here"}
case class ClaudeStreetResponse(
  _chainOfThought: String,
  streetName: String,
)

object ClaudeStreetResponse {
  
  implicit val encoder: Encoder[ClaudeStreetResponse] = deriveEncoder[ClaudeStreetResponse]
  implicit val decoder: Decoder[ClaudeStreetResponse] = deriveDecoder[ClaudeStreetResponse]
}
