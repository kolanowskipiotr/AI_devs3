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
import sttp.client3.circe.circeBodySerializer
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{Identity, RequestT, UriContext, basicRequest}
import sttp.model.StatusCode
import sttp.tapir.server.stub.TapirStubInterpreter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class
SideFlagSpec extends AnyWordSpec with Matchers with EitherValues with ApiKeys:

  private val log: Logger = LoggerFactory.getLogger(getClass)

  "Side flags" should {

    "S05E02 side flag" in {

      val lats = List(
        "70.736377",
        "76.185710",
        "71.133730",
        "58.238647",
        "87.854235",
        "72.744574",
        "69.161629",
        "82.804412",
        "69.242524",
        "83.395576",
        "87.973250",
        "65.659462",
        "76.391377",
        "76.428167",
        "89.472081",
      )

      val lons = List(
        "123.208409",
        "123.643150",
        "125.194607",
        "125.501924",
      )

      for {
        lat <- lats
        lon <- lons
      } yield println(s"https://maps.google.com/?q=$lat,$lon")
    }
  }
