package pl.pko.ai.devs3.s01.e02

import pl.pko.ai.devs3.ollama.model.OllamaResponse

case class Context(
  question: Option[IdentityCheckMessage],
  answare: Option[OllamaResponse],
  checkResult: Option[IdentityCheckMessage]
)

object Context {

  def empty: Context = Context(None, None, None)
}
