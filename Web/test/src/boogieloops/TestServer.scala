package boogieloops.web

import io.undertow.Undertow
import scala.util.Random

/**
 * Test server for running boogieloops.web API tests
 *
 * Provides a simple server setup for integration testing with automatic port selection
 * and proper lifecycle management.
 */
object TestServer {

  @volatile private var server: Option[Undertow] = None
  @volatile private var currentPort: Option[Int] = None
  @volatile private var currentRoutes: Option[TestUserCrudAPI] = None

  // Find an available port
  private def findAvailablePort(): Int = {
    val basePort = 18080
    val maxAttempts = 100

    val availablePort = (0 until maxAttempts).iterator.map { _ =>
      val port = basePort + Random.nextInt(1000)
      try {
        val testServer = java.net.ServerSocket(port)
        testServer.close()
        Some(port)
      } catch {
        case _: java.io.IOException => None // Port is in use, try next
      }
    }.collectFirst { case Some(port) => port }

    availablePort.getOrElse(
      throw new RuntimeException("Could not find an available port for testing")
    )
  }

  def startServer(): (String, TestUserCrudAPI) = this.synchronized {
    if (server.isEmpty) {
      val port = findAvailablePort()
      val routes = new TestUserCrudAPI()

      val newServer = Undertow.builder
        .addHttpListener(port, "localhost")
        .setHandler(routes.defaultHandler)
        .build

      newServer.start()
      server = Some(newServer)
      currentPort = Some(port)
      currentRoutes = Some(routes)

      // Give server time to start
      Thread.sleep(200)
    }

    val port = currentPort.getOrElse(throw new RuntimeException("Server port not available"))
    val routes = currentRoutes.getOrElse(throw new RuntimeException("Server routes not available"))

    (s"http://localhost:$port", routes)
  }

  def stopServer(): Unit = this.synchronized {
    server.foreach(_.stop())
    server = None
    currentPort = None
    currentRoutes = None
  }

  def withServer[T](f: (String, TestUserCrudAPI) => T): T = {
    val (host, routes) = startServer()
    try {
      routes.resetUsers() // Reset state for clean tests
      f(host, routes)
    } finally {
      stopServer()
    }
  }
}
