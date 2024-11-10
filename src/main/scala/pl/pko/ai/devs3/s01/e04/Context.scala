package pl.pko.ai.devs3.s01.e04

case class Context(
  llmApiKey: String, 
  hqApikey: String,
  personalData: Option[String],
  anonymizedPersonalData: Option[String]
)

object Context {
  def empty(llmApiKey: String, hqApikey: String): Context = Context(
    llmApiKey = llmApiKey,
    hqApikey = hqApikey,
    personalData = None,
    anonymizedPersonalData = None,
  )
}
