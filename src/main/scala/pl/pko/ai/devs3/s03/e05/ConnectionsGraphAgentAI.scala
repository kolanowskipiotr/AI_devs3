package pl.pko.ai.devs3.s03.e05

import io.circe.Error
import io.circe.syntax.*
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.neo4j.driver.exceptions.Neo4jException
import org.neo4j.driver.{AuthTokens, GraphDatabase}
import pl.pko.ai.devs3.agent.AgentAI
import pl.pko.ai.devs3.hq.model.HQResponse
import pl.pko.ai.devs3.s03.e03.BanAN.db.model.TaskRequest
import pl.pko.ai.devs3.s03.e05.BanAN.db.model.{ConnectionsResponse, UsersResponse}
import sttp.client3
import sttp.client3.*
import sttp.client3.asynchttpclient.monix.AsyncHttpClientMonixBackend
import sttp.client3.circe.{asJson, circeBodySerializer}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import java.nio.file.{Files, Paths, StandardOpenOption}
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.util.{Right, Using}

case class ConnectionsGraphAgentAI(lesson: String) extends AgentAI {

  override def endpoints: List[ServerEndpoint[Any, Future]] =
    List(
      endpoint
        .post
        .in("sync" / "agents" / "s03" / "e05" / "run")
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
          getDataAndSendToHQ(Context(
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

  private def getDataAndSendToHQ(context: Context): HQResponse = {
    Right(context.copy(startPerson = Some("Rafał"), endPerson = Some("Barbara")))
      .flatMap(loadUsersData)
      .flatMap(loadConnectionsData)
      .flatMap(checkNeo4jConnection)
      .flatMap(upsertConnectionsToNeo4j)
      .flatMap(findRoutFromStartToStopPerson)
      .flatMap(cacheContext)
      .flatMap(postAllReportToHQ)
    match {
      case Left(value) => value match
        case HttpError(body, _) => tryParseHQResponse(body.toString)
        case err => tryParseHQResponse(err.toString)
      case Right(value) => HQResponse(0, value.toString)
    }
  }

  private def findRoutFromStartToStopPerson(context: Context): Either[Neo4jException, Context] = {
    Using(GraphDatabase.driver(
      context.neo4jUri,
      AuthTokens.basic(context.neo4jUser, context.neo4jPassword)
    )) { driver =>
      val session = driver.session()
      try {
        session.executeRead(tx => {
          val query =
            s"""
               |MATCH path = (start:User {username: "${context.startPerson.getOrElse("")}"})-[:CONNECTED*1..10]-(end:User {username: "${context.endPerson.getOrElse("")}"})
               |RETURN path, length(path) AS steps
               |ORDER BY steps ASC
               |LIMIT 1;
               |""".stripMargin
          log.info(s"Executing query: $query")

          val result = tx.run(query)
          var resultScala = Map.empty[String, Int]
          while (result.hasNext) {
            val record = result.next()
            val path = record.get("path").asPath().nodes().asScala.map(node => node.get("username").asString()).mkString(", ")
            val steps = record.get("steps").asInt()
            log.info(s"Record - Path: $path, Steps: $steps")
            resultScala = resultScala + (path -> steps)
          }
          log.info(s"Path-Steps Map: ${resultScala.mkString(", ")}")

          resultScala
        })
      } finally {
        session.close()
      }
    } match {
      case scala.util.Success(result) =>
        if (result.isEmpty) {
          log.warn("No paths found between the specified users.")
          Left(Neo4jException("No paths found"))
        } else {
          Right(context.copy(rotes = result))
        }
      case scala.util.Failure(exception) =>
        log.error("Error executing Neo4j query", exception)
        Left(Neo4jException(exception.getMessage))
    }
  }

  private def upsertConnectionsToNeo4j(context: Context): Either[Neo4jException, Context] = {
    if(context.neo4jFed){
      Right(context)
    } else {
      Using(GraphDatabase.driver(
        context.neo4jUri,
        AuthTokens.basic(
          context.neo4jUser,
          context.neo4jPassword
        )
      )) { driver =>
        val session = driver.session()
        try {
          session.executeWrite(tx => {
            context.users.foreach(user => {
              tx.run(
                s"""
                   |MERGE (u:User {userid: "${user.id}", username: "${user.username}"})
                   |""".stripMargin
              )
              log.info(s"Upserted user ${user.id}: ${user.username}")
            })
            context.connections.foreach(connection => {
              tx.run(
                s"""
                   |MERGE (u1:User {userid: "${connection.user1_id}"})
                   |MERGE (u2:User {userid: "${connection.user2_id}"})
                   |MERGE (u1)-[:CONNECTED]->(u2)
                   |""".stripMargin
              )
              log.info(s"Upserted connection ${connection.user1_id} -> ${connection.user2_id}")
            })

            context
          })
        } finally {
          session.close()
        }
      } match {
        case scala.util.Success(ctx) => Right(ctx.copy(neo4jFed = true))
        case scala.util.Failure(exception) => Left(Neo4jException(exception.getMessage))
      }
    }
  }

  private def checkNeo4jConnection(context: Context): Either[Neo4jException, Context] = {
    Using(GraphDatabase.driver(
      context.neo4jUri,
      AuthTokens.basic(
        context.neo4jUser,
        context.neo4jPassword
      )
    )) { driver =>
      driver.verifyConnectivity()
      context
    } match {
      case scala.util.Success(ctx) => Right(ctx.copy(neo4jConnected = true))
      case scala.util.Failure(exception) => Left(Neo4jException(exception.getMessage))
    }
  }

  private def loadUsersData(context: Context): Either[RequestError, Context] =
    if(context.users.isEmpty) {
      AsyncHttpClientMonixBackend.resource()
        .use { backend =>
          val requestBody = TaskRequest(
            task = "database",
            apikey = context.hqApikey,
            query = "SELECT * FROM users;"
          )
          basicRequest
            .post(uri"https://centrala.ag3nts.org/apidb")
            .body(requestBody)
            .response(asJson[UsersResponse])
            .send(backend)
            .map(response => {
              log.info(s"Send request ${response.request}, Body(${requestBody.asJson})")
              log.info(s"Got response code: ${response.code} Body: ${response.body}")
              response.body match
                case Right(value) => Right(context.copy(users = value.reply))
                case Left(value) => Left(value)
            })
        }
        .runSyncUnsafe()
    } else {
      Right(context)
    }


  private def loadConnectionsData(context: Context): Either[RequestError, Context] =
    if(context.connections.isEmpty) {
    AsyncHttpClientMonixBackend.resource()
      .use { backend =>
        val requestBody = TaskRequest(
          task = "database",
          apikey = context.hqApikey,
          query = "SELECT * FROM connections;"
        )
        basicRequest
          .post(uri"https://centrala.ag3nts.org/apidb")
          .body(requestBody)
          .response(asJson[ConnectionsResponse])
          .send(backend)
          .map(response => {
            log.info(s"Send request ${response.request}, Body(${requestBody.asJson})")
            log.info(s"Got response code: ${response.code} Body: ${response.body}")
            response.body match
              case Right(value) => Right(context.copy(connections = value.reply))
              case Left(value) => Left(value)
          })
      }
      .runSyncUnsafe()
    } else {
      Right(context)
    }

  private def cacheContext(context: Context): Either[String, Context] = {
    val directory = Paths.get("src/main/scala/pl/pko/ai/devs3/s03/e05/cache")
    val filePath = directory.resolve("context.json")

    try {
      Files.write(
        filePath,
        context.anonimized.asJson.noSpaces.getBytes,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING
      )
      Right(context)
    } catch {
      case e: Exception => Left(e.getMessage)
    }
  }

  private def postAllReportToHQ(context: Context): Either[String, Context] = {
    context.rotes.keySet.foreach(rote => {
    val requestBody = HQReportRequest(
      task = "connections",
      apikey = context.hqApikey,
      answer = Some(rote)
    )
    AsyncHttpClientMonixBackend.resource()
      .use { backend =>
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
      .runSyncUnsafe()
    })
    Right(context.anonimized)
  }

}
