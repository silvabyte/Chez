package chezwiz.agent.providers

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration
import chezwiz.agent.ChezError
import scala.util.{Try, Success, Failure}

/**
 * HTTP/1.1 client implementation for servers that don't support HTTP/2.
 * This is a workaround for requests-scala 0.9.0 proxy issues.
 */
object Http11Client {

  private val client = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_1_1)
    .connectTimeout(Duration.ofSeconds(15))
    .build()

  def post(
      url: String,
      headers: Map[String, String],
      body: String,
      readTimeout: Int = 60000
  ): Either[ChezError, String] = {
    Try {
      val requestBuilder = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofMillis(readTimeout))
        .POST(HttpRequest.BodyPublishers.ofString(body))

      headers.foreach { case (k, v) => requestBuilder.header(k, v) }

      val request = requestBuilder.build()
      val response = client.send(request, HttpResponse.BodyHandlers.ofString())

      if (response.statusCode() >= 400) {
        Left(ChezError.NetworkError(
          message = s"HTTP ${response.statusCode()}: ${response.body()}",
          statusCode = Some(response.statusCode())
        ))
      } else {
        Right(response.body())
      }
    } match {
      case Success(result) => result
      case Failure(ex: java.net.http.HttpTimeoutException) =>
        Left(ChezError.NetworkError(s"Request to $url timed out: ${ex.getMessage}"))
      case Failure(ex: java.net.ConnectException) =>
        Left(ChezError.NetworkError(s"Failed to connect to $url: ${ex.getMessage}"))
      case Failure(ex) =>
        // Enhanced error logging for debugging
        ex.printStackTrace()
        val errorMsg = ex match {
          case e: java.net.UnknownHostException =>
            s"Unknown host: ${e.getMessage} in url $url"
          case e: java.net.ConnectException =>
            s"Connection failed: ${e.getMessage} to $url"
          case e: java.net.http.HttpTimeoutException =>
            s"Request timeout: ${e.getMessage} for $url"
          case e =>
            s"Network request failed: ${e.getClass.getSimpleName}: ${e.getMessage}"
        }
        Left(ChezError.NetworkError(errorMsg))
    }
  }
}
