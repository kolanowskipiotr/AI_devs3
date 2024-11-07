package pl.pko.ai.devs3.s01.e01

import pl.pko.ai.devs3.llm.ollama.model.OllamaResponse

case class GoogleSerchResult(
  ollamaResponse: OllamaResponse,
  googleResponse: String
)
