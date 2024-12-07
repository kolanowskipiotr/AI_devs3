package pl.pko.ai.devs3.agent

import io.circe.Error
import org.slf4j.{Logger, LoggerFactory}
import pl.pko.ai.devs3.hq.model.HQResponse
import sttp.tapir.server.ServerEndpoint
import io.circe.parser.decode
import monix.eval.Task
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.substringBetween
import sttp.capabilities.WebSockets
import sttp.capabilities.monix.MonixStreams
import sttp.client3.{ResponseException, SttpBackend}

import scala.concurrent.Future

trait AgentAI {

  type RequestError = ResponseException[String, Error]
  type Backend = SttpBackend[Task, MonixStreams & WebSockets]

  protected val log: Logger = LoggerFactory.getLogger(getClass)

  val lesson: String

  def endpoints: List[ServerEndpoint[Any, Future]]
  
  protected def tryParseHQResponse(body: String): HQResponse =
    decode[HQResponse](body) match {
      case Right(hqResponse) => hqResponse
      case Left(err) =>
        log.error(s"Parsing of error body fail ${err.getMessage}", err)
        HQResponse.systemError
    }

  protected def extractValuesInTag(str: String, tag: String, separator: String = "\n"): List[String] = {
    substringBetween(str, s"<$tag>", s"</$tag>")
      .split(separator)
      .map(_.replace(separator, ""))
      .filter(StringUtils.isNotBlank)
      .map(_.trim)
      .toList
  }

  protected def extractValuesTag(str: String, tag: String): Option[String] = {
    Option(substringBetween(str, s"<$tag>", s"</$tag>"))
      .filter(StringUtils.isNotBlank)
      .map(_.trim)
  }
  
  private def mkString[K: Ordering, V](map: Map[K, List[V]]): String = {
    map.toList
      .sortBy(_._1)
      .map { case (key, values) =>
        s"$key:\n\t${values.mkString("\n\t")}"
      }
      .mkString("\n")
  }
  
  protected def splitNonEmpty(content: String, separator: String = "\n"): List[String] =
    content
      .split(separator)
      .map(_.replace(separator, ""))
      .filter(StringUtils.isNotBlank)
      .map(_.trim)
      .toList
}
