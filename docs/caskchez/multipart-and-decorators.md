# Multipart, Streaming, and Decorators

This guide covers three production essentials now supported by CaskChez:

- Multipart uploads (body isn’t pre‑consumed by validation)
- Streaming responses (pass through to Cask)
- Route decorators (built‑in and custom)

All examples below are implemented in:

- `CaskChez/src/main/scala/caskchez/examples/UploadStreamingAPI.scala`
- `CaskChez/src/main/scala/caskchez/examples/UploadStreamingServer.scala`

## Run the Demo Server

Start the server (blocks):

```bash
make example-caskchez-upload
# or customize
PORT=9000 make example-caskchez-upload
```

In another terminal, run the curl demos:

```bash
# multipart upload
make example-caskchez-upload-curl-upload

# streaming 1024 bytes
make example-caskchez-upload-curl-stream

# decorated route (gzip + custom header)
make example-caskchez-upload-curl-decorated
```

You can override host/port for the curl targets:

```bash
CASKCHEZ_HOST=127.0.0.1 CASKCHEZ_PORT=9000 make example-caskchez-upload-curl
```

## Multipart Uploads (Pass‑Through Body)

When `Content-Type` is not JSON (e.g., `multipart/form-data`), CaskChez leaves the request body untouched. You can parse the body downstream using Undertow’s `FormParserFactory`:

```scala
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
  // extract fields/files ...
  write(ujson.Obj("ok" -> true))
}
```

## Streaming Responses

Return a `cask.Response[geny.Readable]` from your handler; CaskChez passes it through unmodified. Example streams `size` bytes:

```scala
@CaskChez.get(
  "/demo/stream/:size",
  RouteSchema(
    summary = Some("Streaming response"),
    responses = Map(200 -> ApiResponse("OK", _root_.chez.Chez.String()))
  )
)
def stream(size: String, r: ValidatedRequest): cask.Response[geny.Readable] = {
  val n = size.toIntOption.getOrElse(0)
  val readable = new geny.Readable {
    def readBytesThrough[T](f: java.io.InputStream => T): T = {
      val is = new java.io.InputStream {
        private var remaining = n
        def read(): Int = if (remaining <= 0) -1 else { remaining -= 1; 'a' }
      }
      try f(is) finally is.close()
    }
  }
  cask.Response(readable, 200, Seq("Content-Type" -> "application/octet-stream"))
}
```

## Decorators (Built‑in + Custom)

CaskChez routes are standard Cask endpoints, so decorators work the same. You can stack built‑ins like `@cask.decorators.compress()` and custom decorators. Example custom header decorator:

```scala
class trace(header: String = "X-Trace") extends scala.annotation.Annotation with cask.router.RawDecorator {
  def wrapFunction(ctx: cask.Request, delegate: Delegate) = {
    delegate(ctx, Map()).map(resp => resp.copy(headers = resp.headers :+ (header -> "true")))
  }
}

@cask.decorators.compress()
@trace("X-Custom-Trace")
@CaskChez.get(
  "/demo/decorated",
  RouteSchema(responses = Map(200 -> ApiResponse("OK", _root_.chez.Chez.String())))
)
def decorated(r: ValidatedRequest): String = "decorated-ok"
```

The curl demo adds `Accept-Encoding: gzip` so you can see compression and the custom header in the response.

