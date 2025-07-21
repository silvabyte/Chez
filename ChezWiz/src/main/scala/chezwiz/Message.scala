package chezwiz.agent

import upickle.default.*
import ujson.Value
import chez.*
import chez.derivation.Schema

sealed trait Role derives Schema, ReadWriter

object Role:
  case object System extends Role
  case object User extends Role
  case object Assistant extends Role

case class ChatMessage(
    @Schema.description("The role of the message sender")
    role: Role,
    
    @Schema.description("The content of the message")
    content: String
) derives Schema, ReadWriter

case class ChatRequest(
    @Schema.description("List of messages in the conversation")
    messages: List[ChatMessage],
    
    @Schema.description("The model to use for generation")
    model: String,
    
    @Schema.description("Sampling temperature (0.0 to 2.0)")
    @Schema.minimum(0.0)
    @Schema.maximum(2.0)
    temperature: Option[Double] = None,
    
    @Schema.description("Maximum tokens to generate")
    @Schema.minimum(1)
    maxTokens: Option[Int] = None,
    
    @Schema.description("Whether to stream the response")
    @Schema.default(false)
    stream: Boolean = false
) derives Schema, ReadWriter

case class Usage(
    @Schema.description("Number of tokens in the prompt")
    @Schema.minimum(0)
    promptTokens: Int,
    
    @Schema.description("Number of tokens in the completion")
    @Schema.minimum(0)
    completionTokens: Int,
    
    @Schema.description("Total number of tokens used")
    @Schema.minimum(0)
    totalTokens: Int
) derives Schema, ReadWriter

case class ChatResponse(
    @Schema.description("The generated content")
    content: String,
    
    @Schema.description("Token usage information")
    usage: Option[Usage] = None,
    
    @Schema.description("The model used for generation")
    model: String,
    
    @Schema.description("The reason the generation finished")
    finishReason: Option[String] = None
) derives Schema, ReadWriter

case class LLMError(
    message: String,
    code: Option[String] = None,
    statusCode: Option[Int] = None
) extends Exception(message)

// Direct Chez usage - no wrapper needed
object ChezSchemaHelpers {
  extension (chez: Chez) {
    def toJsonValue: ujson.Value = chez.toJsonSchema
  }
}

// Request for structured object generation
case class ObjectRequest(
    @Schema.description("List of messages in the conversation")
    messages: List[ChatMessage],
    
    @Schema.description("The model to use for generation")
    model: String,
    
    @Schema.description("The Chez schema for structured output")
    schema: Chez,
    
    @Schema.description("Sampling temperature (0.0 to 2.0)")
    @Schema.minimum(0.0)
    @Schema.maximum(2.0)
    temperature: Option[Double] = None,
    
    @Schema.description("Maximum tokens to generate")
    @Schema.minimum(1)
    maxTokens: Option[Int] = None,
    
    @Schema.description("Whether to stream the response")
    @Schema.default(false)
    stream: Boolean = false
) {
  // Convert schema to JSON for API calls
  def schemaAsJson: ujson.Value = schema.toJsonSchema
}

// Response containing structured object (type-safe)
case class ObjectResponse[T](
    @Schema.description("The generated structured object")
    `object`: T,
    
    @Schema.description("Token usage information")
    usage: Option[Usage] = None,
    
    @Schema.description("The model used for generation")
    model: String,
    
    @Schema.description("The reason the generation finished")
    finishReason: Option[String] = None
)

// Raw response with ujson.Value for internal provider parsing
case class RawObjectResponse(
    @Schema.description("The generated structured object as raw JSON")
    `object`: ujson.Value,
    
    @Schema.description("Token usage information")
    usage: Option[Usage] = None,
    
    @Schema.description("The model used for generation")
    model: String,
    
    @Schema.description("The reason the generation finished")
    finishReason: Option[String] = None
) derives ReadWriter {
  // Convert to type-safe ObjectResponse
  def to[T: Reader]: ObjectResponse[T] = ObjectResponse[T](
    `object` = read[T](`object`),
    usage = usage,
    model = model,
    finishReason = finishReason
  )
}

