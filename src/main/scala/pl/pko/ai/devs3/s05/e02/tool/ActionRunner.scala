package pl.pko.ai.devs3.s05.e02.tool

import io.circe.generic.auto.*
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import monix.execution.Scheduler.Implicits.global
import org.apache.commons.lang3.StringUtils
import pl.pko.ai.devs3.hq.model.HQResponse
import pl.pko.ai.devs3.llm.claude.ai.service.ClaudeService
import pl.pko.ai.devs3.s05.e02.{ActionCommand, Context, Tool}
import sttp.client3.asynchttpclient.monix.AsyncHttpClientMonixBackend
import sttp.client3.circe.{asJson, circeBodySerializer}
import sttp.client3.{Identity, RequestT, UriContext, basicRequest}
import sttp.model.Method

class ActionRunner {

  private val log = org.slf4j.LoggerFactory.getLogger(getClass)

  val finalAnswerToolName = "FINAL_ANSWER_TOOL"

  val tools: List[Tool] = List(
    Tool(
      toolName = finalAnswerToolName,
      description = "Tool responds the final answer to User",
      parameters = "Yur final answer to user need or query",
      usageExamples = List(ActionCommand(finalAnswerToolName, "Wrocław Zabobrze")),
    ),
    Tool(
      toolName = "FIND_PEOPLE_IN_LOCATION_TOOL",
      description = "Tool retrieves people that were sported in provided location",
      parameters = "Location name to search for people",
      usageExamples = List(
        ActionCommand("FIND_PEOPLE_IN_LOCATION_TOOL", "Lubawa")
      ),
    ),
    Tool(
      toolName = "GET_PERSON_ID_TOOL",
      description = "Tool retrieves person ID based on provided name",
      parameters = "Person name to search for ID",
      usageExamples = List(
        ActionCommand("GET_PERSON_ID_TOOL", "SAMUEL")
      ),
    ),
    Tool(
      toolName = "GET_PERSON_GPS_COORDINATES_TOOL",
      description = "Tool retrieves person GPS coordinates based on provided ID",
      parameters = "Person ID to search for GPS coordinates",
      usageExamples = List(
        ActionCommand("GET_PERSON_GPS_COORDINATES_TOOL", "98")
      ),
    )
  )

  def isFinalAnswerTool(context: Context, command: ActionCommand): Boolean =
    extractTool(context, command).exists(_.toolName == finalAnswerToolName)

  def extractTool(context: Context, command: ActionCommand): Option[Tool] =
    context.db.tools.find(tool => command.tool.toLowerCase.contains(tool.toolName.toLowerCase))

  def runAction(context: Context, command: ActionCommand): Context = {
    extractTool(context, command) match {
      case Some(tool) =>
        runActionWithTool(context, tool, command)
      case None =>
        log.error(s"Tool not found for command: $command")
        context
    }
  }

  private def runActionWithTool(context: Context, tool: Tool, command: ActionCommand): Context = {
    tool.toolName match
      case toolName if toolName == finalAnswerToolName => answerQuestion(context, tool, command)
      case "FIND_PEOPLE_IN_LOCATION_TOOL" => findPeopleLocations(context, tool, command)
      case "GET_PERSON_ID_TOOL" => getPersonId(context, tool, command)
      case "GET_PERSON_GPS_COORDINATES_TOOL" => getPersonGPSCoordinates(context, tool, command)
      case _ => context
  }

  private def answerQuestion(context: Context, tool: Tool, command: ActionCommand): Context = {
    log.info(s"\nAnswering user message ${context.userMessage} \nwith answer ${command.parameters}")
    val promptX: String =
      s"""
         |Convert data:
         |${command.parameters}
         |
         |to JSON format:
         |{
         |  "NAME": {
         |    "lat": 12.345,
         |    "lon": 65.431
         |  },
         |  "NEXT-NAME": {
         |    "lat": 19.433,
         |    "lon": 12.123
         |  }
         |}
         |
         |Example:
         |{
         |  "RAFAL": {
         |  	"lat": 53.451974,
         |	  "lon": 18.759189
         |  },
         |  "AZAZEL": {
         |	  "lat": 50.064851459004686,
         |		"lon": 19.94988170674601
         |  },
         |  "SAMUEL": {
         |		"lat": 53.50357079380177,
         |		"lon": 19.745866344712706
         |  }
         |}
         |
         |Respond only with generated JSON
        """.stripMargin

    AsyncHttpClientMonixBackend.resource()
      .use { backend =>
        ClaudeService.sendPrompt(
          backend = backend,
          apiKey = context.claudeApiKey,
          prompt = promptX)
      }
      .runSyncUnsafe()
      .body match {
      case Left(error) =>
        context.copy(
          db = context.db.saveAction(tool, command, error.toString)
        )
      case Right(response) =>
        decode[Map[String, Map[String, Double]]](response.textResponse) match
          case Left(error) =>
            context.copy(
              db = context.db.saveAction(tool, command, error.toString)
            )
          case Right(answer) =>
            log.info(s"\nResponse: ${response.textResponse}")
            context.copy(
              db = context.db.saveAction(tool, command, response.textResponse),
              agentAnswer = answer
            )
    }
  }

