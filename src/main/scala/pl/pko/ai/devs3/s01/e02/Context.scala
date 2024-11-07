package pl.pko.ai.devs3.s01.e02

import pl.pko.ai.devs3.llm.model.LLMResponse
import pl.pko.ai.devs3.llm.ollama.model.OllamaResponse

case class Context(
  llmApiKey: String,
  question: Option[IdentityCheckMessage],
  answare: Option[LLMResponse],
  checkResult: Option[IdentityCheckMessage]
)

object Context {

  def empty(openApiKey: String): Context = Context(openApiKey, None, None, None)
}
