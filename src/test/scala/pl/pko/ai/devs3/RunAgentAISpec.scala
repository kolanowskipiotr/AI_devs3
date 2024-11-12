package pl.pko.ai.devs3

import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.{Logger, LoggerFactory}
import pl.pko.ai.devs3.agent.AgentAI
import pl.pko.ai.devs3.s01.e05.AnonimisationAgentAI
import pl.pko.ai.devs3.s02.e01.InterogationAgentAI
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