package pl.pko.ai.devs3.s04.e01

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/**
 * Example
 * {
 * "_chainOfthoughts": "Opisz łańcucha swoich myśli",
 * "descriptionsOfAllPeople": ["Opis pierwszej osoby na zdjęciu. Odzież informacje od przecinkami", "Opis kolejnej osoby"]
 * }
 */
case class PeopleDescription(
  _chainOfthoughts: String,
  descriptionsOfAllPeople: List[String]
)

object PeopleDescription {

  implicit val encoder: Encoder[PeopleDescription] = deriveEncoder[PeopleDescription]
  implicit val decoder: Decoder[PeopleDescription] = deriveDecoder[PeopleDescription]
}
