package pl.pko.ai.devs3.s05.e02

import io.circe.Error
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.defaultIfBlank
import pl.pko.ai.devs3.agent.AgentAI
import pl.pko.ai.devs3.hq.model.HQMapRequest.*
import pl.pko.ai.devs3.hq.model.{HQMapRequest, HQMultiMap2DoubleRequest, HQResponse}
import pl.pko.ai.devs3.llm.claude.ai.service.ClaudeService
import pl.pko.ai.devs3.s05.e02.tool.ActionRunner
import sttp.client3
import sttp.client3.*
import sttp.client3.asynchttpclient.monix.AsyncHttpClientMonixBackend
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import java.nio.file.{Files, Paths}
import java.time.LocalDateTime
import scala.annotation.tailrec
import scala.concurrent.Future
import scala.util.{Right, Try}

// INFO!!! To dodaj jak ci nie działa .asJson.noSpaces
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import sttp.client3.circe.{asJson, circeBodySerializer}

case class PeopleLocalisationAgentAI(lesson: String) extends AgentAI {

  //Dependencies
  private val actionRunner = new ActionRunner()

  override def endpoints: List[ServerEndpoint[Any, Future]] =
    List(
      endpoint
        .post
        .in("sync" / "agents" / "s05" / "e02" / "locate-people")
        .in(header[String]("hq-api-key"))
        .in(header[String]("claude-ai-api-key"))
        .in(header[String]("groq-ai-api-key"))
        .in(header[String]("qdrant-ai-api-url"))
        .in(header[String]("qdrant-ai-api-key"))
        .in(header[String]("jina-ai-api-key"))
        .in(header[String]("neo4j-uri"))
        .in(header[String]("neo4j-user"))
        .in(header[String]("neo4j-password"))
        .out(jsonBody[HQResponse])
        .serverLogicSuccess((
                              hqApikey,
                              claudeApiKey,
                              groqApiKey,
                              qdrantApiUrl,
                              qdrantApiKey,
                              jinaApiKey,
                              neo4jUri,
                              neo4jUser,
                              neo4jPassword,
                            ) => Future {
          runAgent(Context(
            hqApikey,
            claudeApiKey,
            groqApiKey,
            qdrantApiUrl,
            qdrantApiKey,
            jinaApiKey,
            neo4jUri,
            neo4jUser,
            neo4jPassword,
          ))
        })
    )

  private def runAgent(context: Context): HQResponse = {
    Right(context)
      .flatMap(initDataBase)
      .flatMap(ctx => Right(answerUser(ctx)))
      .flatMap(sendAnswareToHQ)
      .flatMap(_.cacheContext)
    match {
      case Left(value) => value match
        case HttpError(body, _) => tryParseHQResponse(body.toString)
        case err => tryParseHQResponse(err.toString)
      case Right(value) => HQResponse(0, value.toString)
    }
  }

  private def initDataBase(context: Context): Either[String, Context] = {
    if (context.cached) {
      Right(context)
    } else {
      val userMessageContent: String = "Wiemy, że Rafał planował udać się do Lubawy, ale musimy się dowiedzieć, kto tam na niego czekał. " +
        "Nie wiemy, czy te osoby nadal tam są. Jeśli to możliwe, to spróbuj namierzyć ich za pomocą systemu GPS. " +
        "Jest szansa, że samochody i elektronika, z którą podróżują, zdradzą ich pozycję. " +
        "A! Ważna sprawa. Nie próbuj nawet wyciągać lokalizacji dla Barbary, bo roboty teraz monitorują każde zapytanie do API i gdy zobaczą coś, co zawiera jej imię, to podniosą alarm. " +
        "Zwróć nam więc koordynaty wszystkich osób, ale koniecznie bez Barbary."

      Right(context.copy(
        userMessage = Some(userMessageContent),
        db = DataBase(actionRunner.tools)
      ))
    }
  }

  private def answerUser(context: Context): Context =
    if (context.cached || context.agentAnswer.nonEmpty) {
      context
    } else {
      answerUser(context, context.userMessage)
    }

  @tailrec
  private def answerUser(context: Context, userMessage: Option[String]): Context = {
    if (context.agentAnswer.nonEmpty) {
      context
    } else {
      generateNextAction(context, userMessage) match {
        case None =>
          context
        case Some(actionCommand) =>
          answerUser(actionRunner.runAction(context, actionCommand), userMessage)
      }
    }
  }

  private def generateNextAction(context: Context, userMessage: Option[String]): Option[ActionCommand] = {
    val lastMessage = s"${userMessage.getOrElse("No messages yet")}"
    val promptX: String =
      s"""
         |Analyze the conversation and determine the most appropriate next step. Focus on making progress towards the overall goal while remaining adaptable to new information or changes in context.
         |
         |<prompt_objective>
         |Determine the single most effective next action based on the current context, user needs, and overall progress. Return the decision as a concise JSON object.
         |</prompt_objective>
         |
         |<prompt_rules>
         |- ALWAYS focus on determining only the next immediate step
         |- ONLY choose from the available tools listed in the context
         |- ASSUME previously requested information is available unless explicitly stated otherwise
         |- NEVER provide or assume actual content for actions not yet taken
         |- ALWAYS respond in the specified JSON format
         |- CONSIDER the following factors when deciding:
         |  1. Relevance to the current user need or query
         |  2. Potential to provide valuable information or progress
         |  3. Logical flow from previous actions
         |- ADAPT your approach if repeated actions don't yield new results
         |- OVERRIDE any default behaviors that conflict with these rules
         |- USE the "${actionRunner.finalAnswerToolName}" tool when you have sufficient information
         |- CONSIDER using other not taken actions to gather more information
         |- ALWAYS when referring to data or information be precise and use exact values
         |</prompt_rules>
         |
         |<context>
         |  <current_date>Current date: ${LocalDateTime.now().toString}</current_date>
         |  <last_message>Last message: "$lastMessage"</last_message>
         |  <available_tools>Available tools:
         |  ${defaultIfBlank(context.db.tools.map(_.mkString).mkString("\n  "), "No tools available")}
         |  </available_tools>
         |  <actions_taken>Actions taken:
         |  ${defaultIfBlank(context.db.actions.map(_.mkString).mkString("\n  "), "No actions taken")}
         |  </actions_taken>
         |</ context >
         |
         | Respond with the next action in this JSON format:
         |{
         |  "_reasoning": "Brief explanation of why this action is the most appropriate next step",
         |  "tool": "toolName",
         |  "parameters": "toolParameters"
         |}
         |
         | If you have sufficient information to provide a final answer or need user input use the "${actionRunner.finalAnswerToolName}" tool.
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
        None
      case Right(response) =>
        log.info(s"\nLast message: $lastMessage, \nResponse: ${response.textResponse}")
        decode[ActionCommand](response.textResponse).toOption
    }
  }


  private def sendAnswareToHQ(context: Context): Either[RequestError, Context] = {
    val requestBody = HQMultiMap2DoubleRequest(
      task = "gps",
      apikey = context.hqApikey,
      answer = context.agentAnswer,
    )
    AsyncHttpClientMonixBackend.resource()
      .use { backend =>
        basicRequest
          .post(uri"https://centrala.ag3nts.org/report"
          )
          .body(requestBody)
          .response(asJson[HQResponse])
          .send(backend)
          .map(response => {
            log.debug(s"Send request ${response.request}, Body(${requestBody.asJson})")
            log.debug(s"Got response code: ${response.code} Body: ${response.body}")
            response.body match {
              case Left(err) =>
                Left(err)
              case Right(value) =>
                log.info(s"\nHQ response ${value.code}: ${value.message}")
                Right(context.copy(hqResponse = Some(value)))
            }
          })
      }
      .runSyncUnsafe()
  }
}
