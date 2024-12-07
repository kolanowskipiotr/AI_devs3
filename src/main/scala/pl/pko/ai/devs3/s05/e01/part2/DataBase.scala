package pl.pko.ai.devs3.s05.e01.part2

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.parse
import io.circe.{Decoder, Encoder, HCursor}
import org.apache.commons.lang3.StringUtils

import java.nio.file.{Files, Path, Paths}
import java.util.UUID
import scala.util.Try

import io.circe.syntax.*

case class DataBase(
                     phoneCalls: Map[String, PhoneCall] = Map.empty,
                     facts: Map[String, Fact] = Map.empty,
                     tools: List[Tool] = List.empty,
                     actions: List[Action] = List.empty,
                   ) {

  private val log = org.slf4j.LoggerFactory.getLogger(getClass)

  def deleteActions(): DataBase = 
    this.copy(actions = List.empty)
  
  def saveAction(tool: Tool, command: ActionCommand, result: String): DataBase = {
    val newAction = Action(tool, command, List(result))
    this.copy(actions = newAction :: actions)
  }

  def saveAction(tool: Tool, command: ActionCommand, result: List[String]): DataBase = {
    log.info(s"\nAction result: ${tool.toolName}, ${command.parameters}, \n${result.mkString("\n")}")
    val newAction = Action(tool, command, result)
    this.copy(actions = newAction :: actions)
  }

  def findKnowledge(keywords: List[String], limit: Int = 10): List[String] = {
    val facts = findFacts(keywords, limit)
    val phoneCalls = findPhoneCalls(keywords, limit)
    (facts ++ phoneCalls)
      .sortBy(_._1)
      .take(limit)
      .map(_._2)
  }

  def findFacts(keywords: List[String], limit: Int = 10): List[(Int, String)] = {
    facts.values
      .map(fact => fact.findBy(keywords))
      .filter(_._1 > 0)
      .toList
      .sortBy(_._1)
      .take(limit)
  }

  def findPhoneCalls(keywords: List[String], limit: Int = 10): List[(Int, String)] = {
    phoneCalls.values
      .map(phoneCall => phoneCall.findBy(keywords))
      .filter(_._1 > 0)
      .toList
      .sortBy(_._1)
      .take(limit)
  }
}

object DataBase {

  implicit val phoneCallsDecoder: Decoder[DataBase] = deriveDecoder[DataBase]
  implicit val phoneCallsEncoder: Encoder[DataBase] = deriveEncoder[DataBase]

  def empty: DataBase = DataBase()

  def apply(phoneCallsFilePath: String, factsDirPath: String, tools: List[Tool]): DataBase = {
    val phoneCallsT: Try[Map[String, PhoneCall]] = readPhoneCalls(phoneCallsFilePath)
    val factsT: Try[Map[String, Fact]] = readFacts(factsDirPath)

    DataBase(
      phoneCalls = phoneCallsT.toOption.getOrElse(Map.empty),
      facts = factsT.toOption.getOrElse(Map.empty),
      tools = tools,
    )
  }

  private def readFacts(factsDirPath: String) = {
    import scala.jdk.CollectionConverters.*
    val factsT = for {
      factsDir <- Try {
        Paths.get(factsDirPath)
      }
      factFiles <- Try {
        Files.list(factsDir).iterator().asScala.toList
      }
      facts <- Try {
        readFactsFromDirectory(factFiles)
      }
    } yield facts
    factsT
  }

  private def readPhoneCalls(phoneCallsFilePath: String) = {
    val phoneCallsT = for {
      phoneCallsToFixContent <- Try {
        new String(Files.readAllBytes(Paths.get(phoneCallsFilePath)))
      }
      phoneCallsToFixCursor <- Try {
        parse(phoneCallsToFixContent).toOption.get.hcursor
      }
    } yield (1 to 5).map(i => toPhoneCallEntry(phoneCallsToFixCursor, s"rozmowa$i")).toMap
    phoneCallsT
  }

  private def readFactsFromDirectory(factFiles: List[Path]): Map[String, Fact] =
    factFiles
      .filter(Files.isRegularFile(_))
      .map(path => {
        val factId = path.getFileName.toString
        (
          factId,
          Fact(
            id = factId,
            content = splitNonEmpty(new String(Files.readAllBytes(path))),
          )
        )
      })
      .toMap

  private def toPhoneCallEntry(cursor: HCursor, callId: String): (String, PhoneCall) =
    callId -> PhoneCall(
      callId,
      cursor.downField(callId).as[List[String]].toOption.get,
    )

  private def splitNonEmpty(content: String, separator: String = "\n"): List[String] =
    content
      .split(separator)
      .map(_.replace(separator, ""))
      .filter(StringUtils.isNotBlank)
      .map(_.trim)
      .toList
}

case class PhoneCall(
                      id: String,
                      content: List[String]
                    ) {
  def findBy(keywords: List[String]): (Int, String) = {
    val fullContent = content.mkString("\n")
    (keywords.count(fullContent.contains), fullContent)
  }
}

object PhoneCall {

  implicit val phoneCallDecoder: Decoder[PhoneCall] = deriveDecoder[PhoneCall]
  implicit val phoneCallEncoder: Encoder[PhoneCall] = deriveEncoder[PhoneCall]
}

case class Fact(
                 id: String,
                 content: List[String]
               ) {
  def findBy(keywords: List[String]): (Int, String) = {
    val fullContent = content.mkString("\n")
    (keywords.count(fullContent.contains), fullContent)
  }
}

object Fact {

  implicit val factDecoder: Decoder[Fact] = deriveDecoder[Fact]
  implicit val factEncoder: Encoder[Fact] = deriveEncoder[Fact]
}

case class Tool(
                 toolName: String,
                 description: String,
                 parameters: String,
                 usageExamples: List[ActionCommand] = List.empty,
                 provideExamples: Boolean = false
                 //                 uuid: String = UUID.randomUUID().toString,
               ) {
  def mkString: String =
    s"Tool: $toolName, Description: $description, Parameters: $parameters" +
      (if (provideExamples) s"\nUsage examples: ${usageExamples.asJson.noSpaces}"
      else "")
}

object Tool {

  implicit val toolDecoder: Decoder[Tool] = deriveDecoder[Tool]
  implicit val toolEncoder: Encoder[Tool] = deriveEncoder[Tool]
}

case class Action(
                   tool: Tool,
                   command: ActionCommand,
                   result: List[String],
                   uuid: String = UUID.randomUUID().toString,
                 ) {

  def mkString: String =
    s"Tool used: ${tool.toolName} Result: ${result.mkString(", ")}"
}

object Action {

  implicit val actionDecoder: Decoder[Action] = deriveDecoder[Action]
  implicit val actionEncoder: Encoder[Action] = deriveEncoder[Action]
}

case class ActionCommand(
                          tool: String,
                          parameters: String
                        )

object ActionCommand {

  implicit val actionCommandDecoder: Decoder[ActionCommand] = deriveDecoder[ActionCommand]
  implicit val actionCommandEncoder: Encoder[ActionCommand] = deriveEncoder[ActionCommand]
}