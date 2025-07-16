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

class OpenAIProvider(protected val apiKey: String) extends BaseLLMProvider:

  override val name: String = "OpenAI"
  override protected val baseUrl: String = "https://api.openai.com/v1"

  override val supportedModels: List[String] = List(
    "gpt-4o",
    "gpt-4o-mini",
    "gpt-4-turbo",
    "gpt-4",
    "gpt-3.5-turbo"
  )

  override protected def buildHeaders(apiKey: String): Map[String, String] =
    Map(
      "Authorization" -> s"Bearer $apiKey",
      "Content-Type" -> "application/json"
    )

  override protected def buildRequestBody(request: ChatRequest): ujson.Value =
    val messages = ujson.Arr(
      request.messages.map(msg =>
        ujson.Obj(
          "role" -> (msg.role match
            case Role.System => "system"
            case Role.User => "user"
            case Role.Assistant => "assistant"),
          "content" -> msg.content
        )
      )*
    )

    val baseObj = ujson.Obj(
      "model" -> request.model,
      "messages" -> messages,
      "stream" -> request.stream
    )

    request.temperature.foreach(temp => baseObj("temperature") = temp)
    request.maxTokens.foreach(tokens => baseObj("max_tokens") = tokens)

    baseObj

  override protected def parseResponse(responseBody: String): Try[ChatResponse] =
    Try {
      val json = ujson.read(responseBody)

      if json.obj.contains("error") then
        val error = json("error")
        throw LLMError(
          message = error("message").str,
          code = error.obj.get("code").map(_.str)
        )

      val choice = json("choices")(0)
      val message = choice("message")
      val content = message("content").str
      val finishReason = choice.obj.get("finish_reason").map(_.str)

      val usage = json.obj.get("usage").map { u =>
        Usage(
          promptTokens = u("prompt_tokens").num.toInt,
          completionTokens = u("completion_tokens").num.toInt,
          totalTokens = u("total_tokens").num.toInt
        )
      }

      ChatResponse(
        content = content,
        usage = usage,
        model = json("model").str,
        finishReason = finishReason
      )
    }.recoverWith {
      case ex: ujson.ParsingFailedException =>
        Failure(LLMError(s"Failed to parse OpenAI response: ${ex.getMessage}"))
      case ex: NoSuchElementException =>
        Failure(LLMError(s"Missing required field in OpenAI response: ${ex.getMessage}"))
    }

  // Helper method to ensure schema is OpenAI compliant
  private def ensureOpenAICompliantSchema(schema: ujson.Value): ujson.Value =
    schema match
      case obj: ujson.Obj =>
        val newObj = obj.copy()

        // For object types, ensure OpenAI strict mode compliance
        if (obj.obj.get("type").exists(_.str == "object")) {
          // Add additionalProperties: false if not present
          if (!obj.obj.contains("additionalProperties")) {
            newObj("additionalProperties") = false
          }

          // Ensure all properties are required for strict mode
          obj.obj.get("properties") match {
            case Some(propsObj: ujson.Obj) =>
              val allPropertyKeys = propsObj.obj.keys.toList.sorted
              // If there's no required array or it doesn't contain all properties, update it
              val currentRequired = obj.obj.get("required") match {
                case Some(arr: ujson.Arr) => arr.arr.map(_.str).toSet
                case _ => Set.empty[String]
              }

              if (currentRequired != allPropertyKeys.toSet) {
                newObj("required") = ujson.Arr(allPropertyKeys.map(ujson.Str(_))*)
              }
            case _ => // No properties, no required array needed
          }
        }

        // Recursively process nested objects
        obj.obj.foreach { case (key, value) =>
          key match
            case "properties" =>
              value match
                case propsObj: ujson.Obj =>
                  val newProps = ujson.Obj()
                  propsObj.obj.foreach { case (propKey, propValue) =>
                    newProps(propKey) = ensureOpenAICompliantSchema(propValue)
                  }
                  newObj("properties") = newProps
                case _ => // Keep as is
            case "items" =>
              newObj("items") = ensureOpenAICompliantSchema(value)
            case _ => // Keep as is
        }
        newObj
      case _ => schema

  override protected def buildObjectRequestBody(request: ObjectRequest): ujson.Value =
    val messages = ujson.Arr(
      request.messages.map(msg =>
        ujson.Obj(
          "role" -> (msg.role match
            case Role.System => "system"
            case Role.User => "user"
            case Role.Assistant => "assistant"),
          "content" -> msg.content
        )
      )*
    )

    // Ensure the schema is OpenAI compliant
    val compliantSchema = ensureOpenAICompliantSchema(request.schema.schema)

    val baseObj = ujson.Obj(
      "model" -> request.model,
      "messages" -> messages,
      "stream" -> request.stream,
      "response_format" -> ujson.Obj(
        "type" -> "json_schema",
        "json_schema" -> ujson.Obj(
          "name" -> "structured_response",
          "schema" -> compliantSchema,
          "strict" -> true
        )
      )
    )

    request.temperature.foreach(temp => baseObj("temperature") = temp)
    request.maxTokens.foreach(tokens => baseObj("max_tokens") = tokens)

    baseObj

  override protected def parseObjectResponse(responseBody: String): Try[ObjectResponse] =
    Try {
      val json = ujson.read(responseBody)

      if json.obj.contains("error") then
        val error = json("error")
        throw LLMError(
          message = error("message").str,
          code = error.obj.get("code").map(_.str)
        )

      val choice = json("choices")(0)
      val message = choice("message")
      val content = message("content").str
      val finishReason = choice.obj.get("finish_reason").map(_.str)

      // Parse the JSON object from the content
      val objectContent = ujson.read(content)

      val usage = json.obj.get("usage").map { u =>
        Usage(
          promptTokens = u("prompt_tokens").num.toInt,
          completionTokens = u("completion_tokens").num.toInt,
          totalTokens = u("total_tokens").num.toInt
        )
      }

      ObjectResponse(
        `object` = objectContent,
        usage = usage,
        model = json("model").str,
        finishReason = finishReason
      )
    }.recoverWith {
      case ex: ujson.ParsingFailedException =>
        Failure(LLMError(s"Failed to parse OpenAI object response: ${ex.getMessage}"))
      case ex: NoSuchElementException =>
        Failure(LLMError(s"Missing required field in OpenAI object response: ${ex.getMessage}"))
    }
