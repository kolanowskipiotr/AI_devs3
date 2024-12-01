package pl.pko.ai.devs3.s04.e04

import io.circe.Error
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils.substringBetween
import pl.pko.ai.devs3.ApiKeys
import pl.pko.ai.devs3.agent.AgentAI
import pl.pko.ai.devs3.llm.claude.ai.model.ClaudeResponse
import pl.pko.ai.devs3.llm.claude.ai.service.ClaudeService
import sttp.client3
import sttp.client3.*
import sttp.client3.asynchttpclient.monix.AsyncHttpClientMonixBackend
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.server.ServerEndpoint

import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.util.Right

/**
 * {{FLG:DARKCAVE}}
 *
 * >> ssh -R 50125:localhost:10296 {{NAZWA_AGENTA}}@azyl.ag3nts.org -p 5022
 * 10296 - localny port
 * 50125 - port publiczny pod którym będzie dostępna aplikacja
 * NAZWA_AGENTA - https://centrala.ag3nts.org/
 *
 * Test curl:
 * curl -X 'POST' \
 *  'https://azyl-50125.ag3nts.org/sync/agents/s04/e04/run' \
 *  -H 'accept: application/json' \
 *  -H 'Content-Type: application/json' \
 *  -d '{
 *  "instruction": "prawo"
 * }'
 *
 * Response:
 * {"description":"trawa"}%
 */
case class FlightOfTheNavigatorAgentAI(lesson: String) extends AgentAI with ApiKeys {

  override def endpoints: List[ServerEndpoint[Any, Future]] =
    List(
      endpoint
        .post
        .in("sync" / "agents" / "s04" / "e04" / "run")
        .in(jsonBody[HQInstructionsRequest])
        .out(jsonBody[HQTerrainDescriptionResponse])
        .serverLogicSuccess((instructions) => Future {
          getDataAndSendToHQ(Context(
            hqApiKey,
            claudeAiKey,
            groqApiKey,
            qdrantApiUrl,
            qdrantApiKey,
            jinaApiKey,
            neo4jUri,
            neo4jUser,
            neo4jPassword,
            Some(instructions),
          ))
        })
    )

  private def getDataAndSendToHQ(context: Context): HQTerrainDescriptionResponse = {
    Right(context)
      .flatMap(navigate)
      //.flatMap(_.cacheContext)
      .flatMap(prepareHQTerrainDescriptionResponse)
    match {
      case Left(value) => value match
        case HttpError(body, _) =>
          HQTerrainDescriptionResponse(body.toString)
        case err =>
          HQTerrainDescriptionResponse(err.toString)
      case Right(value) =>
        value
    }
  }

  protected def prepareHQTerrainDescriptionResponse(context: Context): Either[String, HQTerrainDescriptionResponse] =
    context.claudeResponse match {
      case Some(response) =>
        val terrain = substringBetween(response.textResponse, "<answer>", "</answer>").replace("\n", "").trim
        Right(HQTerrainDescriptionResponse(terrain))
      case None => Left("No response from Claude AI")
    }

  private def navigate(context: Context): Either[RequestError, Context] = {
    val prompt =
      s"""
         |Jesteś systemem nawigacyjnym drona, który ma za zadanie określić typ terenu w miejscu lądowania.
         |
         |<prompt_objective>
         |Określ typ terenu w miejscu końcowym lotu drona, bazując na planie lotu zawartym w sekcji <instruction> oraz mapie terenu 4x4.
         |</prompt_objective>
         |
         |<map_description>
         |Mapa terenu 4x4 zawiera następujące elementy:
         |Rząd 1: start drona, trawa, pojedyńcze drzewo, dom
         |Rząd 2: trawa, młyn, trawa, trawa
         |Rząd 3: trawa, trawa, skały, dwa drzewa
         |Rząd 4: góry, góry, samochód, jaskinia
         |</map_description>
         |
         |<prompt_rules>
         |- ZAWSZE rozpocznij od pozycji start drona (lewy górny róg)
         |- DOKŁADNIE wykonuj instrukcje lotu zawarte w sekcji <instruction>
         |- Poruszaj się tylko w obrębie siatki 4x4
         |- Odpowiedź MUSI być w języku polskim
         |- Odpowiedź MUSI zawierać 1-2 słowa opisujące teren w końcowej pozycji
         |- Odpowiedź MUSI być umieszczona w znacznikach <answer>
         |- NIGDY nie dodawaj dodatkowych wyjaśnień ani komentarzy
         |</prompt_rules>
         |
         |<movement_rules>
         |Akceptowane formaty instrukcji ruchu:
         |1. Polskie słowa: północ, południe, wschód, zachód
         |2. Polskie kierunki: góra, dół, prawo, lewo
         |3. Angielskie słowa: north, south, east, west
         |4. Pojedyncze litery: N, S, E, W
         |
         |Każdy z tych formatów oznacza:
         |- północ/góra/north/N - ruch o jedno pole w górę
         |- południe/dół/south/S - ruch o jedno pole w dół
         |- wschód/prawo/east/E - ruch o jedno pole w prawo
         |- zachód/lewo/west/W - ruch o jedno pole w lewo
         |</movement_rules>
         |
         |<prompt_examples>
         |USER:
         |<instruction>
         |wschód, południe, południe
         |</instruction>
         |
         |AI: <answer>trawa</answer>
         |
         |USER:
         |<instruction>
         |prawo, prawo, dół, dół, prawo
         |</instruction>
         |
         |AI: <answer>dwa drzewa</answer>
         |
         |USER:
         |<instruction>
         |south, south, S
         |</instruction>
         |
         |AI: <answer>góry</answer>
         |
         |USER:
         |<instruction>
         |E,E,E,dół,south,S
         |</instruction>
         |
         |AI: <answer>jaskinia</answer>
         |
         |USER:
         |<instruction>
         |prawo, east, E, południe
         |</instruction>
         |
         |AI: <answer>młyn</answer>
         |</prompt_examples>
         |
         |Jesteś gotowy do nawigacji drona. Określ typ terenu w miejscu końcowym lotu drona, bazując na planie lotu:
         |<instruction>
         |${context.HQInstructionsRequest.map(_.instruction).getOrElse("")}
         |</instruction>
         |""".stripMargin
    AsyncHttpClientMonixBackend.resource()
      .use { backend => ClaudeService.sendPrompt(backend, context.claudeApiKey, prompt) }
      .runSyncUnsafe()
      .body match {
      case Left(err) => Left(err)
      case Right(value) => Right(context.copy(claudeResponse = Some(value)))
    }
  }
}
