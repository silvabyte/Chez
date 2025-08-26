package caskchez

import _root_.chez.*
import cask.{Request, Response}
import cask.router.Result
import upickle.default.*
import scala.reflect.ClassTag
import caskchez.openapi.config.OpenAPIConfig
import caskchez.openapi.generators.OpenAPIGenerator

/**
 * Thread-local storage for ValidatedRequest to pass data from endpoint to parameter readers
 */
object ValidatedRequestStore {
  private val store = new ThreadLocal[ValidatedRequest]()

  def set(request: ValidatedRequest): Unit = store.set(request)
  def get(): Option[ValidatedRequest] = Option(store.get())
  def clear(): Unit = store.remove()
}

/**
 * Unified custom endpoint that provides Fastify-like schema validation with complete route schema support
 *
 * This endpoint supports:
 *   - Headers validation
 *   - Body validation with direct typed data injection
 *   - Query parameters validation
 *   - Path parameters validation
 *   - Response validation (in development mode)
 *   - Automatic route registration for OpenAPI generation
 */
object CaskChez {

  /**
   * POST endpoint with complete route schema validation Usage: @CaskChez.post("/path", completeRouteSchema)
   */
  class post(val path: String, routeSchema: RouteSchema)
      extends cask.HttpEndpoint[Response.Raw, Seq[String]] {

    // Register this route immediately when the class is instantiated
    RouteSchemaRegistry.register(path, "POST", routeSchema)

    val methods = Seq("post")

    def wrapFunction(ctx: Request, delegate: Delegate): Result[Response.Raw] = {

      // TODO: use Try, Success, Failure
      try {
        // Validate the complete request against the route schema
        SchemaValidator.validateRequest(ctx, routeSchema) match {
          case Right(validatedRequest) =>
            // Store validated request in thread-local storage
            ValidatedRequestStore.set(validatedRequest)

            // TODO: use Try, Success, Failure
            try {
              // Call the route method with all validated data available
              delegate(ctx, Map.empty).map { result =>
                // The result is already a Response.Raw, so return it directly
                result
              }
            } finally {
              ValidatedRequestStore.clear()
            }
          case Left(errors) =>
            // If validation fails, return error response
            Result.Success(SchemaValidator.createErrorResponse(errors, 400))
        }
      } catch {
        case e: Exception =>
          Result.Success(
            SchemaValidator.createErrorResponse(
              List(caskchez.ValidationError.SchemaError(
                s"Validation failed: ${e.getMessage}",
                "/"
              )),
              500
            )
          )
      }
    }

    def wrapPathSegment(s: String): Seq[String] = Seq(s)

    type InputParser[T] = cask.endpoints.QueryParamReader[T]
  }

  // Parameter readers for accessing validated data in route methods

  /**
   * Custom parameter reader for ValidatedRequest - provides access to all validated data
   */
  implicit object ValidatedRequestReader extends cask.endpoints.QueryParamReader[ValidatedRequest] {
    def arity = 0
    def read(ctx: Request, label: String, v: Seq[String]): ValidatedRequest = {
      ValidatedRequestStore
        .get()
        .getOrElse {
          // Return an empty ValidatedRequest with error indication
          // The endpoint should handle this case appropriately
          ValidatedRequest(
            original = ctx,
            validatedBody = None,
            validatedQuery = None,
            validatedParams = None,
            validatedHeaders = None
          )
        }
    }
  }

