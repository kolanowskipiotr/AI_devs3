package pl.pko.ai.devs3.agent

import org.slf4j.{Logger, LoggerFactory}
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.Future

trait AgentAI {
  
  protected val log: Logger = LoggerFactory.getLogger(getClass)
  
  val lesson: String
  def endpoints: List[ServerEndpoint[Any, Future]]
}
