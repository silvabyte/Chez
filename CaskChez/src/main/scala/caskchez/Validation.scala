package caskchez

import chez.*
import chez.primitives.*
import chez.complex.*
import upickle.default.*
import scala.util.{Try, Success, Failure}

/**
 * Validation logic for CaskChez module
 * 
 * This file contains the core validation functionality that integrates Chez schemas
 * with Cask request processing for automatic request validation.
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
  case class RequestBodyError(message: String, path: String, field: Option[String] = None) extends ValidationError
  case class PathParamError(message: String, path: String, field: Option[String] = None) extends ValidationError
  case class QueryParamError(message: String, path: String, field: Option[String] = None) extends ValidationError
  case class HeaderError(message: String, path: String, field: Option[String] = None) extends ValidationError
  case class ContentTypeError(message: String, path: String, field: Option[String] = None) extends ValidationError
  case class SchemaError(message: String, path: String, field: Option[String] = None) extends ValidationError
  
  /**
   * Convert Chez ValidationError to CaskChez ValidationError
   */
  def fromChezError(chezError: chez.ValidationError, errorType: String = "request"): ValidationError = {
    val message = chezError.toString
    val path = "/" // TODO: Extract path from chez error
    
    errorType match {
      case "body" => RequestBodyError(message, path)
      case "params" => PathParamError(message, path)
      case "query" => QueryParamError(message, path)
      case "headers" => HeaderError(message, path)
      case _ => SchemaError(message, path)
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
   * Get validated parameter value
   */
  def getParam(name: String): Option[String] = {
    validatedParams.flatMap(_.get(name))
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
 * Core validation trait that provides methods for validating different parts of HTTP requests
 */
trait RequestValidator {
  
  /**
   * Validate request body against a Chez schema
   */
  def validateBody(request: cask.Request, schema: Chez): ValidationResult = {
    Try {
      val bodyBytes = request.data.readAllBytes()
      val bodyStr = new String(bodyBytes, "UTF-8")
      val bodyJson = ujson.read(bodyStr)
      
      schema match {
        case objSchema: ObjectChez =>
          bodyJson match {
            case obj: ujson.Obj =>
              val errors = objSchema.validate(obj)
              if (errors.isEmpty) Valid else Invalid(errors.map(ValidationError.fromChezError(_, "body")))
            case _ => Invalid(List(ValidationError.RequestBodyError("Expected JSON object", "/")))
          }
        case _ => 
          // For non-object schemas, we need to validate the raw JSON value
          Valid // TODO: Implement validation for other schema types
      }
    } match {
      case Success(result) => result
      case Failure(e) => Invalid(List(ValidationError.RequestBodyError(s"Failed to parse body: ${e.getMessage}", "/")))
    }
  }
  
  /**
   * Validate path parameters against a Chez schema
   */
  def validateParams(params: Map[String, String], schema: Chez): ValidationResult = {
    schema match {
      case objSchema: ObjectChez =>
        // Convert string params to ujson.Obj for validation
        val paramsJson = ujson.Obj.from(params.map { case (k, v) => k -> ujson.Str(v) })
        val errors = objSchema.validate(paramsJson)
        if (errors.isEmpty) Valid else Invalid(errors.map(ValidationError.fromChezError(_, "params")))
      case _ => 
        // For non-object schemas, validate individual parameter
        Valid // TODO: Implement validation for other schema types
    }
  }
  
  /**
   * Validate query parameters against a Chez schema
   */
  def validateQuery(query: Map[String, scala.collection.Seq[String]], schema: Chez): ValidationResult = {
    schema match {
      case objSchema: ObjectChez =>
        // Convert string query params to ujson.Obj for validation (take first value from each seq)
        val queryJson = ujson.Obj.from(query.map { case (k, v) => k -> ujson.Str(v.headOption.getOrElse("")) })
        val errors = objSchema.validate(queryJson)
        if (errors.isEmpty) Valid else Invalid(errors.map(ValidationError.fromChezError(_, "query")))
      case _ => 
        // For non-object schemas, validate individual query parameter
        Valid // TODO: Implement validation for other schema types
    }
  }
  
  /**
   * Validate headers against a Chez schema
   */
  def validateHeaders(headers: Map[String, scala.collection.Seq[String]], schema: Chez): ValidationResult = {
    schema match {
      case objSchema: ObjectChez =>
        // Convert string headers to ujson.Obj for validation (take first value from each seq)
        val headersJson = ujson.Obj.from(headers.map { case (k, v) => k -> ujson.Str(v.headOption.getOrElse("")) })
        val errors = objSchema.validate(headersJson)
        if (errors.isEmpty) Valid else Invalid(errors.map(ValidationError.fromChezError(_, "headers")))
      case _ => 
        // For non-object schemas, validate individual header
        Valid // TODO: Implement validation for other schema types
    }
  }
  
  /**
   * Validate content type against expected content type
   */
  def validateContentType(request: cask.Request, expectedContentType: String): ValidationResult = {
    request.headers.get("content-type").flatMap(_.headOption) match {
      case Some(contentType) if contentType.startsWith(expectedContentType) => Valid
      case Some(contentType) => Invalid(List(ValidationError.ContentTypeError(
        s"Expected content type '$expectedContentType' but got '$contentType'", "/")))
      case None => Invalid(List(ValidationError.ContentTypeError(
        s"Missing content type header, expected '$expectedContentType'", "/")))
    }
  }
}

/**
 * Default implementation of RequestValidator
 */
object RequestValidator extends RequestValidator

/**
 * Schema validator that combines all validation logic
 */
object SchemaValidator {
  
  /**
   * Validate a complete request against a RouteSchema
   */
  def validateRequest(request: cask.Request, routeSchema: RouteSchema, pathParams: Map[String, String] = Map.empty): Either[List[ValidationError], ValidatedRequest] = {
    val validator = RequestValidator
    var allErrors = List.empty[ValidationError]
    
    // Collect all validation results
    val bodyValidation = routeSchema.body.map(validator.validateBody(request, _))
    val paramsValidation = routeSchema.params.map(validator.validateParams(pathParams, _))
    val queryValidation = routeSchema.query.map(validator.validateQuery(request.queryParams, _))
    val headersValidation = routeSchema.headers.map(validator.validateHeaders(request.headers, _))
    
    // Collect all errors
    bodyValidation.foreach(result => if (!result.isValid) allErrors ++= result.errors)
    paramsValidation.foreach(result => if (!result.isValid) allErrors ++= result.errors)
    queryValidation.foreach(result => if (!result.isValid) allErrors ++= result.errors)
    headersValidation.foreach(result => if (!result.isValid) allErrors ++= result.errors)
    
    // If there are errors, return them
    if (allErrors.nonEmpty) {
      Left(allErrors)
    } else {
      // Create validated request with parsed data
      val validatedBody = routeSchema.body.map { _ =>
        Try(ujson.read(new String(request.data.readAllBytes(), "UTF-8"))).toOption
      }.flatten
      
      val validatedRequest = ValidatedRequest(
        original = request,
        validatedBody = validatedBody,
        validatedParams = if (pathParams.nonEmpty) Some(pathParams) else None,
        validatedQuery = if (request.queryParams.nonEmpty) Some(request.queryParams.map { case (k, v) => k -> v.headOption.getOrElse("") }) else None,
        validatedHeaders = if (request.headers.nonEmpty) Some(request.headers.map { case (k, v) => k -> v.headOption.getOrElse("") }) else None
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