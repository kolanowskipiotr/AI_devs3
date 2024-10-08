package pl.pko.ai.devs3

import domain.helth.check.HelthCheckAgent
import domain.hq.api.agent.HQAPIAgent

import com.typesafe.scalalogging.Logger
import sttp.tapir.*
import sttp.tapir.server.netty.sync.NettySyncServer

import scala.util.Random

@main
def main(): Unit =
  val logger = Logger(getClass.getName)

  val applicationPort = getRandomPort
  var server = NettySyncServer().port(applicationPort)

  val agents = List(
    HelthCheckAgent("System"),
    HQAPIAgent("Lesson 0"),
  )
  agents.foreach(agent => {
    logger.info(s"${agent.lesson} Agent: ${agent.getClass.getSimpleName}")
    logger.info("Registered endpoints:")
    agent.endpoints.foreach(endpoint => {
      logger.info(endpoint.show)
      server = server.addEndpoint(endpoint)
    })
  })

  sys.addShutdownHook {
    logger.info(s"Shutting down...")
  }

  logger.info(s"Applition starting on port: $applicationPort")
  server.startAndWait()

def getRandomPort: Int = {
  val minPort = 8081 // Minimum port number above 8080
  val maxPort = 65535 // Maximum valid port number
  Random.nextInt((maxPort - minPort) + 1) + minPort
}

