package pl.pko.ai.devs3.s01.e03

import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.slf4j.{Logger, LoggerFactory}
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{UriContext, basicRequest}
import sttp.model.StatusCode
import sttp.tapir.server.stub.TapirStubInterpreter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class RobotsCalibrationSystemFixesAgentAISpec extends AnyFlatSpec with Matchers with EitherValues:

  private val log: Logger = LoggerFactory.getLogger(getClass)

  private val identityCheckAgent = RobotsCalibrationSystemFixesAgentAI("ScalaTest")
  private val backendStub = TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
    .whenServerEndpointsRunLogic(identityCheckAgent.endpoints)
    .backend()

  it should "run agent" in {
    // given
    val serviceName = "ScalaTest"

    // when
    val response = basicRequest
      .post(uri"http://test.com/sync/agents/s01/e03/fix-calibration")
      .headers(Map("llm-ai-api-key" -> "***"))
      .headers(Map("hq-api-key" -> "***"))
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