  /**
   * GET endpoint with complete route schema validation Usage: @CaskChez.get("/path", completeRouteSchema)
   */
  class get(val path: String, routeSchema: RouteSchema)
      extends cask.HttpEndpoint[String, Seq[String]] {

    // Register this route immediately when the class is instantiated
    RouteSchemaRegistry.register(path, "GET", routeSchema)

    val methods = Seq("get")

    def wrapFunction(ctx: Request, delegate: Delegate): Result[Response.Raw] = {

      // TODO: use Try, Success, Failure
      try {
        // Validate the complete request against the route schema (query params, headers, path params)
        SchemaValidator.validateRequest(ctx, routeSchema) match {
          case Right(validatedRequest) =>
            // Store validated request in thread-local storage
            ValidatedRequestStore.set(validatedRequest)

            // TODO: use Try, Success, Failure
            try {
              // Call the route method with all validated data available
              delegate(ctx, Map.empty).map { result =>
                // Use the first successful response schema for status code, or default to 200
                val statusCode = routeSchema.responses.keys.headOption match {
                  case Some(code: Int) => code
                  case Some(code: String) => code.toIntOption.getOrElse(200)
                  case None => 200
                }
                Response(
                  result,
                  statusCode = statusCode,
                  headers = Seq("Content-Type" -> "application/json")
                )
              }
            } finally {
              ValidatedRequestStore.clear()
            }
          case Left(errors) =>
            // If validation fails, return error response
            Result.Success(SchemaValidator.createErrorResponse(errors, 400))
        }
      } catch {
        case e: Exception =>
          Result.Success(
            SchemaValidator.createErrorResponse(
              List(caskchez.ValidationError.SchemaError(
                s"Validation failed: ${e.getMessage}",
                "/"
              )),
              500
            )
          )
      }
    }

    def wrapPathSegment(s: String): Seq[String] = Seq(s)
    type InputParser[T] = cask.endpoints.QueryParamReader[T]
  }

  /**
   * PUT endpoint with complete route schema validation Usage: @CaskChez.put("/path", completeRouteSchema)
   */
  class put(val path: String, routeSchema: RouteSchema)
      extends cask.HttpEndpoint[String, Seq[String]] {

    // Register this route immediately when the class is instantiated
    RouteSchemaRegistry.register(path, "PUT", routeSchema)

    val methods = Seq("put")

    def wrapFunction(ctx: Request, delegate: Delegate): Result[Response.Raw] = {

      // TODO: use Try, Success, Failure
      try {
        // Validate the complete request against the route schema
        SchemaValidator.validateRequest(ctx, routeSchema) match {
          case Right(validatedRequest) =>
            // Store validated request in thread-local storage
            ValidatedRequestStore.set(validatedRequest)

            // TODO: use Try, Success, Failure
            try {
              // Call the route method with all validated data available
              delegate(ctx, Map.empty).map { result =>
                // Use the first successful response schema for status code, or default to 200
                val statusCode = routeSchema.responses.keys.headOption match {
                  case Some(code: Int) => code
                  case Some(code: String) => code.toIntOption.getOrElse(200)
                  case None => 200
                }
                Response(
                  result,
                  statusCode = statusCode,
                  headers = Seq("Content-Type" -> "application/json")
                )
              }
            } finally {
              ValidatedRequestStore.clear()
            }
          case Left(errors) =>
            // If validation fails, return error response
            Result.Success(SchemaValidator.createErrorResponse(errors, 400))
        }
      } catch {
        case e: Exception =>
          Result.Success(
            SchemaValidator.createErrorResponse(
              List(caskchez.ValidationError.SchemaError(
                s"Validation failed: ${e.getMessage}",
                "/"
              )),
              500
            )
          )
      }
    }

    def wrapPathSegment(s: String): Seq[String] = Seq(s)
    type InputParser[T] = cask.endpoints.QueryParamReader[T]
  }

  /**
   * PATCH endpoint with complete route schema validation Usage: @CaskChez.patch("/path", completeRouteSchema)
   */
  class patch(val path: String, routeSchema: RouteSchema)
      extends cask.HttpEndpoint[String, Seq[String]] {

    // Register this route immediately when the class is instantiated
    RouteSchemaRegistry.register(path, "PATCH", routeSchema)

    val methods = Seq("patch")

    def wrapFunction(ctx: Request, delegate: Delegate): Result[Response.Raw] = {

      // TODO: use Try, Success, Failure
      try {
        // Validate the complete request against the route schema
        SchemaValidator.validateRequest(ctx, routeSchema) match {
          case Right(validatedRequest) =>
            // Store validated request in thread-local storage
            ValidatedRequestStore.set(validatedRequest)

            // TODO: use Try, Success, Failure
            try {
              // Call the route method with all validated data available
              delegate(ctx, Map.empty).map { result =>
                // Use the first successful response schema for status code, or default to 200
                val statusCode = routeSchema.responses.keys.headOption match {
                  case Some(code: Int) => code
                  case Some(code: String) => code.toIntOption.getOrElse(200)
                  case None => 200
                }
                Response(
                  result,
                  statusCode = statusCode,
                  headers = Seq("Content-Type" -> "application/json")
                )
              }
            } finally {
              ValidatedRequestStore.clear()
            }
          case Left(errors) =>
            // If validation fails, return error response
            Result.Success(SchemaValidator.createErrorResponse(errors, 400))
        }
      } catch {
        case e: Exception =>
          Result.Success(
            SchemaValidator.createErrorResponse(
              List(caskchez.ValidationError.SchemaError(
                s"Validation failed: ${e.getMessage}",
                "/"
              )),
              500
            )
          )
      }
    }

    def wrapPathSegment(s: String): Seq[String] = Seq(s)
    type InputParser[T] = cask.endpoints.QueryParamReader[T]
  }

