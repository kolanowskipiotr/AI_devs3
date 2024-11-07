package pl.pko.ai.devs3.s01.e02

import pl.pko.ai.devs3.ollama.model.OllamaResponse

case class Context(
  openApiKey: String,
  question: Option[IdentityCheckMessage],
  answare: Option[OllamaResponse],
  checkResult: Option[IdentityCheckMessage]
)

object Context {

  def empty(openApiKey: String): Context = Context(openApiKey, None, None, None)
}
