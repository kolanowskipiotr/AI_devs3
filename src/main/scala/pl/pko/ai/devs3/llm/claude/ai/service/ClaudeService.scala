package pl.pko.ai.devs3.llm.claude.ai.service

import io.circe.{Error, Json}
import monix.eval.Task
import org.slf4j.{Logger, LoggerFactory}
import pl.pko.ai.devs3.llm.claude.ai.model.ClaudeResponse
import sttp.capabilities.WebSockets
import sttp.capabilities.monix.MonixStreams
import sttp.client3.circe.asJson
import sttp.client3.{Response, ResponseException, SttpBackend, UriContext, basicRequest}

object ClaudeService {

  private val log: Logger = LoggerFactory.getLogger(getClass)

  def sendPrompt(backend: SttpBackend[Task, MonixStreams & WebSockets], apiKey: String, prompt: String): Task[Response[Either[ResponseException[String, Error], ClaudeResponse]]] = {
    val requestBody = s"""{
      |    "model": "claude-3-5-sonnet-20241022",
      |    "max_tokens": 1024,
      |    "messages": [
      |        {"role": "user", "content": "${Json.fromString(prompt)}"}
      |    ]
      |}""".stripMargin
    log.info(requestBody)

    basicRequest
      .post(uri"https://api.anthropic.com/v1/messages")
      .header("x-api-key", apiKey)
      .header("anthropic-version", "2024-10-22")
      .header("content-type", "application/json")
      .body(requestBody)
      .response(asJson[ClaudeResponse])
      .send(backend)
      .map { response =>
        log.info(s"Send request ${response.request}, Body($requestBody)")
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response
      }
  }
}
