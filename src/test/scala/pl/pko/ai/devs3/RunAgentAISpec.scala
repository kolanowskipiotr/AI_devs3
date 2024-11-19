package pl.pko.ai.devs3

import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.{Logger, LoggerFactory}
import pl.pko.ai.devs3.agent.AgentAI
import pl.pko.ai.devs3.s01.e05.AnonimisationAgentAI
import pl.pko.ai.devs3.s02.e01.InterogationAgentAI
import pl.pko.ai.devs3.s02.e04.FilteringAndSortingAgentAI
import pl.pko.ai.devs3.s02.e05.ArticleAnalizerAgentAI
import pl.pko.ai.devs3.s03.e01.KeyWordsExtractionAgentAI
import pl.pko.ai.devs3.s03.e02.VectorStoreAgentAI
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{Identity, RequestT, UriContext, basicRequest}
import sttp.model.StatusCode
import sttp.tapir.server.stub.TapirStubInterpreter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class RunAgentAISpec extends AnyWordSpec with Matchers with EitherValues with ApiKeys:

  private val log: Logger = LoggerFactory.getLogger(getClass)

  "Endpoint" should {

    "run VectorStoreAgentAI" in {
      // given
      val agentAi = VectorStoreAgentAI("ScalaTest")
      val request = basicRequest
        .post(uri"http://test.com/sync/agents/s03/e02/run")
        .headers(Map(
          "hq-api-key" -> hqApiKey,
          "claude-ai-api-key" -> claudeAiKey,
          "groq-ai-api-key" -> groqApiKey,
          "qdrant-ai-api-url" -> qdrantApiUrl,
          "qdrant-ai-api-key" -> qdrantApiKey,
          "jina-ai-api-key" -> jinaApiKey,
        ))

      runAgent(agentAi, request)
    }
    
    "run KeyWordsExtractionAgentAI" in {
      // given
      val agentAi = KeyWordsExtractionAgentAI("ScalaTest")
      val request = basicRequest
        .post(uri"http://test.com/sync/agents/s03/e01/run")
        .headers(Map(
          "claude-ai-api-key" -> claudeAiKey,
          "groq-ai-api-key" -> groqApiKey,
          "hq-api-key" -> hqApiKey
        ))

      runAgent(agentAi, request)
    }

    "run ArticleAnalizerAgentAI" in {
      // given
      val agentAi = ArticleAnalizerAgentAI("ScalaTest")
      val request = basicRequest
        .post(uri"http://test.com/sync/agents/s02/e05/run")
        .headers(Map(
          "claude-ai-api-key" -> claudeAiKey,
          "groq-ai-api-key" -> groqApiKey,
          "hq-api-key" -> hqApiKey
        ))

      runAgent(agentAi, request)
    }
    "run FilteringAndSortingAgentAI" in {
      // given
      val agentAi = FilteringAndSortingAgentAI("ScalaTest")
      val request = basicRequest
        .post(uri"http://test.com/sync/agents/s02/e04/run")
        .headers(Map(
          "claude-ai-api-key" -> claudeAiKey,
          "groq-ai-api-key" -> groqApiKey,
          "hq-api-key" -> hqApiKey
        ))

      runAgent(agentAi, request)
    }

    "run AnonimisationAgentAI" in {
      // given
      val agentAi = AnonimisationAgentAI("ScalaTest")
      val request = basicRequest
        .post(uri"http://test.com/sync/agents/s01/e04/run")
        .headers(Map("llm-ai-api-key" -> claudeAiKey))
        .headers(Map("hq-api-key" -> hqApiKey))

      runAgent(agentAi, request)
    }

    "run InterogationAgentAI" in {
      // given
      val agentAi = InterogationAgentAI("ScalaTest")
      val request = basicRequest
        .post(uri"http://test.com/sync/agents/s02/e01/run")
        .headers(Map("llm-ai-api-key" -> claudeAiKey))
        .headers(Map("hq-api-key" -> hqApiKey))

      runAgent(agentAi, request)
    }
  }

  private def runAgent(agentAi: AgentAI, request: RequestT[Identity, Either[String, String], Any]): Unit = {
    val backendStub = TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
      .whenServerEndpointsRunLogic(agentAi.endpoints)
      .backend()
    val serviceName = "ScalaTest"
    // when
    val response = request
      .send(backendStub)

    // then
    response.map(res => {
      res.code shouldBe StatusCode.Ok
      log.info(
        s"${res.body.value}"
          .replace("\\n", "\n")
          .replace("\\\"", "\"")
      )
    }).unwrap
  }

  extension [T](t: Future[T]) def unwrap: T = Await.result(t, Duration.Inf)
