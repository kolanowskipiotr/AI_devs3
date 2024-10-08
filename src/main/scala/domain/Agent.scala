package pl.pko.ai.devs3
package domain

import sttp.capabilities.WebSockets
import sttp.shared.Identity
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.netty.sync.OxStreams

trait Agent {
  val lesson: String
  def endpoints: List[ServerEndpoint[OxStreams & WebSockets, Identity]]
}
