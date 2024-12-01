package pl.pko.ai.devs3.llm.claude.ai.service

import io.circe.{Error, Json}
import monix.eval.Task
import org.slf4j.{Logger, LoggerFactory}
import pl.pko.ai.devs3.llm.claude.ai.model.ClaudeResponse
import sttp.capabilities.WebSockets
import sttp.capabilities.monix.MonixStreams
import sttp.client3.circe.asJson
import sttp.client3.{Response, ResponseException, SttpBackend, UriContext, basicRequest, multipart, multipartFile}

import java.nio.file.Path

object ClaudeService {

  private val log: Logger = LoggerFactory.getLogger(getClass)

  def sendPrompt(backend: SttpBackend[Task, MonixStreams & WebSockets], apiKey: String, prompt: String): Task[Response[Either[ResponseException[String, Error], ClaudeResponse]]] = {
    val requestBody =
      s"""{
         |    "model": "claude-3-5-sonnet-20241022",
         |    "max_tokens": 1024,
         |    "messages": [
         |        {"role": "user", "content": ${Json.fromString(prompt)}}
         |    ]
         |}""".stripMargin
    log.info(requestBody)

    basicRequest
      .post(uri"https://api.anthropic.com/v1/messages")
      .header("x-api-key", apiKey)
      .header("anthropic-version", "2023-06-01")
      .header("content-type", "application/json")
      .body(requestBody)
      .response(asJson[ClaudeResponse])
      .send(backend)
      .map { response =>
        log.info(s"Send request ${response.request}, Body(${requestBody.replace("\\n", "\n")})")
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response
      }
  }

  def sendImageAnalisePrompt(
                              backend: SttpBackend[Task, MonixStreams & WebSockets],
                              apiKey: String,
                              imageBase64: Option[String],
                              imageMimeType: String,
                              systemPrompt: String
                            ): Task[Response[Either[ResponseException[String, Error], ClaudeResponse]]] = {
    val requestBody =
      s"""{
         |  "model": "claude-3-5-sonnet-20241022",
         |  "max_tokens": 1024,
         |  "messages": [
         |    {
         |      "role": "user",
         |      "content": [
         |        {
         |          "type": "image",
         |          "source": {
         |            "type": "base64",
         |            "media_type": "image/${imageMimeType.toLowerCase}",
         |            "data": "${imageBase64.getOrElse("")}"
         |          }
         |        },
         |        {
         |          "type": "text",
         |          "text":  ${Json.fromString(systemPrompt)}
         |        }
         |      ]
         |    }
         |  ]
         |}""".stripMargin

    basicRequest
      .post(uri"https://api.anthropic.com/v1/messages")
      .header("x-api-key", apiKey)
      .header("anthropic-version", "2023-06-01")
      .header("content-type", "application/json")
      .body(requestBody)
      .response(asJson[ClaudeResponse])
      .send(backend)
      .map { response =>
        log.info(s"Send request ${response.request}, Body(${imageBase64.map(requestBody.replace(_, "{imageBase64}")).getOrElse(requestBody)}")
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response
      }
  }

  def sendTranscribeImagePrompt(backend: SttpBackend[Task, MonixStreams & WebSockets], apiKey: String, file: Path): Task[Response[Either[ResponseException[String, Error], ClaudeResponse]]] = {
    val fileFormat = file.getFileName.toString.split("\\.").last
    val imageBase64 = java.util.Base64.getEncoder.encodeToString(java.nio.file.Files.readAllBytes(file))
    val requestBody =
      s"""{
         |  "model": "claude-3-5-sonnet-20241022",
         |  "max_tokens": 1024,
         |  "messages": [
         |    {
         |      "role": "user",
         |      "content": [
         |        {
         |          "type": "image",
         |          "source": {
         |            "type": "base64",
         |            "media_type": "image/$fileFormat",
         |            "data": "$imageBase64"
         |          }
         |        },
         |        {
         |          "type": "text",
         |          "text": "Transcribe the image <rules>- NEVER provide any explanation - ALWAYS return only transcribed text</rules>"
         |        }
         |      ]
         |    }
         |  ]
         |}""".stripMargin

    basicRequest
      .post(uri"https://api.anthropic.com/v1/messages")
      .header("x-api-key", apiKey)
      .header("anthropic-version", "2023-06-01")
      .header("content-type", "application/json")
      .body(requestBody)
      .response(asJson[ClaudeResponse])
      .send(backend)
      .map { response =>
        log.info(s"Send request ${response.request}, Body(${requestBody.replace(imageBase64, "{imageBase64}")})")
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response
      }
  }

  def sendAudioTranscription(backend: SttpBackend[Task, MonixStreams & WebSockets], apiKey: String, filePath: String): Task[Response[Either[ResponseException[String, Error], Json]]] = {
    basicRequest
      .post(uri"https://api.groq.com/openai/v1/audio/transcriptions")
      .header("Authorization", s"bearer $apiKey")
      .multipartBody(
        multipartFile("file", new java.io.File(filePath)),
        multipart("model", "whisper-large-v3"),
        multipart("temperature", "0"),
        multipart("response_format", "json"),
        multipart("language", "en")
      )
      .response(asJson[Json])
      .send(backend)
      .map { response =>
        log.info(s"Send request ${response.request}, File($filePath)")
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response
      }
  }

  /**
   * Transcribe png file using anthropic api
   * https://ollama.com/library/llama3.2-vision
   */
}
