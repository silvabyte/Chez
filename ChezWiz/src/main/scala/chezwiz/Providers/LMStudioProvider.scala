package chezwiz.agent.providers

import upickle.default.*
import chezwiz.agent.{
  ChatRequest,
  ChatResponse,
  ObjectRequest,
  ObjectResponse,
  Role,
  Usage,
  ChezError,
  MessageContent,
  MessageContentPart,
  EmbeddingRequest,
  EmbeddingResponse,
  Embedding,
  EmbeddingInput
}

/**
 * Provider for LM Studio local LLM server.
 * LM Studio runs OpenAI-compatible API locally without authentication.
 *
 * @param baseUrl The base URL of the LM Studio server (e.g., "http://localhost:1234/v1")
 * @param modelId The model ID to use (defaults to "local-model")
 * @param httpVersionParam The HTTP version to use (defaults to Http2, but Http11 may be more reliable for local servers)
 */
class LMStudioProvider(
    override protected val baseUrl: String,
    val modelId: String = "local-model",
    val httpVersionParam: HttpVersion = HttpVersion.Http2
) extends BaseLLMProvider:

  override val httpVersion: HttpVersion = httpVersionParam

  override val name: String = s"LMStudio($baseUrl)"

  // LM Studio doesn't require API key
  protected val apiKey: String = ""

  // Allow any model by default, or restrict to the specified model
  override val supportedModels: List[String] = List(modelId)

  override protected def buildHeaders(apiKey: String): Map[String, String] = {
    // LM Studio only needs Content-Type header
    Map("Content-Type" -> "application/json")
  }

  override protected def buildRequestBody(request: ChatRequest): ujson.Value = {
    val messages = ujson.Arr(
      request.messages.map(msg => {
        val roleStr = msg.role match
          case Role.System => "system"
          case Role.User => "user"
          case Role.Assistant => "assistant"

        val contentValue = msg.content match {
          case MessageContent.Text(text) => ujson.Str(text)
          case MessageContent.ImageUrl(url, detail) =>
            ujson.Arr(
              ujson.Obj("type" -> "text", "text" -> ""),
              ujson.Obj(
                "type" -> "image_url",
                "image_url" -> ujson.Obj(
                  "url" -> url,
                  "detail" -> detail
                )
              )
            )
          case MessageContent.MultiModal(parts) =>
            ujson.Arr(
              parts.map {
                case MessageContentPart.TextPart(_, text) =>
                  ujson.Obj("type" -> "text", "text" -> text)
                case MessageContentPart.ImageUrlPart(_, imageUrl) =>
                  ujson.Obj(
                    "type" -> "image_url",
                    "image_url" -> ujson.Obj(
                      "url" -> imageUrl.url,
                      "detail" -> imageUrl.detail
                    )
                  )
              }*
            )
        }

        ujson.Obj(
          "role" -> roleStr,
          "content" -> contentValue
        )
      })*
    )

    val baseObj = ujson.Obj(
      "model" -> request.model,
      "messages" -> messages,
      "stream" -> request.stream
    )

    request.temperature.foreach(temp => baseObj("temperature") = temp)
    request.maxTokens.foreach(tokens => baseObj("max_tokens") = tokens)

    baseObj
  }

  override protected def buildObjectRequestBody(request: ObjectRequest): ujson.Value = {
    val messages = ujson.Arr(
      request.messages.map(msg => {
        val roleStr = msg.role match
          case Role.System => "system"
          case Role.User => "user"
          case Role.Assistant => "assistant"

        val contentValue = msg.content match {
          case MessageContent.Text(text) => ujson.Str(text)
          case MessageContent.ImageUrl(url, detail) =>
            ujson.Arr(
              ujson.Obj("type" -> "text", "text" -> ""),
              ujson.Obj(
                "type" -> "image_url",
                "image_url" -> ujson.Obj(
                  "url" -> url,
                  "detail" -> detail
                )
              )
            )
          case MessageContent.MultiModal(parts) =>
            ujson.Arr(
              parts.map {
                case MessageContentPart.TextPart(_, text) =>
                  ujson.Obj("type" -> "text", "text" -> text)
                case MessageContentPart.ImageUrlPart(_, imageUrl) =>
                  ujson.Obj(
                    "type" -> "image_url",
                    "image_url" -> ujson.Obj(
                      "url" -> imageUrl.url,
                      "detail" -> imageUrl.detail
                    )
                  )
              }*
            )
        }

        ujson.Obj(
          "role" -> roleStr,
          "content" -> contentValue
        )
      })*
    )

    val baseObj = ujson.Obj(
      "model" -> request.model,
      "messages" -> messages,
      "stream" -> request.stream
    )

    // LM Studio uses json_schema format for structured output
    baseObj("response_format") = ujson.Obj(
      "type" -> "json_schema",
      "json_schema" -> ujson.Obj(
        "name" -> "structured_output",
        "strict" -> "true",
        "schema" -> request.schema.toJsonSchema
      )
    )

    request.temperature.foreach(temp => baseObj("temperature") = temp)
    request.maxTokens.foreach(tokens => baseObj("max_tokens") = tokens)

    baseObj
  }

  override protected def parseResponse(responseBody: String): Either[ChezError, ChatResponse] = {
    try {
      try {
        val openAIResponse = read[OpenAIResponse](responseBody)
        Right(openAIResponse.toChatResponse)
      } catch {
        case _: upickle.core.AbortException | _: ujson.ParsingFailedException =>
          try {
            val errorResponse = read[ErrorResponse](responseBody)
            Left(ChezError.ApiError(
              message = errorResponse.error.message,
              code = errorResponse.error.code.orElse(errorResponse.error.`type`),
              statusCode = None
            ))
          } catch {
            case _: Exception =>
              Left(ChezError.ParseError(s"Failed to parse LM Studio response: $responseBody"))
          }
      }
    } catch {
      case ex: ujson.ParsingFailedException =>
        Left(ChezError.ParseError(s"Failed to parse LM Studio response: ${ex.getMessage}"))
      case ex: NoSuchElementException =>
        Left(ChezError.ParseError(
          s"Missing required field in LM Studio response: ${ex.getMessage}"
        ))
      case ex: Exception =>
        Left(ChezError.ParseError(
          s"Unexpected error parsing LM Studio response: ${ex.getMessage}"
        ))
    }
  }

  override protected def parseObjectResponse(responseBody: String)
      : Either[ChezError, ObjectResponse[ujson.Value]] = {
    try {
      try {
        val openAIResponse = read[OpenAIResponse](responseBody)
        Right(openAIResponse.toObjectResponse)
      } catch {
        case _: upickle.core.AbortException | _: ujson.ParsingFailedException =>
          try {
            val errorResponse = read[ErrorResponse](responseBody)
            Left(ChezError.ApiError(
              message = errorResponse.error.message,
              code = errorResponse.error.code.orElse(errorResponse.error.`type`),
              statusCode = None
            ))
          } catch {
            case _: Exception =>
              Left(ChezError.ParseError(
                s"Failed to parse LM Studio object response: $responseBody"
              ))
          }
      }
    } catch {
      case ex: ujson.ParsingFailedException =>
        Left(
          ChezError.ParseError(s"Failed to parse LM Studio response body: ${ex.getMessage}")
        )
      case ex: NoSuchElementException =>
        Left(ChezError.ParseError(
          s"Missing required field in LM Studio response: ${ex.getMessage}"
        ))
      case ex: Exception =>
        Left(ChezError.ParseError(s"Unexpected error in parseObjectResponse: ${ex.getMessage}"))
    }
  }

  override def validateModel(model: String): Either[ChezError.ModelNotSupported, Unit] = {
    // Allow any model for LM Studio since users can load different models
    Right(())
  }

  // Embedding support
  override val supportsEmbeddings: Boolean = true
  override val supportedEmbeddingModels: List[String] = List("*") // Accept any embedding model
  
  override def embed(request: EmbeddingRequest): Either[ChezError, EmbeddingResponse] = {
    val url = s"$baseUrl/embeddings"
    val body = buildEmbeddingRequestBody(request)
    
    for {
      responseText <- makeRequest(url, buildHeaders(""), body)
      response <- parseEmbeddingResponse(responseText)
    } yield response
  }
  
  private def buildEmbeddingRequestBody(request: EmbeddingRequest): ujson.Value = {
    val baseObj = ujson.Obj(
      "model" -> request.model,
      "input" -> (request.input match {
        case EmbeddingInput.Single(text) => ujson.Str(text)
        case EmbeddingInput.Multiple(texts) => ujson.Arr(texts.map(ujson.Str(_))*)
      })
    )
    
    request.dimensions.foreach(d => baseObj("dimensions") = d)
    if (request.encodingFormat != "float") {
      baseObj("encoding_format") = request.encodingFormat
    }
    
    baseObj
  }
  
  private def parseEmbeddingResponse(responseBody: String): Either[ChezError, EmbeddingResponse] = {
    try {
      val json = ujson.read(responseBody)
      
      // Parse the LM Studio embedding response format
      val data = json("data").arr
      val embeddings = data.zipWithIndex.map { case (item, idx) =>
        val embeddingArray = item("embedding").arr.map(_.num.toFloat).toVector
        Embedding(
          values = embeddingArray,
          index = item.obj.get("index").map(_.num.toInt).getOrElse(idx)
        )
      }.toList
      
      val usage = json.obj.get("usage").map { u =>
        Usage(
          promptTokens = u("prompt_tokens").num.toInt,
          completionTokens = u.obj.get("completion_tokens").map(_.num.toInt).getOrElse(0),
          totalTokens = u("total_tokens").num.toInt
        )
      }
      
      val model = json("model").str
      val dimensions = embeddings.headOption.map(_.values.size).getOrElse(0)
      
      Right(EmbeddingResponse(
        embeddings = embeddings,
        usage = usage,
        model = model,
        dimensions = dimensions
      ))
    } catch {
      case ex: ujson.ParsingFailedException =>
        Left(ChezError.ParseError(s"Failed to parse LM Studio embedding response: ${ex.getMessage}"))
      case ex: NoSuchElementException =>
        Left(ChezError.ParseError(s"Missing required field in LM Studio embedding response: ${ex.getMessage}"))
      case ex: Exception =>
        Left(ChezError.ParseError(s"Unexpected error parsing LM Studio embedding response: ${ex.getMessage}"))
    }
  }

object LMStudioProvider:
  /**
   * Create a new LM Studio provider
   *
   * @param baseUrl The base URL of the LM Studio server (e.g., "http://localhost:1234/v1")
   * @param modelId The model ID to use (defaults to "local-model")
   * @param httpVersion The HTTP version to use (defaults to Http2, but Http11 may be more reliable for local servers)
   */
  def apply(
      baseUrl: String = "http://localhost:1234/v1",
      modelId: String = "local-model",
      httpVersion: HttpVersion = HttpVersion.Http2
  ): LMStudioProvider =
    new LMStudioProvider(baseUrl, modelId, httpVersionParam = httpVersion)
