package chezwiz.agent.providers

import scala.concurrent.Future
import scala.util.Try
import chezwiz.agent.{ChatRequest, ChatResponse, ObjectRequest, ObjectResponse, LLMError}

trait LLMProvider:
  def name: String
  def supportedModels: List[String]

  def chat(request: ChatRequest): Future[ChatResponse]
  def generateObject(request: ObjectRequest): Future[ObjectResponse]
  def validateModel(model: String): Boolean = supportedModels.contains(model)

  protected def buildHeaders(apiKey: String): Map[String, String]
  protected def buildRequestBody(request: ChatRequest): ujson.Value
  protected def buildObjectRequestBody(request: ObjectRequest): ujson.Value
  protected def parseResponse(responseBody: String): Try[ChatResponse]
  protected def parseObjectResponse(responseBody: String): Try[ObjectResponse]

abstract class BaseLLMProvider extends LLMProvider:
  import scala.concurrent.ExecutionContext.Implicits.global

  protected def apiKey: String
  protected def baseUrl: String

  protected def makeRequest(
      url: String,
      headers: Map[String, String],
      body: ujson.Value
  ): Future[String] =
    Future {
      val response = requests.post(
        url = url,
        headers = headers,
        data = body.toString(),
        readTimeout = 30000,
        connectTimeout = 10000
      )

      if response.statusCode >= 400 then
        throw LLMError(
          message = s"HTTP ${response.statusCode}: ${response.text()}",
          code = None,
          statusCode = Some(response.statusCode)
        )

      response.text()
    }

  override def chat(request: ChatRequest): Future[ChatResponse] =
    for
      _ <- Future.successful(
        if !validateModel(request.model) then
          throw LLMError(s"Model '${request.model}' not supported by ${name}", None, None)
      )
      headers = buildHeaders(apiKey)
      body = buildRequestBody(request)
      responseText <- makeRequest(s"$baseUrl/chat/completions", headers, body)
      response <- Future.fromTry(parseResponse(responseText))
    yield response

  override def generateObject(request: ObjectRequest): Future[ObjectResponse] =
    for
      _ <- Future.successful(
        if !validateModel(request.model) then
          throw LLMError(s"Model '${request.model}' not supported by ${name}", None, None)
      )
      headers = buildHeaders(apiKey)
      body = buildObjectRequestBody(request)
      responseText <- makeRequest(s"$baseUrl/chat/completions", headers, body)
      response <- Future.fromTry(parseObjectResponse(responseText))
    yield response
