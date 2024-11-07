package pl.pko.ai.devs3.llm.ollama.service

import io.circe.{Error, Json}
import monix.eval.Task
import sttp.client3.circe.asJson
import org.slf4j.{Logger, LoggerFactory}
import pl.pko.ai.devs3.llm.ollama.model.OllamaResponse
import sttp.capabilities.WebSockets
import sttp.capabilities.monix.MonixStreams
import sttp.client3.{Response, ResponseException, SttpBackend, UriContext, basicRequest}

object OllamaService {

  private val log: Logger = LoggerFactory.getLogger(getClass)

  def sendPrompt(backend: SttpBackend[Task, MonixStreams & WebSockets], prompt: String): Task[Response[Either[ResponseException[String, Error], OllamaResponse]]] = {
    val requestBody = s"""{ "model": "gemma2:2b", "prompt": ${Json.fromString(prompt)}, "stream": false }"""
    log.info(requestBody)

    basicRequest
      .post(uri"http://localhost:11434/api/generate")
      .body(requestBody)
      .response(asJson[OllamaResponse])
      .send(backend)
      .map { response =>
        log.info(s"Send request ${response.request}, Body($requestBody)")
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response
      }
  }
}
