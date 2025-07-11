package caskchez

import _root_.chez.*
import upickle.default.*
import scala.util.{Try, Success, Failure}

/**
 * Validation logic for CaskChez module
 *
 * This file contains the core validation functionality that integrates Chez schemas with Cask request processing for automatic
 * request validation.
 */

/**
 * Validation results for different types of validation
 */
sealed trait ValidationResult {
  def isValid: Boolean = this match {
    case Valid      => true
    case Invalid(_) => false
  }

  def errors: List[ValidationError] = this match {
    case Valid         => List.empty
    case Invalid(errs) => errs
  }
}

case object Valid extends ValidationResult
case class Invalid(override val errors: List[ValidationError]) extends ValidationResult

/**
 * Validation error types specific to web requests
 */
sealed trait ValidationError {
  def message: String
  def path: String
  def field: Option[String]
}

object ValidationError {
  case class RequestBodyError(message: String, path: String, field: Option[String] = None) extends ValidationError
  case class PathParamError(message: String, path: String, field: Option[String] = None) extends ValidationError
  case class QueryParamError(message: String, path: String, field: Option[String] = None) extends ValidationError
  case class HeaderError(message: String, path: String, field: Option[String] = None) extends ValidationError
  case class ContentTypeError(message: String, path: String, field: Option[String] = None) extends ValidationError
  case class SchemaError(message: String, path: String, field: Option[String] = None) extends ValidationError

  // fromChezError method temporarily removed - will be restored once Chez validation types are resolved
}

/**
 * Validated request container that holds both the original request and validated data
 */
case class ValidatedRequest(
    original: cask.Request,
    validatedBody: Option[ujson.Value] = None,
    validatedParams: Option[Map[String, String]] = None,
    validatedQuery: Option[Map[String, String]] = None,
    validatedHeaders: Option[Map[String, String]] = None
) {

  /**
   * Get validated body as a specific type
   */
  def getBody[T: ReadWriter]: Either[ValidationError, T] = {
    validatedBody match {
      case Some(body) =>
        Try(read[T](body)) match {
          case Success(value) => Right(value)
          case Failure(e)     => Left(ValidationError.RequestBodyError(e.getMessage, "/"))
        }
      case None => Left(ValidationError.RequestBodyError("No validated body available", "/"))
    }
  }

  /**
   * Get validated body as a specific type, throwing an exception if parsing fails This is useful for endpoints where validation
   * has already passed
   */
  def getBodyUnsafe[T: ReadWriter]: T = {
    getBody[T] match {
      case Right(value) => value
      case Left(error)  => throw new RuntimeException(s"Failed to parse validated body: ${error.message}")
    }
  }

  /**
   * Get validated parameter value
   */
  def getParam(name: String): Option[String] = {
    validatedParams.flatMap(_.get(name))
  }

  def getQuery[T: ReadWriter]: Either[ValidationError, T] = {
    validatedQuery match {
      case Some(query) =>
        Try(read[T](write(query))) match {
          case Success(value) => Right(value)
          case Failure(e)     => Left(ValidationError.QueryParamError(e.getMessage, "/"))
        }
      case None => Left(ValidationError.QueryParamError("No validated query parameters available", "/"))
    }
  }

  /**
   * Get validated query parameter value
   */
  def getQueryParam(name: String): Option[String] = {
    validatedQuery.flatMap(_.get(name))
  }

  /**
   * Get validated header value
   */
  def getHeader(name: String): Option[String] = {
    validatedHeaders.flatMap(_.get(name))
  }
}

// Validation methods removed - all validation now done inline in SchemaValidator.validateRequest

/**
 * Schema validator that combines all validation logic
 */
object SchemaValidator {

  /**
   * Validate a complete request against a RouteSchema
   */
  def validateRequest(
      request: cask.Request,
      routeSchema: RouteSchema,
      pathParams: Map[String, String] = Map.empty
  ): Either[List[ValidationError], ValidatedRequest] = {
    var allErrors = List.empty[ValidationError]
    var parsedBody: Option[ujson.Value] = None

    // Parse and validate body once if needed
    routeSchema.body.foreach { schema =>
      Try {
        val bodyBytes = request.data.readAllBytes()
        val bodyStr = new String(bodyBytes, "UTF-8")
        val bodyJson = ujson.read(bodyStr)
        parsedBody = Some(bodyJson) // Store parsed body for reuse

        // For now, just do basic JSON parsing validation
        // TODO: Add proper Chez schema validation once types are resolved

      } match {
        case Success(_) => // Body parsed successfully
        case Failure(e) => allErrors ++= List(ValidationError.RequestBodyError(s"Failed to parse body: ${e.getMessage}", "/"))
      }
    }

    // TODO: Add validation for params, query, headers once Chez types are resolved

    // If there are errors, return them
    if (allErrors.nonEmpty) {
      Left(allErrors)
    } else {
      // Create validated request with already parsed data
      val validatedRequest = ValidatedRequest(
        original = request,
        validatedBody = parsedBody,
        validatedParams = if (pathParams.nonEmpty) Some(pathParams) else None,
        validatedQuery =
          if (request.queryParams.nonEmpty) Some(request.queryParams.map { case (k, v) => k -> v.headOption.getOrElse("") })
          else None,
        validatedHeaders =
          if (request.headers.nonEmpty) Some(request.headers.map { case (k, v) => k -> v.headOption.getOrElse("") }) else None
      )

      Right(validatedRequest)
    }
  }

  /**
   * Create a validation error response
   */
  def createErrorResponse(errors: List[ValidationError], statusCode: Int = 400): cask.Response[ujson.Value] = {
    val errorResponse = ujson.Obj(
      "error" -> "Validation failed",
      "message" -> "Request validation failed",
      "details" -> ujson.Arr(errors.map { error =>
        ujson.Obj(
          "type" -> error.getClass.getSimpleName,
          "message" -> error.message,
          "path" -> error.path,
          "field" -> error.field.map(ujson.Str(_)).getOrElse(ujson.Null)
        )
      }*)
    )

    cask.Response(
      data = errorResponse,
      statusCode = statusCode,
      headers = Seq("Content-Type" -> "application/json")
    )
  }
}
