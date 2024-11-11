package pl.pko.ai.devs3.s02.e01

case class Context(
  llmApiKey: String, 
  hqApikey: String,
  interigationAnalizis: Option[String],
  streetName: Option[String],
)

object Context {
  def empty(llmApiKey: String, hqApikey: String): Context = Context(
    llmApiKey = llmApiKey,
    hqApikey = hqApikey,
    interigationAnalizis = None,
    streetName = None,
  )
}
