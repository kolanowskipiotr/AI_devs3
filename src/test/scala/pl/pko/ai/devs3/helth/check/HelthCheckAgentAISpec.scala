package pl.pko.ai.devs3.helth.check

import io.circe.Json
import io.circe.parser.parse
import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{UriContext, basicRequest}
import sttp.tapir.server.stub.TapirStubInterpreter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


class HelthCheckAgentAISpec extends AnyFlatSpec with Matchers with EitherValues:

  private val helthCheckAgent = HelthCheckAgentAI("ScalaTest")
  private val backendStub = TapirStubInterpreter(SttpBackendStub.asynchronousFuture)
    .whenServerEndpointsRunLogic(helthCheckAgent.endpoints)
    .backend()

  it should "return health check" in {
    // given
    val serviceName = "ScalaTest"

    // when
    val response = basicRequest
      .get(uri"http://test.com/helth-chack?service-name=$serviceName")
      .send(backendStub)

    // then
    response.map(res => {
      val jsonResponse = parse(res.body.value).getOrElse(Json.Null)
      jsonResponse.hcursor.get[String]("serviceName").value shouldBe "ScalaTest"
    }).unwrap
  }

  extension [T](t: Future[T]) def unwrap: T = Await.result(t, Duration.Inf)
