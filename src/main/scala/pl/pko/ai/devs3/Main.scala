package pl.pko.ai.devs3

import org.slf4j.{Logger, LoggerFactory}
import sttp.tapir.server.interceptor.exception.{ExceptionContext, ExceptionHandler}
import sttp.tapir.server.netty.{NettyConfig, NettyFutureServer, NettyFutureServerBinding, NettyFutureServerOptions}
import sun.misc.{Signal, SignalHandler}

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS
import scala.concurrent.duration.{Duration, FiniteDuration}
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
    //Default: 8090
    val minPort = 8081 // Minimum port number above 8080
    val maxPort = 65535 // Maximum valid port number
    Random.nextInt((maxPort - minPort) + 1) + minPort
  }

  logAIAgentsEndpoints()

  val exceptionHandler: ExceptionHandler[Future] = ExceptionHandler.pure { case ctx: ExceptionContext =>
    log.error(s"Exception occurred while processing request: ${ctx.request}", ctx.e)
    None
  }

  val serverOptions = NettyFutureServerOptions
    .customiseInterceptors
    .metricsInterceptor(Endpoints.prometheusMetrics.metricsInterceptor())
    .exceptionHandler(exceptionHandler)
    .options

  val port = sys.env.get("HTTP_PORT").flatMap(_.toIntOption).getOrElse(getRandomPort)

  val program =
    for
      binding <- NettyFutureServer(serverOptions)
        .config(NettyConfig.default.copy(requestTimeout = Some(FiniteDuration(60, SECONDS))))
        .port(port)
        .addEndpoints(Endpoints.all)
        .start()
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
