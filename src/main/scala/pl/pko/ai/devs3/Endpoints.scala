package pl.pko.ai.devs3

import pl.pko.ai.devs3.agent.AgentAI
import pl.pko.ai.devs3.helth.check.HelthCheckAgentAI
import pl.pko.ai.devs3.hq.api.agent.{HQAPIAgentAI, HQAPIAsyncAgentAI}
import pl.pko.ai.devs3.s01.e01.RobotsSystemLoginAgentAI
import pl.pko.ai.devs3.s01.e02.RobotsSystemIdentityCheckAgentAI
import pl.pko.ai.devs3.s01.e03.RobotsCalibrationSystemFixesAgentAI
import pl.pko.ai.devs3.s01.e05.AnonimisationAgentAI
import pl.pko.ai.devs3.s02.e01.InterogationAgentAI
import pl.pko.ai.devs3.s02.e04.FilteringAndSortingAgentAI
import pl.pko.ai.devs3.s02.e05.ArticleAnalizerAgentAI
import sttp.tapir.*
import sttp.shared.Identity
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.Future

object Endpoints:

  val aiAgents: List[AgentAI] = List(
    HelthCheckAgentAI("System"),
    HQAPIAgentAI("Prework"),
    HQAPIAsyncAgentAI("Prework"),
    RobotsSystemLoginAgentAI("S01EO1"),
    RobotsSystemIdentityCheckAgentAI("S01EO2"),
    RobotsCalibrationSystemFixesAgentAI("S01EO3"),
    AnonimisationAgentAI("S01EO4"),
    InterogationAgentAI("S02EO1"),
    FilteringAndSortingAgentAI("S02EO4"),
    ArticleAnalizerAgentAI("S02EO5"),
  )
  
  val apiEndpoints: List[ServerEndpoint[Any, Future]] =
    aiAgents.flatMap(_.endpoints)

  val docEndpoints: List[ServerEndpoint[Any, Future]] = SwaggerInterpreter()
    .fromServerEndpoints[Future](apiEndpoints, "ai_devs3", "1.0.0")

  val prometheusMetrics: PrometheusMetrics[Future] = PrometheusMetrics.default[Future]()
  val metricsEndpoint: ServerEndpoint[Any, Future] = prometheusMetrics.metricsEndpoint

  val all: List[ServerEndpoint[Any, Future]] = apiEndpoints ++ docEndpoints ++ List(metricsEndpoint)
