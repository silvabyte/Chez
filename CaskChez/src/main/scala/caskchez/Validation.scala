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
    case Valid => true
    case Invalid(_) => false
  }

  def errors: List[ValidationError] = this match {
    case Valid => List.empty
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
  case class RequestBodyError(message: String, path: String, field: Option[String] = None)
      extends ValidationError
  case class PathParamError(message: String, path: String, field: Option[String] = None)
      extends ValidationError
  case class QueryParamError(message: String, path: String, field: Option[String] = None)
      extends ValidationError
  case class HeaderError(message: String, path: String, field: Option[String] = None)
      extends ValidationError
  case class ContentTypeError(message: String, path: String, field: Option[String] = None)
      extends ValidationError
  case class SchemaError(message: String, path: String, field: Option[String] = None)
      extends ValidationError

  def fromChezError(chezError: chez.ValidationError, context: String): ValidationError = {
    val errorType = context match {
      case "body" =>
        (msg: String, path: String, field: Option[String]) => RequestBodyError(msg, path, field)
      case "query" =>
        (msg: String, path: String, field: Option[String]) => QueryParamError(msg, path, field)
      case "params" =>
        (msg: String, path: String, field: Option[String]) => PathParamError(msg, path, field)
      case "headers" =>
        (msg: String, path: String, field: Option[String]) => HeaderError(msg, path, field)
      case _ => (msg: String, path: String, field: Option[String]) => SchemaError(msg, path, field)
    }

    chezError match {
      case chez.ValidationError.TypeMismatch(expected, actual, path) =>
        errorType(s"Type mismatch: expected $expected, got $actual", path, None)
      case chez.ValidationError.MissingField(field, path) =>
        errorType(s"Missing required field: $field", path, Some(field))
      case chez.ValidationError.InvalidFormat(format, value, path) =>
        errorType(s"Invalid format '$format' for value: $value", path, None)
      case chez.ValidationError.OutOfRange(min, max, value, path) =>
        val rangeDesc = (min, max) match {
          case (Some(minVal), Some(maxVal)) => s"between $minVal and $maxVal"
          case (Some(minVal), None) => s"at least $minVal"
          case (None, Some(maxVal)) => s"at most $maxVal"
          case (None, None) => "within valid range"
        }
        errorType(s"Value $value is not $rangeDesc", path, None)
      case chez.ValidationError.ParseError(message, path) =>
        errorType(s"Parse error: $message", path, None)
      case chez.ValidationError.PatternMismatch(pattern, value, path) =>
        errorType(s"Value '$value' does not match pattern: $pattern", path, None)
      case chez.ValidationError.AdditionalProperty(property, path) =>
        errorType(s"Additional property not allowed: $property", path, Some(property))
      case chez.ValidationError.UniqueViolation(path) =>
        errorType("Array items must be unique", path, None)
      case chez.ValidationError.MinItemsViolation(min, actual, path) =>
        errorType(s"Array must have at least $min items, got $actual", path, None)
      case chez.ValidationError.MaxItemsViolation(max, actual, path) =>
        errorType(s"Array must have at most $max items, got $actual", path, None)
      case chez.ValidationError.ContainsViolation(minContains, maxContains, actualContains, path) =>
        val containsDesc = (minContains, maxContains) match {
          case (Some(min), Some(max)) => s"between $min and $max"
          case (Some(min), None) => s"at least $min"
          case (None, Some(max)) => s"at most $max"
          case (None, None) => "valid number of"
        }
        errorType(
          s"Array must contain $containsDesc matching items, got $actualContains",
          path,
          None
        )
      case chez.ValidationError.MinLengthViolation(min, actual, path) =>
        errorType(s"String must be at least $min characters, got $actual", path, None)
      case chez.ValidationError.MaxLengthViolation(max, actual, path) =>
        errorType(s"String must be at most $max characters, got $actual", path, None)
      case chez.ValidationError.MinPropertiesViolation(min, actual, path) =>
        errorType(s"Object must have at least $min properties, got $actual", path, None)
      case chez.ValidationError.MaxPropertiesViolation(max, actual, path) =>
        errorType(s"Object must have at most $max properties, got $actual", path, None)
      case chez.ValidationError.MultipleOfViolation(multipleOf, value, path) =>
        errorType(s"Value $value must be a multiple of $multipleOf", path, None)
      case chez.ValidationError.CompositionError(message, path) =>
        errorType(s"Composition validation failed: $message", path, None)
      case chez.ValidationError.ReferenceError(ref, path) =>
        errorType(s"Reference error for $ref", path, None)
    }
  }
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
          case Failure(e) => Left(ValidationError.RequestBodyError(e.getMessage, "/"))
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
      case Left(error) =>
        throw new RuntimeException(s"Failed to parse validated body: ${error.message}")
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
          case Failure(e) => Left(ValidationError.QueryParamError(e.getMessage, "/"))
        }
      case None =>
        Left(ValidationError.QueryParamError("No validated query parameters available", "/"))
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

