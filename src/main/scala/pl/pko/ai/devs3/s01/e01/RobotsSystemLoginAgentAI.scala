package pl.pko.ai.devs3.s01.e01

import io.circe
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.{Error, Json}
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.jsoup.Jsoup
import pl.pko.ai.devs3.agent.AgentAI
import pl.pko.ai.devs3.hq.model.{HQRequest, HQResponse}
import pl.pko.ai.devs3.llm.ollama.model.OllamaResponse
import sttp.capabilities.WebSockets
import sttp.capabilities.monix.MonixStreams
import sttp.client3
import sttp.client3.*
import sttp.client3.asynchttpclient.monix.AsyncHttpClientMonixBackend
import sttp.client3.circe.{asJson, circeBodySerializer}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import java.net.URLEncoder
import scala.concurrent.Future

//OLLAMA_HOST=127.0.0.1:11435 ollama serve


/**
 * <!DOCTYPE html>
 * <html lang="en">
 *
 * <head>
 * <meta charset="UTF-8">
 * <meta name="viewport" content="width=device-width, initial-scale=1.0">
 * <title>XYZ - firmware upgrade</title>
 * <link rel="stylesheet" type="text/css" href="/css/bootstrap.min.css">
 * <link rel="stylesheet" type="text/css" href="/css/fontawesome-all.min.css">
 * <link rel="stylesheet" type="text/css" href="/css/iofrm-style.css">
 * <link rel="stylesheet" type="text/css" href="/css/iofrm-theme40.css">
 * <style>
 * dt.old {
 * text-decoration: line-through;
 * color: #8b8b8b
 * }
 * </style>
 * </head>
 *
 * <body>
 * <div class="form-body without-side">
 * <div class="iofrm-layout">
 * <div class="form-holder">
 * <div class="form-content">
 * <div class="form-items">
 * <h3 class="font-md">Download section</h3>
 * <p>Download the latest software for your zone patrolling robot.</p>
 * <dl>
 * <dt><a href="/files/0_13_4b.txt">Version 0.13.4b</a></dt>
 * <dd>- robot no longer kills people<br />- some other stability improvements</dd>
 * <dt class="old">Version 0.13.4</dt>
 * <dd>- security improvements</dd>
 *
 * <dt class="old">Version 0.12.1</dt>
 * <dd>- added some extra security</dd>
 * </dl>
 * <h2 style="background:#f4ffaa;font-family:monospace">{{FLG:FIRMWARE}}</h2>
 * </div>
 * </div>
 * </div>
 * </div>
 * </div>
 * <script src="/js/jquery.min.js"></script>
 * <script src="/js/popper.min.js"></script>
 * <script src="/js/bootstrap.bundle.min.js"></script>
 * <script src="/js/main.js"></script>
 * </body>
 *
 * </html>
 */
