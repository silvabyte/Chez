package chezwiz.agent.providers

import scala.util.Try
import chezwiz.agent.{ChatRequest, ChatResponse, ObjectRequest, RawObjectResponse, LLMError}

trait LLMProvider:
  def name: String
  def supportedModels: List[String]

  def chat(request: ChatRequest): ChatResponse
  def generateObject(request: ObjectRequest): RawObjectResponse
  def validateModel(model: String): Boolean = supportedModels.contains(model)

  protected def buildHeaders(apiKey: String): Map[String, String]
  protected def buildRequestBody(request: ChatRequest): ujson.Value
  protected def buildObjectRequestBody(request: ObjectRequest): ujson.Value
  protected def parseResponse(responseBody: String): Try[ChatResponse]
  protected def parseObjectResponse(responseBody: String): Try[RawObjectResponse]

abstract class BaseLLMProvider extends LLMProvider:

  protected def apiKey: String
  protected def baseUrl: String

  protected def makeRequest(
      url: String,
      headers: Map[String, String],
      body: ujson.Value
  ): String =
    val response = requests.post(
      url = url,
      headers = headers,
      data = body.toString(),
      readTimeout = 60000,
      connectTimeout = 15000
    )
    
    if response.statusCode >= 400 then
      val errorText = response.text()
      throw LLMError(
        message = s"HTTP ${response.statusCode}: $errorText",
        code = None,
        statusCode = Some(response.statusCode)
      )

    response.text()

  override def chat(request: ChatRequest): ChatResponse =
    if !validateModel(request.model) then
      throw LLMError(s"Model '${request.model}' not supported by ${name}", None, None)
    
    val headers = buildHeaders(apiKey)
    val body = buildRequestBody(request)
    val responseText = makeRequest(s"$baseUrl/chat/completions", headers, body)
    
    parseResponse(responseText) match
      case scala.util.Success(response) => response
      case scala.util.Failure(ex) => throw ex

  override def generateObject(request: ObjectRequest): RawObjectResponse =
    if !validateModel(request.model) then
      throw LLMError(s"Model '${request.model}' not supported by ${name}", None, None)
    
    val headers = buildHeaders(apiKey)
    val body = buildObjectRequestBody(request)
    val responseText = makeRequest(s"$baseUrl/chat/completions", headers, body)
    
    parseObjectResponse(responseText) match
      case scala.util.Success(response) => response
      case scala.util.Failure(ex) => throw ex
