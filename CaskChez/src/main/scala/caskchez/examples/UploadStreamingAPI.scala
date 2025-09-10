package caskchez.examples

import cask._
import scala.annotation.unused
import upickle.default._
import caskchez._
import caskchez.CaskChez.ValidatedRequestReader
import caskchez.CaskChez
import io.undertow.server.handlers.form.FormParserFactory
import os.*

class trace(header: String = "X-Trace") extends scala.annotation.Annotation with cask.router.RawDecorator {
  def wrapFunction(ctx: cask.Request, delegate: Delegate) = {
    delegate(ctx, Map()).map(resp => resp.copy(headers = resp.headers :+ (header -> "true")))
  }
}

class UploadStreamingAPI extends cask.MainRoutes {

  case class UploadResponse(fields: Map[String, String], files: List[(String, Long)]) derives ReadWriter

  @cask.decorators.compress()
  @trace()
  @CaskChez.post(
    "/demo/upload",
    RouteSchema(
      summary = Some("Multipart upload"),
      description = Some("Accepts multipart/form-data without consuming body"),
      tags = List("demo"),
      responses = Map(200 -> ApiResponse("OK", _root_.chez.Chez.String()))
    )
  )
  def upload(r: ValidatedRequest): String = {
    val parser = FormParserFactory.builder().build().createParser(r.original.exchange)
    val form = parser.parseBlocking()
    var fields = Map.empty[String, String]
    var files = List.empty[(String, Long)]
    // Use os-lib to create a temp directory for this upload
    val outDir = os.temp.dir(prefix = "caskchez-upload-")
    val it = form.iterator()
    while (it.hasNext) {
      val name = it.next()
      val vs = form.get(name).iterator()
      while (vs.hasNext) {
        val v = vs.next()
        if (v.isFileItem) {
          val fi = v.getFileItem
          files = files :+ (name -> fi.getFileSize)
        } else fields = fields.updated(name, v.getValue)
      }
    }
    // Persist a simple note.txt if provided to demonstrate os-lib
    fields.get("note").foreach { txt =>
      os.write.over(outDir / "note.txt", txt)
    }
    upickle.default.write(UploadResponse(fields, files))
  }

  @cask.decorators.compress()
  @trace()
  @CaskChez.get(
    "/demo/stream/:size",
    RouteSchema(
      summary = Some("Streaming response"),
      description = Some("Streams N bytes to client"),
      tags = List("demo"),
      responses = Map(200 -> ApiResponse("OK", _root_.chez.Chez.String()))
    )
  )
  def stream(size: String, @unused r: ValidatedRequest): cask.Response[String] = {
    val n = math.max(0, size.toIntOption.getOrElse(0))
    val data = "a" * n
    cask.Response(
      data = data,
      statusCode = 200,
      headers = Seq("Content-Type" -> "application/octet-stream")
    )
  }

  @cask.decorators.compress()
  @trace("X-Custom-Trace")
  @CaskChez.get(
    "/demo/decorated",
    RouteSchema(
      summary = Some("Decorated route"),
      description = Some("Shows custom and built-in decorators with CaskChez"),
      tags = List("demo"),
      responses = Map(200 -> ApiResponse("OK", _root_.chez.Chez.String()))
    )
  )
  def decorated(r: ValidatedRequest): String = {
    "decorated-ok"
  }

  initialize()
}
