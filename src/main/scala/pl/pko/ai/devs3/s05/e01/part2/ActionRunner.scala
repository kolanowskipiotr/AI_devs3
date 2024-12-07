package pl.pko.ai.devs3.s05.e01.part2

import monix.execution.Scheduler.Implicits.global
import pl.pko.ai.devs3.llm.claude.ai.service.ClaudeService
import sttp.client3.asynchttpclient.monix.AsyncHttpClientMonixBackend
import sttp.client3.{Identity, RequestT, UriContext, basicRequest}
import sttp.model.Method

class ActionRunner {

  private val log = org.slf4j.LoggerFactory.getLogger(getClass)

  val finalAnswerToolName = "FINAL_ANSWER_TOOL"

  val tools: List[Tool] = List(
    Tool(
      toolName = "FIND_FACTS_TOOL",
      description = "Tool retrieves facts from memory based on provided keywords",
      parameters = "Keywords to identify facts separated by semicolon",
      usageExamples = List(
        ActionCommand("FIND_FACTS_TOOL", "Wrocław; Zabobrze")
      ),
    ),
    Tool(
      toolName = "ASK_LLM_MODEL_TOOL",
      description = "Tool to ask LLM model and retrieve its answer",
      parameters = "Any general knowledge question, process or categorize data using LLM model. This LLM dos not have context and access to external sources. Always provide required context n parameters",
      usageExamples = List(
        ActionCommand("ASK_LLM_MODEL_TOOL", "What is the capital of Poland?"),
        ActionCommand("ASK_LLM_MODEL_TOOL", "Process {{put your data here}} and extract names from it"),
        ActionCommand("ASK_LLM_MODEL_TOOL", "Categorize {{put your data here}} into categories: ({{category1}}, {{category2}}, {{categoryN}})"),
      ),
    ),
    Tool(
      toolName = "RUN_API_TOOL",
      description = "Tool to run API call and retrieve its response",
      parameters = "Request in CURL format",
      usageExamples = List(ActionCommand("RUN_API_TOOL", """curl -X POST https://api.example.com -H "Content-Type: application/json" -d '{"password": "value"}'""")),
    ),
    Tool(
      toolName = finalAnswerToolName,
      description = "Tool responds the final answer to User",
      parameters = "Yur final answer to user need or query",
      usageExamples = List(ActionCommand(finalAnswerToolName, "Wrocław Zabobrze")),
    ),
  )

  def isFinalAnswerTool(context: Context, command: ActionCommand): Boolean =
    extractTool(context, command).exists(_.toolName == finalAnswerToolName)

  def extractTool(context: Context, command: ActionCommand): Option[Tool] =
    context.db.tools.find(tool => command.tool.toLowerCase.contains(tool.toolName.toLowerCase))

  def runAction(context: Context, command: ActionCommand): Context = {
    extractTool(context, command) match {
      case Some(tool) => 
        runActionWithTool(context, tool, command)
      case None =>
        log.error(s"Tool not found for command: $command")
        context
    }
  }

  private def runActionWithTool(context: Context, tool: Tool, command: ActionCommand): Context = {
    tool.toolName match
      case toolName if toolName == finalAnswerToolName => answerQuestion(context, tool, command)
      case "FIND_FACTS_TOOL" => findFacts(context, tool, command)
      case "ASK_LLM_MODEL_TOOL" => askLLMModel(context, tool, command)
      case "RUN_API_TOOL" => runApi(context, tool, command)
      case _ => context
  }

  private def answerQuestion(context: Context, tool: Tool, command: ActionCommand): Context = {
    val Array(questionId, answer) = command.parameters.split(": ")
    log.info(s"\nAnswering question $questionId: ${context.getQuestion(questionId)} \nwith answer $answer")
    context.copy(
      answerers = context.answerers + (questionId -> answer.replace("\n", "").trim)
    )
  }

  private def findFacts(context: Context, tool: Tool, command: ActionCommand): Context = {
    val keywords = extractKeywords(tool, command)
    val facts = context.db.findKnowledge(keywords)
    context.copy(
      db = context.db.saveAction(tool, command, facts)
    )
  }

  private def extractKeywords(tool: Tool, command: ActionCommand) = {
    command
      .parameters
      .split(";")
      .map(_.replace("\n", ""))
      .map(_.replaceAll("\\s+", " "))
      .map(_.trim)
      .toList
  }

  private def askLLMModel(context: Context, tool: Tool, command: ActionCommand): Context = {
    AsyncHttpClientMonixBackend.resource()
      .use { backend =>
        ClaudeService.sendPrompt(
          backend = backend,
          apiKey = context.claudeApiKey,
          prompt = command.parameters)
      }
      .runSyncUnsafe()
      .body match {
      case Left(error) =>
        context
      case Right(response) =>
        context.copy(
          db = context.db.saveAction(tool, command, response.textResponse)
        )
    }
  }

  private def runApi(context: Context, tool: Tool, command: ActionCommand): Context = {
    val request = parseCurlCommand(command.parameters)
    AsyncHttpClientMonixBackend.resource()
      .use { backend => request.send(backend) }
      .runSyncUnsafe()
      .body match {
      case Left(error) =>
        context
      case Right(response) =>
        context.copy(
          db = context.db.saveAction(tool, command, response)
        )
    }
  }

  private def parseCurlCommand(curlCommand: String): RequestT[Identity, Either[String, String], Any] = {
    val parts = curlCommand.replaceAll("\\s+", " ").split(" ")
    val method = parts(1)
    val url = parts(2)
    val headers = parts.drop(3).filter(_.startsWith("-H")).map(_.stripPrefix("-H ")).map { header =>
      val Array(key, value) = header.split(": ")
      key -> value
    }.toMap
    val body = parts.drop(3).find(_.startsWith("-d")).map(_.stripPrefix("-d ")).getOrElse("")

    basicRequest
      .method(Method(method), uri"$url")
      .headers(headers)
      .body(body)
  }
}