  /**
   * DELETE endpoint with complete route schema validation Usage: @CaskChez.delete("/path", completeRouteSchema)
   */
  class delete(val path: String, routeSchema: RouteSchema)
      extends cask.HttpEndpoint[String, Seq[String]] {

    // Register this route immediately when the class is instantiated
    RouteSchemaRegistry.register(path, "DELETE", routeSchema)

    val methods = Seq("delete")

    def wrapFunction(ctx: Request, delegate: Delegate): Result[Response.Raw] = {

      // TODO: use Try, Success, Failure
      try {
        // Validate the complete request against the route schema (usually query params, headers, path params)
        SchemaValidator.validateRequest(ctx, routeSchema) match {
          case Right(validatedRequest) =>
            // Store validated request in thread-local storage
            ValidatedRequestStore.set(validatedRequest)

            // TODO: use Try, Success, Failure
            try {
              // Call the route method with all validated data available
              delegate(ctx, Map.empty).map { result =>
                // Use the first successful response schema for status code, or default to 204 for DELETE
                val statusCode = routeSchema.responses.keys.headOption match {
                  case Some(code: Int) => code
                  case Some(code: String) => code.toIntOption.getOrElse(204)
                  case None => 204 // No Content is typical for DELETE
                }
                Response(
                  result,
                  statusCode = statusCode,
                  headers = Seq("Content-Type" -> "application/json")
                )
              }
            } finally {
              ValidatedRequestStore.clear()
            }
          case Left(errors) =>
            // If validation fails, return error response
            Result.Success(SchemaValidator.createErrorResponse(errors, 400))
        }
      } catch {
        case e: Exception =>
          Result.Success(
            SchemaValidator.createErrorResponse(
              List(caskchez.ValidationError.SchemaError(
                s"Validation failed: ${e.getMessage}",
                "/"
              )),
              500
            )
          )
      }
    }

    def wrapPathSegment(s: String): Seq[String] = Seq(s)
    type InputParser[T] = cask.endpoints.QueryParamReader[T]
  }

  /**
   * OpenAPI 3.1.1 Swagger endpoint for automatic API documentation Usage: @CaskChez.swagger("/openapi")
   */
  class swagger(val path: String, config: OpenAPIConfig = OpenAPIConfig())
      extends cask.HttpEndpoint[String, Seq[String]] {

    val methods = Seq("get")

    def wrapFunction(ctx: Request, delegate: Delegate): Result[Response.Raw] = {

      // TODO: use Try, Success, Failure
      try {
        // Generate OpenAPI document with intelligent caching
        val openAPIDoc = OpenAPIGenerator.generateDocument(config)

        Result.Success(
          Response(
            data = write(openAPIDoc),
            statusCode = 200,
            headers = Seq(
              "Content-Type" -> "application/json",
              "Access-Control-Allow-Origin" -> "*", // Enable CORS for Swagger UI
              "Access-Control-Allow-Methods" -> "GET, OPTIONS",
              "Access-Control-Allow-Headers" -> "Content-Type"
            )
          )
        )
      } catch {
        case e: Exception =>
          Result.Success(
            Response(
              data = write(
                ujson.Obj(
                  "error" -> "Failed to generate OpenAPI specification",
                  "message" -> e.getMessage,
                  "type" -> "OpenAPIGenerationError"
                )
              ),
              statusCode = 500,
              headers = Seq("Content-Type" -> "application/json")
            )
          )
      }
    }

    def wrapPathSegment(s: String): Seq[String] = Seq(s)
    type InputParser[T] = cask.endpoints.QueryParamReader[T]
  }
}
