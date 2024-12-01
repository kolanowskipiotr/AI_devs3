package pl.pko.ai.devs3.s04.e03

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import pl.pko.ai.devs3.hq.model.HQResponse

import java.nio.file.{Files, Paths, StandardOpenOption}

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
                    cached: Boolean = false,
                    questions: Map[String, String] = Map.empty,
                    answerers: Map[String, String] = Map.empty,
                    hqResponse: Option[HQResponse] = None,
                    pagesToAnalise: List[String] = List.empty,
                    pageDB: PageDb = PageDb.empty
                  ) {
  
  def addAnswers(newAnswerers: Map[String, String]): Map[String, String] = {
    answerers ++ newAnswerers.filterNot { case (k, _) => answerers.contains(k) }
  }
  
  def hasAllTheAnswers: Boolean = 
    questions.keySet == answerers.keySet

  def cacheContext: Either[String, Context] = {
    val directory = Paths.get(Context.cachePath)
    val filePath = directory.resolve("context.json")

    try {
      Files.write(
        filePath,
        anonimized.asJson.noSpaces.getBytes,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      )
      Right(this)
    } catch {
      case e: Exception => Left(e.getMessage)
    }
  }

  private def anonimized: Context = {
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

  val cachePath = "src/main/scala/pl/pko/ai/devs3/s04/e03/cache/context.json"

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
    val cacheFilePath = Paths.get(cachePath)
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