package pl.pko.ai.devs3.s05.e01.part2

import io.circe.Error
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.defaultIfBlank
import pl.pko.ai.devs3.agent.AgentAI
import pl.pko.ai.devs3.hq.model.HQMapRequest.*
import pl.pko.ai.devs3.hq.model.{HQMapRequest, HQResponse}
import pl.pko.ai.devs3.llm.claude.ai.service.ClaudeService
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
import sttp.client3.circe.{asJson, circeBodySerializer}
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*

/**
 * PODDAJE SIĘ:
 *
 * Wkleiłem wszystkie transkrypcje i fakty do jednego pliku wysłąłem do Claude i po prostu zadawałem pytania
 *
 * curl --request GET \
 * --url https://centrala.ag3nts.org/report \
 * --header 'Content-Type: application/json' \
 * --header 'User-Agent: insomnia/10.1.1' \
 * --data '{
 * "task":"phone",
 * "apikey":"d0e06171-6ebe-42b7-927b-0784be87fb58",
 * "answer": {
 * "01": "Samuel",
 * "02": "https://rafal.ag3nts.org/b46c3",
 * "03": "Nauczyciel",
 * "04": "Barbara, Samuel",
 * "05": "43a5ade38f9e4efaf91a6b04ba25725b",
 * "06": "Aleksander Ragowski"
 * }
 * }'
 *
 * {
 * "code": 0,
 * "message": "{{FLG:MYKINGDOM}}"
 * }
 */
case class AnswerQuestionsAgentAI(lesson: String) extends AgentAI {

  //Dependencies
  private val actionRunner = new ActionRunner()

  val dataDir = "src/main/scala/pl/pko/ai/devs3/s05/e01/data"

  override def endpoints: List[ServerEndpoint[Any, Future]] =
    List(
      endpoint
        .post
        .in("sync" / "agents" / "s05" / "e01" / "answer-questions")
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
      .flatMap(ctx => Right(answerQuestions(ctx)))
      .flatMap(_.cacheContext)
      .flatMap(sendAnswareToHQ)
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
      val phoneCallsFilePath = s"$dataDir/phone_sorted.json"
      val factsDirPath = s"$dataDir/facts"
      val questionsFilePath = s"$dataDir/phone_questions.json"

      val questionsT = for {
        questionsFileContent <- Try {
          new String(Files.readAllBytes(Paths.get(questionsFilePath)))
        }
        questions <- Try {
          decode[Map[String, String]](questionsFileContent).toOption.get
        }
      } yield questions

      Right(context.copy(
        questions = questionsT.toOption.getOrElse(Map.empty),
        db = DataBase(phoneCallsFilePath, factsDirPath, actionRunner.tools)
      ))
    }
  }

  private def answerQuestions(context: Context): Context =
    if (context.cached || context.hasAllAnswerer) {
      context
    } else {
      context.questions.keySet.foldLeft(context) {
        case (ctx, questionId) =>
          answerQuestion(ctx, questionId)
      }
    }

  @tailrec
  private def answerQuestion(context: Context, questionId: String): Context = {
    if (context.isQuestionAnswered(questionId)) {
      context
    } else {
      generateNextAction(context, questionId) match {
        case None =>
          context
        case Some(actionCommand) if actionRunner.isFinalAnswerTool(context, actionCommand) =>
          
          answerQuestion(actionRunner.runAction(
            context.copy(db = context.db.deleteActions()), 
            actionCommand.copy(parameters = s"$questionId: ${actionCommand.parameters}")
          ), questionId)
        case Some(actionCommand) =>
          answerQuestion(actionRunner.runAction(context, actionCommand), questionId)
      }
    }
  }

  private def generateNextAction(context: Context, questionId: String): Option[ActionCommand] = {
    val lastMessage = s"Find brief answer to question: ${context.getQuestion(questionId).getOrElse("No messages yet")}"
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
    val requestBody = HQMapRequest(
      task = "phone",
      apikey = context.hqApikey,
      answer = context.answerers
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
