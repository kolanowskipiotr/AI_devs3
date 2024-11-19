package pl.pko.ai.devs3.llm.jina.ai

import io.circe.{Error, Json}
import monix.eval.Task
import org.slf4j.{Logger, LoggerFactory}
import pl.pko.ai.devs3.llm.jina.ai.model.JinaResponse.decoder
import pl.pko.ai.devs3.llm.jina.ai.model.{JinaRequest, JinaResponse}
import pl.pko.ai.devs3.llm.ollama.service.OllamaService.getClass
import sttp.capabilities.WebSockets
import sttp.capabilities.monix.MonixStreams
import sttp.client3.circe.{asJson, circeBodySerializer}
import sttp.client3.{Response, ResponseException, SttpBackend, UriContext, basicRequest}

object JinaClient {

  private val log: Logger = LoggerFactory.getLogger(getClass)

  def calculateEmbedings(backend: SttpBackend[Task, MonixStreams & WebSockets], jinaApiKey: String, data: List[String]): Task[Response[Either[ResponseException[String, Error], JinaResponse]]] = {
    val request = JinaRequest(
      model = "jina-embeddings-v3",
      task = "text-matching",
      dimensions = 1024,
      late_chunking = false,
      embedding_type = "float",
      input = data.map(Json.fromString).map(_.noSpaces)
    )
    basicRequest
      .body(request)
      .post(uri"https://api.jina.ai/v1/embeddings")
      .header("Authorization", s"Bearer $jinaApiKey")
      .header("Content-Type", "application/json")
      .response(asJson[JinaResponse])
      .send(backend)
      .map { response =>
        log.info(s"Send request ${response.request}, Body(${request.copy(input = List("<content_removes>"))})")
        log.info(s"Got response code: ${response.code} Body: ${response.body.map(_.copy(data = List.empty))}")
        response
      }
  }
}
