package pl.pko.ai.devs3.llm.groq.service

import io.circe.{Error, Json}
import monix.eval.Task
import org.slf4j.{Logger, LoggerFactory}
import pl.pko.ai.devs3.llm.claude.ai.model.ClaudeResponse
import pl.pko.ai.devs3.llm.groq.model.GroqResponse
import sttp.capabilities.WebSockets
import sttp.capabilities.monix.MonixStreams
import sttp.client3.circe.asJson
import sttp.client3.{Response, ResponseException, SttpBackend, UriContext, basicRequest, multipart, multipartFile}

import java.nio.file.Path

object GroqService {

  private val log: Logger = LoggerFactory.getLogger(getClass)

  def sendAudioTranscription(backend: SttpBackend[Task, MonixStreams & WebSockets], apiKey: String, filePath: Path): Task[Response[Either[ResponseException[String, Error], GroqResponse]]] = {
    basicRequest
      .post(uri"https://api.groq.com/openai/v1/audio/transcriptions")
      .header("Authorization", s"bearer $apiKey")
      .multipartBody(
        multipartFile("file", filePath),
        multipart("model", "whisper-large-v3"),
        multipart("temperature", "0"),
        multipart("response_format", "json"),
        multipart("language", "en")
      )
      .response(asJson[GroqResponse])
      .send(backend)
      .map { response =>
        log.info(s"Send request ${response.request}, $filePath")
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response
      }
  }
}