case class RobotsSystemLoginAgentAI(lesson: String) extends AgentAI {

  override def endpoints: List[ServerEndpoint[Any, Future]] =
    List(
      endpoint
        .post
        .in("sync" / "agents" / "s01e01" / "login")
        .in(header[String]("open-ai-api-key"))
        .out(jsonBody[HQResponse])
        .serverLogicSuccess(hqApiKey => Future {
          getDataAndSendToHQ(hqApiKey)
        })
    )

  private def getDataAndSendToHQ(hqApiKey: String): HQResponse = {

    scrapLoginPageUsingFirecrawl()
      .flatMap(extractQuestion)
      .flatMap(searchGoogleForAnswer)
      .flatMap(extractTextFromHtml)
      .flatMap(findAnsware)
      .map(logInToRobotSystem)
    match {
      case Left(value) => value match
        case HttpError(body, _) => tryParseHQResponse(body)
        case err => tryParseHQResponse(err.toString)
      case Right(value) => HQResponse(0, value.toString)
    }
  }

  private def logInToRobotSystem(ollamaResponse: OllamaResponse): Either[String, String] =
    AsyncHttpClientMonixBackend.resource()
      .use { backend => logInToRobotSystemRequest(backend, ollamaResponse) }
      .runSyncUnsafe()

  private def logInToRobotSystemRequest(backend: SttpBackend[Task, MonixStreams & WebSockets], ollamaResponse: OllamaResponse): Task[Either[String, String]] = {
    val username = "tester" // Replace with actual username
    val password = "574e112a" // Replace with actual password
    val answer = ollamaResponse.response.replaceAll("\\s", "") // The answer extracted from the OllamaResponse

    val formData = Map(
      "username" -> username,
      "password" -> password,
      "answer" -> answer
    )
    log.info(formData.mkString(", "))

    basicRequest
      .post(uri"http://xyz.ag3nts.org/")
      .contentType("application/x-www-form-urlencoded")
      .body(formData)
      .response(asString)
      .send(backend)
      .map { response =>
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response.body
      }
  }

  private def findAnsware(googleSearchResults: GoogleSerchResult): Either[ResponseException[String, Error], OllamaResponse] =
    AsyncHttpClientMonixBackend.resource()
      .use { backend => findAnswareJustAskRequest(backend, googleSearchResults) }
      .runSyncUnsafe()

  private def findAnswareJustAskRequest(backend: SttpBackend[Task, MonixStreams & WebSockets], googleSearchResults: GoogleSerchResult): Task[Either[ResponseException[String, Error], OllamaResponse]] = {
    val prompt =
      s"""
         |From now on you're a Search engine.
         |
         |<objective>
         |Your only task is to search your database and find answer to question.
         |</objective>
         |
         |<rules>
         |- UNDER NO CIRCUMSTANCES provide explanations or additional text
         |- Answer for question is always a year as an number
         |</rules>
         |
         |<question>
         |${googleSearchResults.ollamaResponse.response}
         |</question>
         |""".stripMargin

    val requestBody = s"""{ "model": "gemma2:2b", "prompt": ${Json.fromString(prompt)}, "stream": false }"""
    log.info(requestBody)

    basicRequest
      .post(uri"http://localhost:11434/api/generate")
      .body(requestBody)
      .response(asJson[OllamaResponse])
      .send(backend)
      .map { response =>
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response.body
      }
  }

  private def findAnswareRequest(backend: SttpBackend[Task, MonixStreams & WebSockets], googleSearchResults: GoogleSerchResult): Task[Either[ResponseException[String, Error], OllamaResponse]] = {
    val prompt = s"""
      |From now on you're a Search engine.
      |
      |<objective>
      |Your only task is to analyze the input and extract answer to question.
      |</objective>
      |
      |<rules>
      |- Always ANSWER with exact question answer from the input.
      |- NEVER change the answer
      |- UNDER NO CIRCUMSTANCES provide explanations or additional text
      |- Answer for question is always a year as an number
      |</rules>
      |
      |<question>
      |${googleSearchResults.ollamaResponse.response}
      |</question>
      |
      |<input>
      |${googleSearchResults.googleResponse.drop(550).take(700)}
      |</input>
      |""".stripMargin

    val requestBody = s"""{ "model": "gemma2:2b", "prompt": ${Json.fromString(prompt)}, "stream": false }"""
    log.info(requestBody)

    basicRequest
      .post(uri"http://localhost:11434/api/generate")
      .body(requestBody)
      .response(asJson[OllamaResponse])
      .send(backend)
      .map { response =>
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response.body
      }
  }

  private def extractTextFromHtml(googleSearchResults: GoogleSerchResult): Either[String, GoogleSerchResult] =
    Right(googleSearchResults.copy(googleResponse = Jsoup.parse(googleSearchResults.googleResponse).text()))

  private def searchGoogleForAnswer(ollamaResponse: OllamaResponse): Either[String, GoogleSerchResult] =
    AsyncHttpClientMonixBackend.resource()
      .use { backend => searchGoogleForAnswerRequest(backend, ollamaResponse) }
      .runSyncUnsafe()

  private def searchGoogleForAnswerRequest(backend: SttpBackend[Task, MonixStreams & WebSockets], ollamaResponse: OllamaResponse): Task[Either[String, GoogleSerchResult]] =
    basicRequest
      .get(uri"https://www.google.pl/search?q=${URLEncoder.encode(ollamaResponse.response)}")
      .response(asString)
      .send(backend)
      .map { response =>
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response.body.fold(
          err => {
            Left(err)
          },
          response => Right(GoogleSerchResult(ollamaResponse, response))
        )
      }

  private def extractQuestion(loginPageScrapped: FirecrawlResponse): Either[ResponseException[String, Error], OllamaResponse] =
    AsyncHttpClientMonixBackend.resource()
      .use { backend => extractQuestionRequest(backend, loginPageScrapped) }
      .runSyncUnsafe()

  private def extractQuestionRequest(backend: SttpBackend[Task, MonixStreams & WebSockets], loginPageScrapped: FirecrawlResponse): Task[Either[ResponseException[String, Error], OllamaResponse]] = {
    val prompt = s"""
      |From now on you're a question Extractor.
      |
      |<objective>
      |Analyze the input and extract the question.
      |</objective>
      |
      |<rules>
      |- Always ANSWER with exact question from the input.
      |- NEVER change the question
      |- UNDER NO CIRCUMSTANCES provide explanations or additional text
      |</rules>
      |
      |<input>
      |${loginPageScrapped.data.markdown}
      |</input>
      |""".stripMargin

    val requestBody = s"""{ "model": "gemma2:2b", "prompt": ${Json.fromString(prompt)}, "stream": false }"""
    log.info(requestBody)

    basicRequest
      .post(uri"http://localhost:11434/api/generate")
      .body(requestBody)
      .response(asJson[OllamaResponse])
      .send(backend)
      .map { response =>
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response.body
      }
  }

  private def scrapLoginPageUsingFirecrawl(): Either[ResponseException[String, Error], FirecrawlResponse] =
    AsyncHttpClientMonixBackend.resource()
      .use { scrapUsingFirecrawlRequest }
      .runSyncUnsafe()

  private def scrapUsingFirecrawlRequest(backend: SttpBackend[Task, MonixStreams & WebSockets]): Task[Either[ResponseException[String, Error], FirecrawlResponse]] =
    basicRequest
      .post(uri"https://api.firecrawl.dev/v1/scrape")
      .headers(Map(
        "Content-Type" -> "application/json",
        "Authorization" -> "Bearer fc-13b40364017542f591a8d04c5aad55ae"
      ))
      .body("""{ "url": "http://xyz.ag3nts.org/", "formats": [ "markdown" ] }""")
      .response(asJson[FirecrawlResponse])
      .send(backend)
      .map { response =>
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response.body
      }

  private def doPostPoligonVeryfication(hqRequest: HQRequest): Either[ResponseException[String, Error], HQResponse] = {
    AsyncHttpClientMonixBackend.resource()
      .use { backend => postPoligonVeryfication(backend, hqRequest) }
      .runSyncUnsafe()
  }

  private def postResponseToHQ(backend: SttpBackend[Task, MonixStreams & WebSockets], hqRequest: HQResponse): Task[Either[ResponseException[String, Error], HQResponse]] =
    basicRequest
      .body(hqRequest)
      .post(uri"https://poligon.aidevs.pl/verify")
      .response(asJson[HQResponse])
      .send(backend)
      .map(response => {
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response.body
      })

  private def postPoligonVeryfication(backend: SttpBackend[Task, MonixStreams & WebSockets], hqRequest: HQRequest): Task[Either[ResponseException[String, Error], HQResponse]] =
    basicRequest
      .body(hqRequest)
      .post(uri"https://poligon.aidevs.pl/verify")
      .response(asJson[HQResponse])
      .send(backend)
      .map(response => {
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response.body
      })
}