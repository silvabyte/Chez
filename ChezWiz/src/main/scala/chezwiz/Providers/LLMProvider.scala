package chezwiz.agent.providers

import chezwiz.agent.{
  ChatRequest,
  ChatResponse,
  ObjectRequest,
  ObjectResponse,
  ChezError,
  EmbeddingRequest,
  EmbeddingResponse,
  EmbeddingInput
}
import scala.util.{Try, Success, Failure}
import scala.annotation.unused

enum HttpVersion:
  case Http11
  case Http2

case class ProviderTimeouts(requestTimeout: Int = 60_000, connectTimeout: Int = 30_000)

trait LLMProvider:
  def name: String
  def supportedModels: List[String]
  def httpVersion: HttpVersion = HttpVersion.Http2
  def timeouts: ProviderTimeouts = ProviderTimeouts()

  def chat(request: ChatRequest): Either[ChezError, ChatResponse]
  def generateObject(request: ObjectRequest): Either[ChezError, ObjectResponse[ujson.Value]]
  def validateModel(model: String): Either[ChezError.ModelNotSupported, Unit] = {
    if supportedModels.contains(model) then Right(())
    else Left(ChezError.ModelNotSupported(model, name, supportedModels))
  }

  // Embedding support
  def supportsEmbeddings: Boolean = false
  def supportedEmbeddingModels: List[String] = List.empty
  def embed(@unused request: EmbeddingRequest): Either[ChezError, EmbeddingResponse] =
    Left(ChezError.ConfigurationError(s"Provider $name does not support embeddings"))

  def embedBatch(texts: List[String], model: String): Either[ChezError, EmbeddingResponse] =
    embed(EmbeddingRequest(EmbeddingInput.Multiple(texts), model))

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
    httpVersion match {
      case HttpVersion.Http11 =>
        Http11Client.post(url, headers, body.toString(), readTimeout = timeouts.requestTimeout)

      case HttpVersion.Http2 =>
        Try {
          val session = requests.Session()
          val response = session.post(
            url = url,
            headers = headers,
            data = body.toString(),
            readTimeout = timeouts.requestTimeout,
            connectTimeout = timeouts.connectTimeout
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
        } match {
          case Success(result) => result
          case Failure(ex) =>
            Left(ChezError.NetworkError(s"Network request failed: ${ex.getMessage}"))
        }
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