/**
 * Helper functions for different types of validation
 */
object ValidationHelpers {

  /**
   * Validate request body against schema
   */
  def validateRequestBody(
      request: cask.Request,
      bodySchema: Chez
  ): Either[List[ValidationError], ujson.Value] = {
    Try {
      val bodyBytes = request.data.readAllBytes()
      val bodyStr = new String(bodyBytes, "UTF-8")
      ujson.read(bodyStr)
    } match {
      case Success(bodyJson) =>
        val context = chez.validation.ValidationContext("/body")
        bodySchema.validate(bodyJson, context) match {
          case result if result.isValid => Right(bodyJson)
          case result => Left(result.errors.map(ValidationError.fromChezError(_, "body")))
        }
      case Failure(e) =>
        Left(List(ValidationError.RequestBodyError(
          s"Failed to parse JSON body: ${e.getMessage}",
          "/"
        )))
    }
  }

  /**
   * Validate query parameters against schema
   */
  def validateQueryParams(
      request: cask.Request,
      querySchema: Chez
  ): Either[List[ValidationError], Map[String, ujson.Value]] = {
    Try {
      // Convert query parameters to ujson.Value format
      val queryMap = request.queryParams.map { case (key, values) =>
        val value = values.headOption.getOrElse("")
        key -> convertStringToJson(value)
      }
      val queryJson = ujson.Obj.from(queryMap)

      val context = chez.validation.ValidationContext("/query")
      querySchema.validate(queryJson, context) match {
        case result if result.isValid =>
          Right(queryMap)
        case result =>
          Left(result.errors.map(ValidationError.fromChezError(_, "query")))
      }
    } match {
      case Success(result) => result
      case Failure(e) =>
        Left(List(ValidationError.QueryParamError(
          s"Failed to process query parameters: ${e.getMessage}",
          "/"
        )))
    }
  }

  /**
   * Validate path parameters against schema
   */
  def validatePathParams(
      pathParams: Map[String, String],
      paramsSchema: Chez
  ): Either[List[ValidationError], Map[String, ujson.Value]] = {
    Try {
      // Convert path parameters to ujson.Value format
      val paramMap = pathParams.map { case (key, value) =>
        key -> convertStringToJson(value)
      }
      val paramsJson = ujson.Obj.from(paramMap)

      val context = chez.validation.ValidationContext("/params")
      paramsSchema.validate(paramsJson, context) match {
        case result if result.isValid =>
          Right(paramMap)
        case result =>
          Left(result.errors.map(ValidationError.fromChezError(_, "params")))
      }
    } match {
      case Success(result) => result
      case Failure(e) =>
        Left(List(ValidationError.PathParamError(
          s"Failed to process path parameters: ${e.getMessage}",
          "/"
        )))
    }
  }

