package chezwiz.agent.providers

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Duration
import chezwiz.agent.ChezError
import scala.util.{Try, Success, Failure}

/**
 * A simple HTTP client for local network requests that bypasses proxy issues
 * in requests-scala 0.9.0. This is a workaround until the library is upgraded.
 */
object LocalNetworkHttpClient {

  private val client = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_1_1) // Force HTTP/1.1 for compatibility with local services
    .connectTimeout(Duration.ofSeconds(15))
    .build()

  def isLocalAddress(url: String): Boolean = {
    Try {
      val uri = new URI(url)
      val host = uri.getHost
      host != null && (
        host == "localhost" ||
          host == "127.0.0.1" ||
          host.startsWith("192.168.") ||
          host.startsWith("10.") ||
          host.startsWith("172.16.") ||
          host.startsWith("172.17.") ||
          host.startsWith("172.18.") ||
          host.startsWith("172.19.") ||
          host.startsWith("172.20.") ||
          host.startsWith("172.21.") ||
          host.startsWith("172.22.") ||
          host.startsWith("172.23.") ||
          host.startsWith("172.24.") ||
          host.startsWith("172.25.") ||
          host.startsWith("172.26.") ||
          host.startsWith("172.27.") ||
          host.startsWith("172.28.") ||
          host.startsWith("172.29.") ||
          host.startsWith("172.30.") ||
          host.startsWith("172.31.")
      )
    }.getOrElse(false)
  }

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
        Left(ChezError.NetworkError(s"Network request failed: ${ex.getMessage}"))
    }
  }
}
