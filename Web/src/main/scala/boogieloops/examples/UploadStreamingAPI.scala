package boogieloops.web.examples

import cask._
import scala.annotation.unused
import upickle.default._
import boogieloops.web._
import boogieloops.web.Web.ValidatedRequestReader
import boogieloops.web.Web
import io.undertow.server.handlers.form.FormParserFactory
import os.*
import os.Source.*
import _root_.boogieloops.schema.*
import _root_.boogieloops.schema.{Schema as bl}

class trace(header: String = "X-Trace") extends scala.annotation.Annotation with cask.router.RawDecorator {
  def wrapFunction(ctx: cask.Request, delegate: Delegate) = {
    delegate(ctx, Map()).map(resp => resp.copy(headers = resp.headers :+ (header -> "true")))
  }
}

class UploadStreamingAPI extends cask.MainRoutes {

  case class UploadResponse(fields: Map[String, String], files: List[(String, Long)]) derives ReadWriter

  @cask.decorators.compress()
  @trace()
  @Web.post(
    "/demo/upload",
    RouteSchema(
      summary = Some("Multipart upload"),
      description = Some("Accepts multipart/form-data without consuming body"),
      tags = List("demo"),
      responses = Map(200 -> ApiResponse("OK", bl.String()))
    )
  )
  def upload(r: ValidatedRequest): String = {
    val parser = FormParserFactory.builder().build().createParser(r.original.exchange)
    val form = parser.parseBlocking()
    var fields = Map.empty[String, String]
    var files = List.empty[(String, Long)]
    // Use os-lib to create a temp directory for this upload
    val outDir = os.temp.dir(prefix = "boogieloops-web-upload-")
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
  @Web.post(
    "/demo/upload-stream",
    RouteSchema(
      summary = Some("Streaming upload"),
      description = Some("Accepts raw request body as a stream (geny.Readable)"),
      tags = List("demo"),
      responses = Map(200 -> ApiResponse("OK", bl.String()))
    )
  )
  def uploadStream(r: ValidatedRequest): String = {
    val headers = r.original.headers
    val fileName = headers.get("x-file-name").flatMap(_.headOption).getOrElse("upload.bin")
    val outDir = os.temp.dir(prefix = "boogieloops-web-upload-")
    val dest = outDir / fileName
    // cask.Request implements geny.Readable & geny.Writable; os.Source.WritableSource enables writing it
    os.write.over(dest, r.original)
    val size = os.size(dest)
    upickle.default.write(ujson.Obj(
      "fileName" -> fileName,
      "size" -> size,
      "path" -> dest.toString
    ))
  }

  @cask.decorators.compress()
  @trace()
  @Web.get(
    "/demo/stream/:size",
    RouteSchema(
      summary = Some("Streaming response"),
      description = Some("Streams N bytes to client"),
      tags = List("demo"),
      responses = Map(200 -> ApiResponse("OK", bl.String()))
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
  @Web.get(
    "/demo/decorated",
    RouteSchema(
      summary = Some("Decorated route"),
      description = Some("Shows custom and built-in decorators with boogieloops.web"),
      tags = List("demo"),
      responses = Map(200 -> ApiResponse("OK", bl.String()))
    )
  )
  def decorated(r: ValidatedRequest): String = {
    "decorated-ok"
  }

  initialize()
}
