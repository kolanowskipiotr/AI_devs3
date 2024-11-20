package pl.pko.ai.devs3.s03.e03

import io.circe.Error
import io.circe.syntax.*
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.substringBetween
import pl.pko.ai.devs3.agent.AgentAI
import pl.pko.ai.devs3.hq.model.HQResponse
import pl.pko.ai.devs3.llm.claude.ai.service.ClaudeService
import pl.pko.ai.devs3.s03.e03.BanAN.db.model.*
import sttp.client3
import sttp.client3.*
import sttp.client3.asynchttpclient.monix.AsyncHttpClientMonixBackend
import sttp.client3.circe.{asJson, circeBodySerializer}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

case class SQLDataBaseAgentAI(lesson: String) extends AgentAI {

  override def endpoints: List[ServerEndpoint[Any, Future]] =
    List(
      endpoint
        .post
        .in("sync" / "agents" / "s03" / "e03" / "run")
        .in(header[String]("hq-api-key"))
        .in(header[String]("claude-ai-api-key"))
        .in(header[String]("groq-ai-api-key"))
        .in(header[String]("qdrant-ai-api-url"))
        .in(header[String]("qdrant-ai-api-key"))
        .in(header[String]("jina-ai-api-key"))
        .out(jsonBody[HQResponse])
        .serverLogicSuccess((hqApikey, claudeApiKey, groqApiKey, qdrantApiUrl, qdrantApiKey, jinaApiKey) => Future {
          getDataAndSendToHQ(Context(hqApikey, claudeApiKey, groqApiKey, qdrantApiUrl, qdrantApiKey, jinaApiKey))
        })
    )

  private def getDataAndSendToHQ(context: Context): HQResponse = {
    Right(context.copy(
      question = Some(Question(
        content = Some("które aktywne datacenter (DC_ID) są zarządzane przez pracowników, którzy są na urlopie (is_active=0)")
      ))))
      .flatMap(readTableNames)
      .flatMap(readTablesStructure)
      .flatMap(generateSQlToAnswareQuestion)
      .flatMap(getDataCenterIds)
      .flatMap(postReportToHQ)
    match {
      case Left(value) => value match
        case HttpError(body, _) => tryParseHQResponse(body.toString)
        case err => tryParseHQResponse(err.toString)
      case Right(value) => HQResponse(0, value.toString)
    }
  }

  private def getDataCenterIds(context: Context): Either[RequestError, Context] =
    AsyncHttpClientMonixBackend.resource()
      .use { backend =>
        val requestBody = TaskRequest(
          task = "database",
          apikey = context.hqApikey,
          query = context.generatedSQL.get
        )
        basicRequest
          .post(uri"https://centrala.ag3nts.org/apidb")
          .body(requestBody)
          .response(asJson[SelectResponse])
          .send(backend)
          .map(response => {
            log.info(s"Send request ${response.request}, Body(${requestBody.asJson})")
            log.info(s"Got response code: ${response.code} Body: ${response.body}")
            response.body match
              case Right(value) => Right(context.copy(generatedSQLResult = Some(value.reply.map(_.dc_id))))
              case Left(value) => Left(value)
          })
      }
      .runSyncUnsafe()

  private def generateSQlToAnswareQuestion(context: Context): Either[RequestError, Context] = {
    val resultStart = """<result>"""
    val resultEnd = """</result>"""
    val prompt =
      s"""
         |You are a SQL developer. Your task is create SQl query to answer the <question>
         |
         |<rules>
         |- The structure of database ids provided in <db_context>
         |- Always find relations between tables
         |- Question in <question> is provided in Polish language
         |- <db_context> is in English language
         |</rules>
         |
         |<db_context>
         |${context.readTableStructure.get.map(_.reply.head).map(table => s"${table.Table}: ${table.`Create Table`}").mkString("\n")}
         |</db_context>
         |
         |<question>
         |${context.question.map(_.content).getOrElse("")}
         |<question>
         |
         |Provide SQL query in the following format:
         |${resultStart}Created SQL query$resultEnd
         |followed by a brief justification.
         |""".stripMargin
    AsyncHttpClientMonixBackend.resource()
      .use { backend => ClaudeService.sendPrompt(backend, context.claudeApiKey, prompt) }
      .runSyncUnsafe()
      .body match {
      case Left(response) => Left(response)
      case Right(response) => Right(context.copy(
        generatedSQL = Some(substringBetween(response.textResponse, resultStart, resultEnd))
      ))
    }
  }

  private def readTablesStructure(context: Context): Either[String, Context] =
    Right(context.copy(readTableStructure = context.readTableNames.map(tableName => {
      tableName.reply.map(readTableStructure(context, _)) map {
        case Right(value) => value
        case Left(value) => throw IllegalStateException(value)
      }
    })))

  private def readTableStructure(context: Context, tableName: TableResponse): Either[RequestError, TableStructureResponse] =
    AsyncHttpClientMonixBackend.resource()
      .use { backend =>
        val requestBody = TaskRequest(
          task = "database",
          apikey = context.hqApikey,
          query = s"show create table ${tableName.`Tables_in_banan`}"
        )
        basicRequest
          .post(uri"https://centrala.ag3nts.org/apidb")
          .body(requestBody)
          .response(asJson[TableStructureResponse])
          .send(backend)
          .map(response => {
            log.info(s"Send request ${response.request}, Body(${requestBody.asJson})")
            log.info(s"Got response code: ${response.code} Body: ${response.body}")
            response.body
          })
      }
      .runSyncUnsafe()

  private def readTableNames(context: Context): Either[RequestError, Context] =
    AsyncHttpClientMonixBackend.resource()
      .use { backend =>
        val requestBody = TaskRequest(
          task = "database",
          apikey = context.hqApikey,
          query = "show tables"
        )
        basicRequest
          .post(uri"https://centrala.ag3nts.org/apidb")
          .body(requestBody)
          .response(asJson[ShowTablesResponse])
          .send(backend)
          .map(response => {
            log.info(s"Send request ${response.request}, Body(${requestBody.asJson})")
            log.info(s"Got response code: ${response.code} Body: ${response.body}")
            response.body match
              case Right(value) => Right(context.copy(readTableNames = Some(value)))
              case Left(value) => Left(value)
          })
      }
      .runSyncUnsafe()

  private def postReportToHQ(context: Context): Either[RequestError, HQResponse] =
    AsyncHttpClientMonixBackend.resource()
      .use { backend => {
        val requestBody = HQReportRequest(
          task = "database",
          apikey = context.hqApikey,
          answer = context.generatedSQLResult.get
        )
        basicRequest
          .post(uri"https://centrala.ag3nts.org/report")
          .body(requestBody)
          .response(asJson[HQResponse])
          .send(backend)
          .map(response => {
            log.info(s"Send request ${response.request}, Body(${requestBody.asJson})")
            log.info(s"Got response code: ${response.code} Body: ${response.body}")
            response.body
          })
      }
      }
      .runSyncUnsafe()
}
