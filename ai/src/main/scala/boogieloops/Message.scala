package boogieloops.ai

import boogieloops.schema.derivation.Schema
import boogieloops.schema.Schema
import upickle.default.ReadWriter

given schemaVectorFloat: Schema[Vector[Float]] = {
  import boogieloops.schema.derivation.CollectionSchemas.given_Schema_Vector
  summon[Schema[Vector[Float]]]
}

sealed trait Role derives Schema, ReadWriter

object Role:
  case object System extends Role
  case object User extends Role
  case object Assistant extends Role

case class RequestMetadata(
    @Schema.description("Unique identifier for the conversation")
    conversationId: Option[String] = None,
    @Schema.description("Unique identifier for the user")
    userId: Option[String] = None,
    @Schema.description("Unique identifier for the tenant/organization")
    tenantId: Option[String] = None
) derives Schema, ReadWriter

sealed trait MessageContent derives Schema, ReadWriter

object MessageContent:
  case class Text(text: String) extends MessageContent derives Schema, ReadWriter
  case class ImageUrl(
      @Schema.description("The URL of the image")
      url: String,
      @Schema.description("The detail level for image processing")
      detail: String = "auto"
  ) extends MessageContent derives Schema, ReadWriter
  case class MultiModal(
      @Schema.description("Array of content parts")
      parts: List[MessageContentPart]
  ) extends MessageContent derives Schema, ReadWriter

sealed trait MessageContentPart derives Schema, ReadWriter

object MessageContentPart:
  case class TextPart(
      @Schema.description("The type of content part")
      `type`: String = "text",
      @Schema.description("The text content")
      text: String
  ) extends MessageContentPart derives Schema, ReadWriter

  case class ImageUrlPart(
      @Schema.description("The type of content part")
      `type`: String = "image_url",
      @Schema.description("The image URL object")
      image_url: ImageUrlContent
  ) extends MessageContentPart derives Schema, ReadWriter

case class ImageUrlContent(
    @Schema.description("The URL of the image")
    url: String,
    @Schema.description("The detail level for image processing")
    detail: String = "auto"
) derives Schema, ReadWriter

case class ChatMessage(
    @Schema.description("The role of the message sender")
    role: Role,
    @Schema.description("The content of the message")
    content: MessageContent
) derives Schema, ReadWriter

object ChatMessage:
  def text(role: Role, text: String): ChatMessage =
    ChatMessage(role, MessageContent.Text(text))

  def multiModal(role: Role, parts: List[MessageContentPart]): ChatMessage =
    ChatMessage(role, MessageContent.MultiModal(parts))

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
    stream: Boolean = false,
    @Schema.description("Optional metadata for request scoping")
    metadata: Option[RequestMetadata] = None
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

// Sealed trait hierarchy for all possible errors
sealed trait SchemaError derives Schema, ReadWriter

object SchemaError:
  case class NetworkError(message: String, statusCode: Option[Int] = None) extends SchemaError
      derives Schema, ReadWriter
  case class ParseError(message: String, cause: Option[String] = None) extends SchemaError
      derives Schema, ReadWriter
  case class ModelNotSupported(model: String, provider: String, supportedModels: List[String])
      extends SchemaError derives Schema, ReadWriter
  case class ApiError(message: String, code: Option[String] = None, statusCode: Option[Int] = None)
      extends SchemaError derives Schema, ReadWriter
  case class SchemaConversionError(message: String, targetType: String) extends SchemaError
      derives Schema, ReadWriter
  case class ConfigurationError(message: String) extends SchemaError derives Schema, ReadWriter

// Request for structured object generation
case class ObjectRequest(
    @Schema.description("List of messages in the conversation")
    messages: List[ChatMessage],
    @Schema.description("The model to use for generation")
    model: String,
    @Schema.description("The Schema for structured output")
    schema: Schema,
    @Schema.description("Sampling temperature (0.0 to 2.0)")
    @Schema.minimum(0.0)
    @Schema.maximum(2.0)
    temperature: Option[Double] = None,
    @Schema.description("Maximum tokens to generate")
    @Schema.minimum(1)
    maxTokens: Option[Int] = None,
    @Schema.description("Whether to stream the response")
    @Schema.default(false)
    stream: Boolean = false,
    @Schema.description("Optional metadata for request scoping")
    metadata: Option[RequestMetadata] = None
)

// Response containing structured object (type-safe)
case class ObjectResponse[T](
    @Schema.description("The generated structured object")
    data: T,
    @Schema.description("Token usage information")
    usage: Option[Usage] = None,
    @Schema.description("The model used for generation")
    model: String,
    @Schema.description("The reason the generation finished")
    finishReason: Option[String] = None
) derives Schema, ReadWriter

// Embedding input type - can be single text or multiple texts
sealed trait EmbeddingInput derives Schema, ReadWriter
object EmbeddingInput:
  case class Single(text: String) extends EmbeddingInput derives Schema, ReadWriter
  case class Multiple(texts: List[String]) extends EmbeddingInput derives Schema, ReadWriter

// Embedding-specific types
case class EmbeddingRequest(
    @Schema.description("Text or array of texts to embed")
    input: EmbeddingInput,
    @Schema.description("The embedding model to use")
    model: String,
    @Schema.description("Optional dimensions parameter (for models that support it)")
    dimensions: Option[Int] = None,
    @Schema.description("Encoding format: 'float' or 'base64'")
    @Schema.default("float")
    encodingFormat: String = "float",
    @Schema.description("Optional metadata for request scoping")
    metadata: Option[RequestMetadata] = None
) derives Schema, ReadWriter

case class Embedding(
    @Schema.description("The embedding vector")
    values: Vector[Float],
    @Schema.description("Index in the batch")
    index: Int
) derives Schema, ReadWriter

case class EmbeddingResponse(
    @Schema.description("List of embeddings")
    embeddings: List[Embedding],
    @Schema.description("Token usage information")
    usage: Option[Usage] = None,
    @Schema.description("The model used for generation")
    model: String,
    @Schema.description("Dimensions of the embeddings")
    dimensions: Int
) derives Schema, ReadWriter
