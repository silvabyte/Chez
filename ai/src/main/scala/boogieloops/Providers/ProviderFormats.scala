package boogieloops.ai.providers

import upickle.default.ReadWriter
import boogieloops.ai.{ChatResponse, Usage, ObjectResponse}

// Provider-specific response formats for automatic parsing

// OpenAI API response format
case class OpenAIMessage(
    role: String,
    content: String
) derives ReadWriter

case class OpenAIChoice(
    message: OpenAIMessage,
    finish_reason: Option[String],
    index: Int
) derives ReadWriter

case class OpenAIUsage(
    prompt_tokens: Int,
    completion_tokens: Int,
    total_tokens: Int
) derives ReadWriter

case class OpenAIResponse(
    id: String,
    `object`: String,
    created: Long,
    model: String,
    choices: List[OpenAIChoice],
    usage: Option[OpenAIUsage]
) derives ReadWriter {
  // Convert to unified ChatResponse
  def toChatResponse: ChatResponse = {
    val firstChoice = choices.head
    ChatResponse(
      content = firstChoice.message.content,
      usage = usage.map(u => Usage(u.prompt_tokens, u.completion_tokens, u.total_tokens)),
      model = model,
      finishReason = firstChoice.finish_reason
    )
  }

  // Convert to unified ObjectResponse with ujson.Value (for structured responses)
  def toObjectResponse: ObjectResponse[ujson.Value] = {
    val firstChoice = choices.head
    val contentJson = ujson.read(firstChoice.message.content)
    ObjectResponse[ujson.Value](
      data = contentJson,
      usage = usage.map(u => Usage(u.prompt_tokens, u.completion_tokens, u.total_tokens)),
      model = model,
      finishReason = firstChoice.finish_reason
    )
  }
}

// Error response format (common to both providers)
case class ErrorDetails(
    message: String,
    `type`: Option[String] = None,
    code: Option[String] = None
) derives ReadWriter

case class ErrorResponse(
    error: ErrorDetails
) derives ReadWriter
