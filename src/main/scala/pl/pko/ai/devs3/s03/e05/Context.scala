package pl.pko.ai.devs3.s03.e05

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.{Decoder, Encoder}
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.substringBetween
import pl.pko.ai.devs3.s03.e03.BanAN.db.model.{ShowTablesResponse, TableStructureResponse}
import pl.pko.ai.devs3.s03.e05.BanAN.db.model.{Connection, User}

import java.nio.file.{Files, Paths}
import scala.util.matching.Regex

case class Context(
                    hqApikey: String,
                    claudeApiKey: String,
                    groqApiKey: String,
                    qdrantApiUrl: String,
                    qdrantApiKey: String,
                    jinaApiKey: String,
                    neo4jUri: String,
                    neo4jUser: String,
                    neo4jPassword: String,
                    neo4jConnected: Boolean = false,
                    neo4jFed: Boolean = false,
                    startPerson: Option[String] = None,
                    endPerson: Option[String] = None,
                    cached: Boolean = false,
                    users: List[User] = List.empty,
                    connections: List[Connection] = List.empty,
                    rotes: Map[String, Int] = Map.empty,
                  ) {

  def anonimized: Context = {
    val secret = "SECRET"
    this.copy(
      hqApikey = secret,
      claudeApiKey = secret,
      groqApiKey = secret,
      qdrantApiUrl = secret,
      qdrantApiKey = secret,
      jinaApiKey = secret,
      neo4jUri = secret,
      neo4jUser = secret,
      neo4jPassword = secret,
    )
  }
}

object Context {

  def apply(
             hqApikey: String,
             claudeApiKey: String,
             groqApiKey: String,
             qdrantApiUrl: String,
             qdrantApiKey: String,
             jinaApiKey: String,
             neo4jUri: String,
             neo4jUser: String,
             neo4jPassword: String): Context = {
    val cacheFilePath = Paths.get("src/main/scala/pl/pko/ai/devs3/s03/e05/cache/context.json")
    if (Files.exists(cacheFilePath)) {
      val cachedContext = decode[Context](new String(Files.readAllBytes(cacheFilePath)))
      cachedContext match {
        case Right(context) => context.copy(
          cached = true,
          hqApikey = hqApikey,
          claudeApiKey = claudeApiKey,
          groqApiKey = groqApiKey,
          qdrantApiUrl = qdrantApiUrl,
          qdrantApiKey = qdrantApiKey,
          jinaApiKey = jinaApiKey,
          neo4jUri = neo4jUri,
          neo4jUser = neo4jUser,
          neo4jPassword = neo4jPassword,
          neo4jConnected = false,
        )
        case Left(_) => empty(
          hqApikey,
          claudeApiKey,
          groqApiKey,
          qdrantApiUrl,
          qdrantApiKey,
          jinaApiKey,
          neo4jUri,
          neo4jUser,
          neo4jPassword,
        )
      }
    } else {
      empty(
        hqApikey,
        claudeApiKey,
        groqApiKey,
        qdrantApiUrl,
        qdrantApiKey,
        jinaApiKey,
        neo4jUri,
        neo4jUser,
        neo4jPassword,
      )
    }
  }

  private def empty(
                     hqApikey: String,
                     claudeApiKey: String,
                     groqApiKey: String,
                     qdrantApiUrl: String,
                     qdrantApiKey: String,
                     jinaApiKey: String,
                     neo4jUri: String,
                     neo4jUser: String,
                     neo4jPassword: String,
                   ) = {
    new Context(
      hqApikey,
      claudeApiKey,
      groqApiKey,
      qdrantApiUrl,
      qdrantApiKey,
      jinaApiKey,
      neo4jUri,
      neo4jUser,
      neo4jPassword,
    )
  }

  implicit val encoder: Encoder[Context] = deriveEncoder[Context]
  implicit val decoder: Decoder[Context] = deriveDecoder[Context]
}