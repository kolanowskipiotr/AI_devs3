package pl.pko.ai.devs3.db.qdrant

import io.circe.{Error, Json}
import monix.eval.Task
import org.slf4j.{Logger, LoggerFactory}
import pl.pko.ai.devs3.db.qdrant.model.{QdrantDocument, QdrantResponse, QdrantSearchRequest, QdrantStoreVectorRequest, SearchResponse}
import pl.pko.ai.devs3.llm.jina.ai.JinaClient.getClass
import sttp.capabilities.WebSockets
import sttp.capabilities.monix.MonixStreams
import sttp.client3.{Response, ResponseException, SttpBackend, UriContext, basicRequest}
import sttp.client3.circe.{asJson, circeBodySerializer}

object QdrantClient {

  private val log: Logger = LoggerFactory.getLogger(getClass)

  private val collectionName = "vector_store_example"

  /**
   * curl --request PUT \
   * --url API_URL/collections/vector_store_example/points \
   * --header 'Content-Type: application/json' \
   * --header 'api-key: ***' \
   * --data '{
   * "points": [
   * {
   * "id": 1,
   * "vector": [0.05, 0.61, 0.76, 0.74],
   * "payload": {
   * "fileName": "Mars"
   * }
   * }
   * ]
   * }'
   */
  def putDocuments(backend: SttpBackend[Task, MonixStreams & WebSockets], qudratApiUrl: String, qdrantApiKey: String, documents: List[QdrantDocument]): Task[Response[Either[ResponseException[String, Error], QdrantResponse]]] = {
    val request = QdrantStoreVectorRequest(documents)

    basicRequest
      .body(request)
      .put(uri"$qudratApiUrl/collections/$collectionName/points")
      .header("Content-Type", "application/json")
      .header("api-key", qdrantApiKey)
      .response(asJson[QdrantResponse])
      .send(backend)
      .map { response =>
        log.info(s"Send request ${response.request}, Body(${request.copy(points = request.points.map(_.copy(vector = List.empty)))})")
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response
      }
  }

  /**
   * Example request:
   * curl --request POST \
   * --url API_URL/collections/vector_store_example/points/search \
   * --header 'Content-Type: application/json' \
   * --header 'api-key: SECRET' \
   * --data '{
   * "vector":  [
   * 0.07999753
   * ],
   * "limit": 1,
   * "with_payload": true
   * }'
   *
   * Example response:
   * {
   * "result": [
   * {
   * "id": "bde78227-e048-40a0-acf4-3e8f2f366694",
   * "version": 1,
   * "score": 0.5685324,
   * "payload": {
   * "fileName": "2024_02_21.txt"
   * }
   * }
   * ],
   * "status": "ok",
   * "time": 0.000667288
   * }
   */

  def searchByVector(backend: SttpBackend[Task, MonixStreams & WebSockets], qudratApiUrl: String, qdrantApiKey: String, vector: List[Double]): Task[Response[Either[ResponseException[String, Error], SearchResponse]]] = {
    val request = QdrantSearchRequest(
      vector = vector,
      limit = 1,
      with_payload = true,
    )

    basicRequest
      .body(request)
      .post(uri"$qudratApiUrl/collections/$collectionName/points/search")
      .header("Content-Type", "application/json")
      .header("api-key", qdrantApiKey)
      .response(asJson[SearchResponse])
      .send(backend)
      .map { response =>
        log.info(s"Send request ${response.request}, Body(${request.copy(vector = List.empty)})")
        log.info(s"Got response code: ${response.code} Body: ${response.body}")
        response
      }
  }
}
