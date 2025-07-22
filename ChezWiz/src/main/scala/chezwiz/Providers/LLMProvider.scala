package chezwiz.agent.providers

import chezwiz.agent.{ChatRequest, ChatResponse, ObjectRequest, ObjectResponse, ChezError}

trait LLMProvider:
  def name: String
  def supportedModels: List[String]

  def chat(request: ChatRequest): Either[ChezError, ChatResponse]
  def generateObject(request: ObjectRequest): Either[ChezError, ObjectResponse[ujson.Value]]
  def validateModel(model: String): Either[ChezError.ModelNotSupported, Unit] = {
    if supportedModels.contains(model) then Right(())
    else Left(ChezError.ModelNotSupported(model, name, supportedModels))
  }

  protected def buildHeaders(apiKey: String): Map[String, String]
  protected def buildRequestBody(request: ChatRequest): ujson.Value
  protected def buildObjectRequestBody(request: ObjectRequest): ujson.Value
  protected def parseResponse(responseBody: String): Either[ChezError, ChatResponse]
  protected def parseObjectResponse(responseBody: String)
      : Either[ChezError, ObjectResponse[ujson.Value]]

abstract class BaseLLMProvider extends LLMProvider:

  protected def apiKey: String
  protected def baseUrl: String

  protected def makeRequest(
      url: String,
      headers: Map[String, String],
      body: ujson.Value
  ): Either[ChezError, String] = {
    try {
      val response = requests.post(
        url = url,
        headers = headers,
        data = body.toString(),
        readTimeout = 60000,
        connectTimeout = 15000
      )

      if response.statusCode >= 400 then
        val errorText = response.text()
        Left(ChezError.NetworkError(
          message = s"HTTP ${response.statusCode}: $errorText",
          statusCode = Some(response.statusCode)
        ))
      else {
        Right(response.text())
      }
    } catch {
      case ex: Exception =>
        Left(ChezError.NetworkError(s"Network request failed: ${ex.getMessage}"))
    }
  }

  override def chat(request: ChatRequest): Either[ChezError, ChatResponse] = {
    for {
      _ <- validateModel(request.model)
      headers = buildHeaders(apiKey)
      body = buildRequestBody(request)
      responseText <- makeRequest(s"$baseUrl/chat/completions", headers, body)
      response <- parseResponse(responseText)
    } yield response
  }

  override def generateObject(request: ObjectRequest)
      : Either[ChezError, ObjectResponse[ujson.Value]] = {
    for {
      _ <- validateModel(request.model)
      headers = buildHeaders(apiKey)
      body = buildObjectRequestBody(request)
      responseText <- makeRequest(s"$baseUrl/chat/completions", headers, body)
      response <- parseObjectResponse(responseText)
    } yield response
  }
