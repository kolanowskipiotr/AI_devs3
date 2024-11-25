package pl.pko.ai.devs3.s04.e01

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
                    cached: Boolean = false,
                    startResponse: Option[String] = None,
                    files: List[File] = List.empty,
                    descriptionsOfAllPeople: List[String] = List.empty,
                    responseMessages: List[String] = List.empty,
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

  def replaceFilesContentWithPlaceholders: Context = {
    val placeholder = "{FILE_CONTENT}"
    this.copy(files = files.map(f => f.copy(base64 = Some(placeholder))))
  }

  def updateFile(updatedFile: File): Context = {
    this.copy(files = files.map(f => if (f.name == updatedFile.name) updatedFile else f))
  }

  def addFiles(files: List[File]): Context = {
    this.copy(files = this.files ++ files.filterNot(f => this.files.exists(_.name == f.name)))
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
    val cacheFilePath = Paths.get("src/main/scala/pl/pko/ai/devs3/s04/e01/cache/context.json")
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

case class File(
  name: String,
  base64: Option[String],
  fixesDeterminate: Boolean = false,
  fixesToApply: List[String] = List.empty,
  fixesApplied: List[String] = List.empty,
) {
  def allFixesApplied: Boolean =
    fixesDeterminate && (
      fixesToApply.isEmpty ||
        fixesToApply.forall(_.equals("NONE")) ||
        fixesToApply.forall(fixesApplied.contains)
      )

  def isValidImage: Boolean =
    base64.isDefined

  def nextFix: String =
    fixesToApply.filterNot(fixesApplied.contains).head

}

object File {
  implicit val encoder: Encoder[File] = deriveEncoder[File]
  implicit val decoder: Decoder[File] = deriveDecoder[File]
}