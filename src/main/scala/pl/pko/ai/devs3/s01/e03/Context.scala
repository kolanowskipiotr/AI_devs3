package pl.pko.ai.devs3.s01.e03

case class Context(
  llmApiKey: String,
  hqApikey: String,
  calibrationData: Option[CalibrationData],
)

object Context {
  
  def empty(llmApiKey: String, hqApikey: String): Context = 
    Context(llmApiKey, hqApikey, None)
}
