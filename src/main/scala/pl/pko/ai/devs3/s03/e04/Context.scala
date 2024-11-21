package pl.pko.ai.devs3.s03.e04

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.{Decoder, Encoder}
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.substringBetween
import pl.pko.ai.devs3.s03.e03.BanAN.db.model.{ShowTablesResponse, TableStructureResponse}

import java.nio.file.{Files, Paths}

case class Context(
  hqApikey: String,
  claudeApiKey: String,
  groqApiKey: String,
  qdrantApiUrl: String,
  qdrantApiKey: String,
  jinaApiKey: String,
  noteAboutBarbara: Option[String] = None,
  poi: List[Note] = List.empty,
  people: Set[String] = Set.empty,
  places: Set[String] = Set.empty,
  locationOfBarbara: Option[String] = None,
) {

  def addPlace(poiToMark: Note, newPlaces: List[Note], knowlage: String): Context = {
    this.copy(
      places = places ++ newPlaces.map(_.poi).filter(p => StringUtils.isNotBlank(p)).toSet,
      poi = markAsAnalized(poi, poiToMark, knowlage) ++ getUnknownPois(newPlaces),
    )
  }

  def addPerson(poiToMark: Note, newPersons: List[Note], knowlage: String): Context = {
    this.copy(
      people = people ++ newPersons.map(_.poi).filter(p => StringUtils.isNotBlank(p)).toSet,
      poi = markAsAnalized(poi, poiToMark, knowlage) ++ getUnknownPois(newPersons),
    )
  }

  private def markAsAnalized(pois: List[Note], poiToMark: Note, knowlage: String): List[Note] =
    pois.map(p => if (p.poi == poiToMark.poi)
      p.copy(analyzed = true, knowlage = Some(knowlage))
    else
      p)

  private def getUnknownPois(pois: List[Note]): List[Note] = {
    pois
      .filter(a => StringUtils.isNotBlank(a.poi))
      .filterNot(p => poi.exists(_.poi == p.poi))
  }

  def anonimized: Context =
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

case class Note(
  poi: String,
  poiType: String,
  knowlage: Option[String] = None,
  analyzed: Boolean = false
)

object Note {

  implicit val encoder: Encoder[Note] = deriveEncoder[Note]
  implicit val decoder: Decoder[Note] = deriveDecoder[Note]
}