  /**
   * Validate request headers against schema
   */
  def validateHeaders(
      request: cask.Request,
      headersSchema: Chez
  ): Either[List[ValidationError], Map[String, ujson.Value]] = {
    Try {
      // Convert headers to ujson.Value format
      val headerMap = request.headers.map { case (key, values) =>
        val value = values.headOption.getOrElse("")
        key -> ujson.Str(value)
      }
      val headersJson = ujson.Obj.from(headerMap)

      val context = chez.validation.ValidationContext("/headers")
      headersSchema.validate(headersJson, context) match {
        case result if result.isValid =>
          Right(headerMap)
        case result =>
          Left(result.errors.map(ValidationError.fromChezError(_, "headers")))
      }
    } match {
      case Success(result) => result
      case Failure(e) =>
        Left(List(ValidationError.HeaderError(s"Failed to process headers: ${e.getMessage}", "/")))
    }
  }

  /**
   * Convert string values to appropriate JSON types based on content
   */
  private def convertStringToJson(value: String): ujson.Value = {
    // Try to parse as different types in order of specificity
    if (value.isEmpty) {
      ujson.Str(value)
    } else if (value == "true" || value == "false") {
      ujson.Bool(value.toBoolean)
    } else if (value == "null") {
      ujson.Null
    } else {
      // Try parsing as number
      Try(value.toDouble) match {
        case Success(num) if value.contains(".") => ujson.Num(num)
        case Success(num) if num.isWhole => ujson.Num(num.toLong)
        case Success(num) => ujson.Num(num)
        case Failure(_) => ujson.Str(value)
      }
    }
  }
}

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
    var validatedBody: Option[ujson.Value] = None
    var validatedQueryParams: Option[Map[String, ujson.Value]] = None
    var validatedPathParams: Option[Map[String, ujson.Value]] = None
    var validatedHeaders: Option[Map[String, ujson.Value]] = None

    // Validate request body if schema is provided
    routeSchema.body.foreach { bodySchema =>
      ValidationHelpers.validateRequestBody(request, bodySchema) match {
        case Right(body) => validatedBody = Some(body)
        case Left(errors) => allErrors ++= errors
      }
    }

    // Validate query parameters if schema is provided
    routeSchema.query.foreach { querySchema =>
      ValidationHelpers.validateQueryParams(request, querySchema) match {
        case Right(queryParams) => validatedQueryParams = Some(queryParams)
        case Left(errors) => allErrors ++= errors
      }
    }

    // Validate path parameters if schema is provided and params exist
    routeSchema.params.foreach { paramsSchema =>
      if (pathParams.nonEmpty) {
        ValidationHelpers.validatePathParams(pathParams, paramsSchema) match {
          case Right(params) => validatedPathParams = Some(params)
          case Left(errors) => allErrors ++= errors
        }
      }
    }

    // Validate headers if schema is provided
    routeSchema.headers.foreach { headersSchema =>
      ValidationHelpers.validateHeaders(request, headersSchema) match {
        case Right(headers) => validatedHeaders = Some(headers)
        case Left(errors) => allErrors ++= errors
      }
    }

    // If there are validation errors, return them
    if (allErrors.nonEmpty) {
      Left(allErrors)
    } else {
      // Create validated request with all validated data
      val validatedRequest = ValidatedRequest(
        original = request,
        validatedBody = validatedBody,
        validatedParams = validatedPathParams.map(_.map { case (k, v) => k -> v.toString }),
        validatedQuery = validatedQueryParams.map(_.map { case (k, v) => k -> v.toString }),
        validatedHeaders = validatedHeaders.map(_.map { case (k, v) => k -> v.toString })
      )

      Right(validatedRequest)
    }
  }

  /**
   * Create a validation error response
   */
  def createErrorResponse(
      errors: List[ValidationError],
      statusCode: Int = 400
  ): cask.Response[ujson.Value] = {
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