  private def findPeopleLocations(context: Context, tool: Tool, command: ActionCommand): Context = {
    val requestBody =
      s"""
         |{
         |  "apikey": "${context.hqApikey}",
         |  "query": "${command.parameters}"
         |}
         |""".stripMargin
    AsyncHttpClientMonixBackend.resource()
      .use { backend =>
        basicRequest
          .post(uri"https://centrala.ag3nts.org/places"
          )
          .body(requestBody)
          .response(asJson[HQResponse])
          .send(backend)
          .map(response => {
            log.debug(s"Send request ${response.request}, Body(${requestBody.asJson})")
            log.debug(s"Got response code: ${response.code} Body: ${response.body}")
            response.body match {
              case Left(error) =>
                context.copy(
                  db = context.db.saveAction(tool, command, error.toString)
                )
              case Right(value) =>
                log.info(s"\nResponse ${value.code}: ${value.message}")
                context.copy(
                  db = context.db.saveAction(tool, command, value.message)
                )
            }
          })
      }
      .runSyncUnsafe()
  }

  def getPersonGPSCoordinates(context: Context, tool: Tool, command: ActionCommand): Context = {
    val personId = "\\d+".r.findFirstIn(command.parameters).getOrElse("NO ID")

    val requestBody =
      s"""
         |{
         |  "userID": "$personId"
         |}
         |""".stripMargin

    AsyncHttpClientMonixBackend.resource()
      .use { backend =>
        basicRequest
          .post(uri"https://centrala.ag3nts.org/gps"
          )
          .body(requestBody)
          .response(asJson[GPSAPIResponse])
          .send(backend)
          .map(response => {
            log.debug(s"Send request ${response.request}, Body(${requestBody.asJson})")
            log.debug(s"Got response code: ${response.code} Body: ${response.body}")
            response.body match {
              case Left(error) =>
                context.copy(
                  db = context.db.saveAction(tool, command, error.toString)
                )
              case Right(value) =>
                log.info(s"\nResponse ${value.code}: ${value.message}")
                context.copy(
                  db = context.db.saveAction(tool, command, value.message.asJson.noSpaces)
                )
            }
          })
      }
      .runSyncUnsafe()
  }

  private def getPersonId(context: Context, tool: Tool, command: ActionCommand): Context = {
    val username = command
      .parameters
      .replace("\n", "")
      .replaceAll("\\s+", " ")
      .split(" ")
      .headOption
      .map(StringUtils.stripAccents)
      .map(_.toUpperCase)
      .getOrElse("NO NAME")

    val requestBody =
      s"""
         |{
         |  "apikey": "${context.hqApikey}",
         |  "task": "database",
         |  "query": "SELECT * from users where username = '$username';"
         |}
         |""".stripMargin

    AsyncHttpClientMonixBackend.resource()
      .use { backend =>
        basicRequest
          .post(uri"https://centrala.ag3nts.org/apidb"
          )
          .body(requestBody)
          .response(asJson[DataBaseResult])
          .send(backend)
          .map(response => {
            log.debug(s"Send request ${response.request}, Body(${requestBody.asJson})")
            log.debug(s"Got response code: ${response.code} Body: ${response.body}")
            response.body match {
              case Left(error) =>
                context.copy(
                  db = context.db.saveAction(tool, command, error.toString)
                )
              case Right(value) =>
                log.info(s"\nResponse $value")
                context.copy(
                  db = context.db.saveAction(tool, command, value.reply.head.id)
                )
            }
          })
      }
      .runSyncUnsafe()
  }

  /**
   * {
   * "reply": [
   * {
   * "id": "98",
   * "username": "Samuel",
   * "access_level": "user",
   * "is_active": "1",
   * "lastlog": "2024-11-29"
   * }
   * ],
   * "error": "OK"
   * }
   */
  case class DataBaseReply(
                            id: String,
                            username: String,
                            access_level: String,
                            is_active: String,
                            lastlog: String,
                          )

  object DataBaseReply {
    implicit val decoder: Decoder[DataBaseReply] = deriveDecoder[DataBaseReply]
    implicit val encoder: Encoder[DataBaseReply] = deriveEncoder[DataBaseReply]
  }

  case class DataBaseResult(
                             reply: List[DataBaseReply],
                             error: String,
                           )

  object DataBaseResult {
    implicit val decoder: Decoder[DataBaseResult] = deriveDecoder[DataBaseResult]
    implicit val encoder: Encoder[DataBaseResult] = deriveEncoder[DataBaseResult]
  }

  /**
   * {
   * "code": 0,
   * "message": {
   * "lat": 53.50357079380177,
   * "lon": 19.745866344712706
   * }
   * }
   */
  case class GPSAPIMessageResponse(
                                    lat: Double,
                                    lon: Double,
                                  )

  object GPSAPIMessageResponse {
    implicit val decoder: Decoder[GPSAPIMessageResponse] = deriveDecoder[GPSAPIMessageResponse]
    implicit val encoder: Encoder[GPSAPIMessageResponse] = deriveEncoder[GPSAPIMessageResponse]
  }

  case class GPSAPIResponse(
                             code: Int,
                             message: GPSAPIMessageResponse,
                           )

  object GPSAPIResponse {
    implicit val decoder: Decoder[GPSAPIResponse] = deriveDecoder[GPSAPIResponse]
    implicit val encoder: Encoder[GPSAPIResponse] = deriveEncoder[GPSAPIResponse]
  }

}