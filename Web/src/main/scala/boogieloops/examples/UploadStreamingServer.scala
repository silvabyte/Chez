package boogieloops.web.examples

import io.undertow.Undertow

object UploadStreamingServer {
  def main(args: Array[String]): Unit = {
    val port = sys.env.get("PORT").flatMap(_.toIntOption).getOrElse(8080)
    val routes = new UploadStreamingAPI()

    val server = Undertow.builder
      .addHttpListener(port, "0.0.0.0")
      .setHandler(routes.defaultHandler)
      .build

    server.start()
    println(s"UploadStreamingAPI started on http://localhost:$port")
    println(
      "Endpoints:\n  POST /demo/upload (multipart)\n  GET /demo/stream/:size (stream)\n  GET /demo/decorated (decorators)"
    )
  }
}
