package caskchez.openapi.endpoints

import cask.{Request, Response}
import cask.router.Result
import upickle.default.*
import caskchez.openapi.config.OpenAPIConfig
import caskchez.openapi.generators.OpenAPIGenerator

/**
 * Custom Cask endpoint for serving OpenAPI 3.1.1 specifications
 * 
 * Usage: @chez.swagger("/openapi")
 */
class swagger(val path: String, config: OpenAPIConfig = OpenAPIConfig())
  extends cask.HttpEndpoint[String, Seq[String]] {
  
  val methods = Seq("get")
  
  def wrapFunction(ctx: Request, delegate: Delegate): Result[Response.Raw] = {
    try {
      // Generate OpenAPI document with intelligent caching
      val openAPIDoc = OpenAPIGenerator.generateDocument(config)
      
      Result.Success(Response(
        data = write(openAPIDoc),
        statusCode = 200,
        headers = Seq(
          "Content-Type" -> "application/json",
          "Access-Control-Allow-Origin" -> "*",  // Enable CORS for Swagger UI
          "Access-Control-Allow-Methods" -> "GET, OPTIONS",
          "Access-Control-Allow-Headers" -> "Content-Type"
        )
      ))
    } catch {
      case e: Exception =>
        Result.Success(Response(
          data = write(ujson.Obj(
            "error" -> "Failed to generate OpenAPI specification",
            "message" -> e.getMessage,
            "type" -> "OpenAPIGenerationError"
          )),
          statusCode = 500,
          headers = Seq("Content-Type" -> "application/json")
        ))
    }
  }

  def wrapPathSegment(s: String): Seq[String] = Seq(s)
  type InputParser[T] = cask.endpoints.QueryParamReader[T]
}