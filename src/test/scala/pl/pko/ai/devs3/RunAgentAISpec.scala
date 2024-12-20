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
import pl.pko.ai.devs3.s03.e03.SQLDataBaseAgentAI
import pl.pko.ai.devs3.s03.e05.ConnectionsGraphAgentAI
import pl.pko.ai.devs3.s04.e01.BarbaraDescriptionAgentAI
import pl.pko.ai.devs3.s04.e02.FineTuneAgentAI
import pl.pko.ai.devs3.s04.e03.UniversalSearcherAgentAI
import pl.pko.ai.devs3.s04.e04.{FlightOfTheNavigatorAgentAI, HQInstructionsRequest}
import pl.pko.ai.devs3.s05.e01.part1.{RecostructPhoneCallsTakeTwoAgentAI, RecostructPhoneCalsAgentAI}
import pl.pko.ai.devs3.s05.e01.part2.AnswerQuestionsAgentAI
import pl.pko.ai.devs3.s05.e02.PeopleLocalisationAgentAI
import pl.pko.ai.devs3.s05.e03.HackRafalaAPIAgentAI
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{BodySerializer, Identity, RequestT, UriContext, basicRequest}
import sttp.model.StatusCode
import sttp.tapir.server.stub.TapirStubInterpreter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import sttp.client3.circe.{asJson, circeBodySerializer}

