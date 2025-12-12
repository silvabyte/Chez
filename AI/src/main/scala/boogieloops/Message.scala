package boogieloops.ai

import boogieloops.schema.derivation.Schematic
import boogieloops.schema.Schema
import upickle.default.ReadWriter

given schemaVectorFloat: Schematic[Vector[Float]] = {
  import boogieloops.schema.derivation.CollectionSchemas.given_Schematic_Vector
  summon[Schematic[Vector[Float]]]
}

sealed trait Role derives Schematic, ReadWriter

object Role:
  case object System extends Role
  case object User extends Role
  case object Assistant extends Role

case class RequestMetadata(
    @Schematic.description("Unique identifier for the conversation")
    conversationId: Option[String] = None,
    @Schematic.description("Unique identifier for the user")
    userId: Option[String] = None,
    @Schematic.description("Unique identifier for the tenant/organization")
    tenantId: Option[String] = None
) derives Schematic, ReadWriter

sealed trait MessageContent derives Schematic, ReadWriter

object MessageContent:
  case class Text(text: String) extends MessageContent derives Schematic, ReadWriter
  case class ImageUrl(
      @Schematic.description("The URL of the image")
      url: String,
      @Schematic.description("The detail level for image processing")
      detail: String = "auto"
  ) extends MessageContent derives Schematic, ReadWriter
  case class MultiModal(
      @Schematic.description("Array of content parts")
      parts: List[MessageContentPart]
  ) extends MessageContent derives Schematic, ReadWriter

sealed trait MessageContentPart derives Schematic, ReadWriter

object MessageContentPart:
  case class TextPart(
      @Schematic.description("The type of content part")
      `type`: String = "text",
      @Schematic.description("The text content")
      text: String
  ) extends MessageContentPart derives Schematic, ReadWriter

  case class ImageUrlPart(
      @Schematic.description("The type of content part")
      `type`: String = "image_url",
      @Schematic.description("The image URL object")
      image_url: ImageUrlContent
  ) extends MessageContentPart derives Schematic, ReadWriter

case class ImageUrlContent(
    @Schematic.description("The URL of the image")
    url: String,
    @Schematic.description("The detail level for image processing")
    detail: String = "auto"
) derives Schematic, ReadWriter

case class ChatMessage(
    @Schematic.description("The role of the message sender")
    role: Role,
    @Schematic.description("The content of the message")
    content: MessageContent
) derives Schematic, ReadWriter

object ChatMessage:
  def text(role: Role, text: String): ChatMessage =
    ChatMessage(role, MessageContent.Text(text))

  def multiModal(role: Role, parts: List[MessageContentPart]): ChatMessage =
    ChatMessage(role, MessageContent.MultiModal(parts))

case class ChatRequest(
    @Schematic.description("List of messages in the conversation")
    messages: List[ChatMessage],
    @Schematic.description("The model to use for generation")
    model: String,
    @Schematic.description("Sampling temperature (0.0 to 2.0)")
    @Schematic.minimum(0.0)
    @Schematic.maximum(2.0)
    temperature: Option[Double] = None,
    @Schematic.description("Maximum tokens to generate")
    @Schematic.minimum(1)
    maxTokens: Option[Int] = None,
    @Schematic.description("Whether to stream the response")
    @Schematic.default(false)
    stream: Boolean = false,
    @Schematic.description("Optional metadata for request scoping")
    metadata: Option[RequestMetadata] = None
) derives Schematic, ReadWriter

case class Usage(
    @Schematic.description("Number of tokens in the prompt")
    @Schematic.minimum(0)
    promptTokens: Int,
    @Schematic.description("Number of tokens in the completion")
    @Schematic.minimum(0)
    completionTokens: Int,
    @Schematic.description("Total number of tokens used")
    @Schematic.minimum(0)
    totalTokens: Int
) derives Schematic, ReadWriter

case class ChatResponse(
    @Schematic.description("The generated content")
    content: String,
    @Schematic.description("Token usage information")
    usage: Option[Usage] = None,
    @Schematic.description("The model used for generation")
    model: String,
    @Schematic.description("The reason the generation finished")
    finishReason: Option[String] = None
) derives Schematic, ReadWriter

// Sealed trait hierarchy for all possible errors
sealed trait SchemaError derives Schematic, ReadWriter

object SchemaError:
  case class NetworkError(message: String, statusCode: Option[Int] = None) extends SchemaError
      derives Schematic, ReadWriter
  case class ParseError(message: String, cause: Option[String] = None) extends SchemaError
      derives Schematic, ReadWriter
  case class ModelNotSupported(model: String, provider: String, supportedModels: List[String])
      extends SchemaError derives Schematic, ReadWriter
  case class ApiError(message: String, code: Option[String] = None, statusCode: Option[Int] = None)
      extends SchemaError derives Schematic, ReadWriter
  case class SchemaConversionError(message: String, targetType: String) extends SchemaError
      derives Schematic, ReadWriter
  case class ConfigurationError(message: String) extends SchemaError derives Schematic, ReadWriter

// Request for structured object generation
case class ObjectRequest(
    @Schematic.description("List of messages in the conversation")
    messages: List[ChatMessage],
    @Schematic.description("The model to use for generation")
    model: String,
    @Schematic.description("The Schema for structured output")
    schema: Schema,
    @Schematic.description("Sampling temperature (0.0 to 2.0)")
    @Schematic.minimum(0.0)
    @Schematic.maximum(2.0)
    temperature: Option[Double] = None,
    @Schematic.description("Maximum tokens to generate")
    @Schematic.minimum(1)
    maxTokens: Option[Int] = None,
    @Schematic.description("Whether to stream the response")
    @Schematic.default(false)
    stream: Boolean = false,
    @Schematic.description("Optional metadata for request scoping")
    metadata: Option[RequestMetadata] = None
)

// Response containing structured object (type-safe)
case class ObjectResponse[T](
    @Schematic.description("The generated structured object")
    data: T,
    @Schematic.description("Token usage information")
    usage: Option[Usage] = None,
    @Schematic.description("The model used for generation")
    model: String,
    @Schematic.description("The reason the generation finished")
    finishReason: Option[String] = None
) derives Schematic, ReadWriter

// Embedding input type - can be single text or multiple texts
sealed trait EmbeddingInput derives Schematic, ReadWriter
object EmbeddingInput:
  case class Single(text: String) extends EmbeddingInput derives Schematic, ReadWriter
  case class Multiple(texts: List[String]) extends EmbeddingInput derives Schematic, ReadWriter

// Embedding-specific types
case class EmbeddingRequest(
    @Schematic.description("Text or array of texts to embed")
    input: EmbeddingInput,
    @Schematic.description("The embedding model to use")
    model: String,
    @Schematic.description("Optional dimensions parameter (for models that support it)")
    dimensions: Option[Int] = None,
    @Schematic.description("Encoding format: 'float' or 'base64'")
    @Schematic.default("float")
    encodingFormat: String = "float",
    @Schematic.description("Optional metadata for request scoping")
    metadata: Option[RequestMetadata] = None
) derives Schematic, ReadWriter

case class Embedding(
    @Schematic.description("The embedding vector")
    values: Vector[Float],
    @Schematic.description("Index in the batch")
    index: Int
) derives Schematic, ReadWriter

case class EmbeddingResponse(
    @Schematic.description("List of embeddings")
    embeddings: List[Embedding],
    @Schematic.description("Token usage information")
    usage: Option[Usage] = None,
    @Schematic.description("The model used for generation")
    model: String,
    @Schematic.description("Dimensions of the embeddings")
    dimensions: Int
) derives Schematic, ReadWriter
