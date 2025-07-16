package chezwiz.agent.providers

import upickle.default.*
import scala.util.{Try, Success, Failure}
import chezwiz.agent.{
  ChatRequest,
  ChatResponse,
  ObjectRequest,
  ObjectResponse,
  Role,
  Usage,
  LLMError
}

//TODO: use Chez library here instead
class AnthropicProvider(protected val apiKey: String) extends BaseLLMProvider:

  override val name: String = "Anthropic"
  override protected val baseUrl: String = "https://api.anthropic.com/v1"

  override val supportedModels: List[String] = List(
    "claude-3-5-sonnet-20241022",
    "claude-3-5-haiku-20241022",
    "claude-3-opus-20240229",
    "claude-3-sonnet-20240229",
    "claude-3-haiku-20240307"
  )

  override protected def buildHeaders(apiKey: String): Map[String, String] =
    Map(
      "x-api-key" -> apiKey,
      "Content-Type" -> "application/json",
      "anthropic-version" -> "2023-06-01"
    )

  override protected def buildRequestBody(request: ChatRequest): ujson.Value =
    // Anthropic separates system message from user/assistant messages
    val (systemChatMessage, conversationChatMessages) =
      request.messages.partition(_.role == Role.System)

    val messages = ujson.Arr(
      conversationChatMessages.map(msg =>
        ujson.Obj(
          "role" -> (msg.role match
            case Role.User => "user"
            case Role.Assistant => "assistant"
            case Role.System =>
              throw new IllegalStateException("System messages should be filtered out")),
          "content" -> msg.content
        )
      )*
    )

    val baseObj = ujson.Obj(
      "model" -> request.model,
      "messages" -> messages,
      "max_tokens" -> request.maxTokens.getOrElse(1024) // Anthropic requires max_tokens
    )

    // Add system message if present
    systemChatMessage.headOption.foreach(sys => baseObj("system") = sys.content)

    request.temperature.foreach(temp => baseObj("temperature") = temp)

    baseObj

  override protected def parseResponse(responseBody: String): Try[ChatResponse] =
    Try {
      val json = ujson.read(responseBody)

      if json.obj.contains("error") then
        val error = json("error")
        throw LLMError(
          message = error("message").str,
          code = error.obj.get("type").map(_.str)
        )

      val content = json("content")(0)("text").str
      val stopReason = json.obj.get("stop_reason").map(_.str)

      val usage = json.obj.get("usage").map { u =>
        Usage(
          promptTokens = u("input_tokens").num.toInt,
          completionTokens = u("output_tokens").num.toInt,
          totalTokens = u("input_tokens").num.toInt + u("output_tokens").num.toInt
        )
      }

      ChatResponse(
        content = content,
        usage = usage,
        model = json("model").str,
        finishReason = stopReason
      )
    }.recoverWith {
      case ex: ujson.ParsingFailedException =>
        Failure(LLMError(s"Failed to parse Anthropic response: ${ex.getMessage}"))
      case ex: NoSuchElementException =>
        Failure(LLMError(s"Missing required field in Anthropic response: ${ex.getMessage}"))
    }

  override protected def buildObjectRequestBody(request: ObjectRequest): ujson.Value =
    // Anthropic separates system message from user/assistant messages
    val (systemChatMessage, conversationChatMessages) =
      request.messages.partition(_.role == Role.System)

    val messages = ujson.Arr(
      conversationChatMessages.map(msg =>
        ujson.Obj(
          "role" -> (msg.role match
            case Role.User => "user"
            case Role.Assistant => "assistant"
            case Role.System =>
              throw new IllegalStateException("System messages should be filtered out")),
          "content" -> msg.content
        )
      )*
    )

    // Add schema-based tool for structured output
    val tool = ujson.Obj(
      "name" -> "structured_response",
      "description" -> request.schema.description.getOrElse(
        "Generate a structured response according to the provided schema"
      ),
      "input_schema" -> request.schema.schema
    )

    val baseObj = ujson.Obj(
      "model" -> request.model,
      "messages" -> messages,
      "max_tokens" -> request.maxTokens.getOrElse(1024),
      "tools" -> ujson.Arr(tool),
      "tool_choice" -> ujson.Obj("type" -> "tool", "name" -> "structured_response")
    )

    // Add system message if present
    systemChatMessage.headOption.foreach(sys => baseObj("system") = sys.content)

    request.temperature.foreach(temp => baseObj("temperature") = temp)

    baseObj

  override protected def parseObjectResponse(responseBody: String): Try[ObjectResponse] =
    Try {
      val json = ujson.read(responseBody)

      if json.obj.contains("error") then
        val error = json("error")
        throw LLMError(
          message = error("message").str,
          code = error.obj.get("type").map(_.str)
        )

      // Extract tool use result for structured output
      val content = json("content")
      val toolUse = content.arr
        .find(item => item.obj.contains("type") && item("type").str == "tool_use")
        .getOrElse(throw LLMError("No tool_use found in Anthropic structured response"))

      val objectContent = toolUse("input")
      val stopReason = json.obj.get("stop_reason").map(_.str)

      val usage = json.obj.get("usage").map { u =>
        Usage(
          promptTokens = u("input_tokens").num.toInt,
          completionTokens = u("output_tokens").num.toInt,
          totalTokens = u("input_tokens").num.toInt + u("output_tokens").num.toInt
        )
      }

      ObjectResponse(
        `object` = objectContent,
        usage = usage,
        model = json("model").str,
        finishReason = stopReason
      )
    }.recoverWith {
      case ex: ujson.ParsingFailedException =>
        Failure(LLMError(s"Failed to parse Anthropic object response: ${ex.getMessage}"))
      case ex: NoSuchElementException =>
        Failure(LLMError(s"Missing required field in Anthropic object response: ${ex.getMessage}"))
    }