class 
RunAgentAISpec extends AnyWordSpec with Matchers with EitherValues with ApiKeys:

  private val log: Logger = LoggerFactory.getLogger(getClass)

  "Endpoint" should {
    
    "run HackRafalaAPIAgentAI" in {
      // given
      val agentAi = HackRafalaAPIAgentAI("ScalaTest")
      val request = basicRequest
        .post(uri"http://test.com/sync/agents/s05/e03/hack-api")
        .headers(Map(
          "hq-api-key" -> hqApiKey,
          "claude-ai-api-key" -> claudeAiKey,
          "groq-ai-api-key" -> groqApiKey,
          "qdrant-ai-api-url" -> qdrantApiUrl,
          "qdrant-ai-api-key" -> qdrantApiKey,
          "jina-ai-api-key" -> jinaApiKey,
          "neo4j-uri" -> neo4jUri,
          "neo4j-user" -> neo4jUser,
          "neo4j-password" -> neo4jPassword,
        ))

      runAgent(agentAi, request)
    }
    
    "run PeopleLocalisationAgentAI" in  {
      // given
      val agentAi = PeopleLocalisationAgentAI("ScalaTest")
      val request = basicRequest
        .post(uri"http://test.com/sync/agents/s05/e02/locate-people")
        .headers(Map(
          "hq-api-key" -> hqApiKey,
          "claude-ai-api-key" -> claudeAiKey,
          "groq-ai-api-key" -> groqApiKey,
          "qdrant-ai-api-url" -> qdrantApiUrl,
          "qdrant-ai-api-key" -> qdrantApiKey,
          "jina-ai-api-key" -> jinaApiKey,
          "neo4j-uri" -> neo4jUri,
          "neo4j-user" -> neo4jUser,
          "neo4j-password" -> neo4jPassword,
        ))

      runAgent(agentAi, request)
    }

    "run AnswerQuestionsAgentAI" in {
      // given
      val agentAi = AnswerQuestionsAgentAI("ScalaTest")
      val request = basicRequest
        .post(uri"http://test.com/sync/agents/s05/e01/answer-questions")
        .headers(Map(
          "hq-api-key" -> hqApiKey,
          "claude-ai-api-key" -> claudeAiKey,
          "groq-ai-api-key" -> groqApiKey,
          "qdrant-ai-api-url" -> qdrantApiUrl,
          "qdrant-ai-api-key" -> qdrantApiKey,
          "jina-ai-api-key" -> jinaApiKey,
          "neo4j-uri" -> neo4jUri,
          "neo4j-user" -> neo4jUser,
          "neo4j-password" -> neo4jPassword,
        ))

      runAgent(agentAi, request)
    }

    "run RecostructPhoneCallsTakeTwoAgentAI" in {
      // given
      val agentAi = RecostructPhoneCallsTakeTwoAgentAI("ScalaTest")
      val request = basicRequest
        .post(uri"http://test.com/sync/agents/s05/e01/reconstruct-take-two")
        .headers(Map(
          "hq-api-key" -> hqApiKey,
          "claude-ai-api-key" -> claudeAiKey,
          "groq-ai-api-key" -> groqApiKey,
          "qdrant-ai-api-url" -> qdrantApiUrl,
          "qdrant-ai-api-key" -> qdrantApiKey,
          "jina-ai-api-key" -> jinaApiKey,
          "neo4j-uri" -> neo4jUri,
          "neo4j-user" -> neo4jUser,
          "neo4j-password" -> neo4jPassword,
        ))

      runAgent(agentAi, request)
    }
    
    "run RecostructPhoneCalsAgentAI" in {
      // given
      val agentAi = RecostructPhoneCalsAgentAI("ScalaTest")
      val request = basicRequest
        .post(uri"http://test.com/sync/agents/s05/e01/reconstruct")
        .headers(Map(
          "hq-api-key" -> hqApiKey,
          "claude-ai-api-key" -> claudeAiKey,
          "groq-ai-api-key" -> groqApiKey,
          "qdrant-ai-api-url" -> qdrantApiUrl,
          "qdrant-ai-api-key" -> qdrantApiKey,
          "jina-ai-api-key" -> jinaApiKey,
          "neo4j-uri" -> neo4jUri,
          "neo4j-user" -> neo4jUser,
          "neo4j-password" -> neo4jPassword,
        ))

      runAgent(agentAi, request)
    }

    "run FlightOfTheNavigatorAgentAI" in {
      // given

      val agentAi = FlightOfTheNavigatorAgentAI("ScalaTest")
      val request = basicRequest
        .post(uri"http://test.com/sync/agents/s04/e04/run")
        .body(HQInstructionsRequest("poleciałem jedno pole w prawo, a później na sam dół"))

      runAgent(agentAi, request)
    }
    
    "run UniversalSearcherAgentAI" in {
      // given
      val agentAi = UniversalSearcherAgentAI("ScalaTest")
      val request = basicRequest
        .post(uri"http://test.com/sync/agents/s04/e03/run")
        .headers(Map(
          "hq-api-key" -> hqApiKey,
          "claude-ai-api-key" -> claudeAiKey,
          "groq-ai-api-key" -> groqApiKey,
          "qdrant-ai-api-url" -> qdrantApiUrl,
          "qdrant-ai-api-key" -> qdrantApiKey,
          "jina-ai-api-key" -> jinaApiKey,
          "neo4j-uri" -> neo4jUri,
          "neo4j-user" -> neo4jUser,
          "neo4j-password" -> neo4jPassword,
        ))

      runAgent(agentAi, request)
    }

    "run FineTuneAgentAI" in {
      // given
      val agentAi = FineTuneAgentAI("ScalaTest")
      val request = basicRequest
        .headers(Map(
          "hq-api-key" -> hqApiKey
        ))
        .post(uri"http://test.com/sync/agents/s04/e02/run")

      runAgent(agentAi, request)
    }
    
    "run BarbaraDescriptionAgentAI" in {
      // given
      val agentAi = BarbaraDescriptionAgentAI("ScalaTest")
      val request = basicRequest
        .post(uri"http://test.com/sync/agents/s04/e01/run")
        .headers(Map(
          "hq-api-key" -> hqApiKey,
          "claude-ai-api-key" -> claudeAiKey,
          "groq-ai-api-key" -> groqApiKey,
          "qdrant-ai-api-url" -> qdrantApiUrl,
          "qdrant-ai-api-key" -> qdrantApiKey,
          "jina-ai-api-key" -> jinaApiKey,
          "neo4j-uri" -> neo4jUri,
          "neo4j-user" -> neo4jUser,
          "neo4j-password" -> neo4jPassword,
        ))

      runAgent(agentAi, request)
    }

    "run ConnectionsGraphAgentAI" in {
      // given
      val agentAi = ConnectionsGraphAgentAI("ScalaTest")
      val request = basicRequest
        .post(uri"http://test.com/sync/agents/s03/e05/run")
        .headers(Map(
          "hq-api-key" -> hqApiKey,
          "claude-ai-api-key" -> claudeAiKey,
          "groq-ai-api-key" -> groqApiKey,
          "qdrant-ai-api-url" -> qdrantApiUrl,
          "qdrant-ai-api-key" -> qdrantApiKey,
          "jina-ai-api-key" -> jinaApiKey,
          "neo4j-uri" -> neo4jUri,
          "neo4j-user" -> neo4jUser,
          "neo4j-password" -> neo4jPassword,
        ))

      runAgent(agentAi, request)
    }
    
    "run BarbaraZawadzkaAgentAI" in {
      // given
      val agentAi = ConnectionsGraphAgentAI("ScalaTest")
      val request = basicRequest
        .post(uri"http://test.com/sync/agents/s03/e04/run")
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

    "run SQLDataBaseAgentAI" in {
      // given
      val agentAi = SQLDataBaseAgentAI("ScalaTest")
      val request = basicRequest
        .post(uri"http://test.com/sync/agents/s03/e03/run")
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
