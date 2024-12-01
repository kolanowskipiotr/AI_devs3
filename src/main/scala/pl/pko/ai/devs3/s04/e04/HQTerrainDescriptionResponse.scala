package pl.pko.ai.devs3.s04.e04

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class HQTerrainDescriptionResponse(description: String)

object HQTerrainDescriptionResponse {
  
  implicit val encoder: Encoder[HQTerrainDescriptionResponse] = deriveEncoder[HQTerrainDescriptionResponse]
  implicit val decoder: Decoder[HQTerrainDescriptionResponse] = deriveDecoder[HQTerrainDescriptionResponse]
}
