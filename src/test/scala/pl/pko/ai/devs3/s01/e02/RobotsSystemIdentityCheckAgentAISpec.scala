package pl.pko.ai.devs3.s01.e02

import io.circe.Json
import io.circe.parser.parse
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.matchers.should.Matchers
import org.slf4j.{Logger, LoggerFactory}
import pl.pko.ai.devs3.helth.check.HelthCheckAgentAI
import sttp.client3.{UriContext, basicRequest}
import sttp.client3.testing.SttpBackendStub
import sttp.model.StatusCode
import sttp.tapir.server.stub.TapirStubInterpreter

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class RobotsSystemIdentityCheckAgentAISpec extends AnyFlatSpec with Matchers with EitherValues:

  private val log: Logger = LoggerFactory.getLogger(getClass)

  private val identityCheckAgent = RobotsSystemIdentityCheckAgentAI("ScalaTest")
  private val backendStub = TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
    .whenServerEndpointsRunLogic(identityCheckAgent.endpoints)
    .backend()

  it should "return health check" in {
    // given
    val serviceName = "ScalaTest"

    // when
    val response = basicRequest
      .post(uri"http://test.com/sync/agents/s01/e02/identity-check")
      .headers(Map("open-ai-api-key" -> "123"))
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
