package pl.pko.ai.devs3
package domain.hq.api.agent

import domain.{Agent, HQResponse}

import sttp.tapir.*
import sttp.capabilities.WebSockets
import sttp.shared.Identity
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.generic.auto._
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.netty.sync.OxStreams

case class HQAPIAgent(lesson: String) extends Agent {
  override def endpoints: List[ServerEndpoint[OxStreams & WebSockets, Identity]] = {
    List(
      endpoint
        .post
        .in("agents" / "hq-api-agent" / "send-data-to-hq")
        .in(header[Option[String]]("hq-api-key"))
        .out(jsonBody[HQResponse])
        .handleSuccess(hqApikey => getDataAndSendToHQ(hqApikey))
    )
  }

  private def getDataAndSendToHQ(hqApiKey: Option[String]): HQResponse = {
    HQResponse(0, "Super. Wszystko OK!")
  }
}
