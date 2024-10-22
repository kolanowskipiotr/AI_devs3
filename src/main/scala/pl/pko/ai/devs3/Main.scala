package pl.pko.ai.devs3

import org.slf4j.{Logger, LoggerFactory}
import sttp.tapir.server.netty.{NettyFutureServer, NettyFutureServerBinding, NettyFutureServerOptions}
import sun.misc.{Signal, SignalHandler}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.io.StdIn
import ExecutionContext.Implicits.global
import scala.util.Random

@main def mainServer(): Unit = {
  val log: Logger = LoggerFactory.getLogger(getClass.getName)

  def logAIAgentsEndpoints(): Unit =
    Endpoints.aiAgents.foreach(agent => {
      log.info(s"${agent.lesson} Agent: ${agent.getClass.getSimpleName}. Registered endpoints:")
      agent.endpoints.foreach(endpoint => {
        log.info(s"\t ${endpoint.show}")
      })
    })

  def logSawaggerUrl(binding: NettyFutureServerBinding): Unit =
    log.info(
      """
        |      _,.,.__,--.__,-----.
        |   ,""   '))              `.
        | ,'   e                    ))
        |(  .='__,                  ,
        | `~`     `-\  /._____,/   /
        |          | | )    (  (   ;
        |          | | |    / / / / 
        |vvVVvvVvVVVvvVVVvvVVvVvvvVvvVv
        |https://tapir.softwaremill.com
        |""".stripMargin +
        "\n" +
        s"Go to http://localhost:${binding.port}/docs to open SwaggerUI.\n" +
        s"Server started on port: ${binding.port}.\n" +
        s"Press ENTER key to exit."
    )

  def getRandomPort: Int = {
    val minPort = 8081 // Minimum port number above 8080
    val maxPort = 65535 // Maximum valid port number
    Random.nextInt((maxPort - minPort) + 1) + minPort
  }

  logAIAgentsEndpoints()

  val serverOptions = NettyFutureServerOptions.customiseInterceptors
    .metricsInterceptor(Endpoints.prometheusMetrics.metricsInterceptor())
    .options

  val port = sys.env.get("HTTP_PORT").flatMap(_.toIntOption).getOrElse(8090)

  val program =
    for
      binding <- NettyFutureServer(serverOptions).port(port).addEndpoints(Endpoints.all).start()
      _ <- Future:
        Signal.handle(new Signal("INT"), (signal: Signal) => {
          log.info("Received SIGINT, stopping server...")
          binding.stop()
          System.exit(0)
        })
      _ <- Future:
        logSawaggerUrl(binding)
        StdIn.readLine()
      stop <-
        log.info("Stopping server...")
        binding.stop()
    yield stop

  Await.result(program, Duration.Inf)
}
