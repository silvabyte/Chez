package caskchez.openapi.models

import upickle.default.*
import caskchez.openapi.config.*

/**
 * Path Item Object - describes operations available on a single path
 */
case class PathItemObject(
    summary: Option[String] = None,
    description: Option[String] = None,
    get: Option[OperationObject] = None,
    put: Option[OperationObject] = None,
    post: Option[OperationObject] = None,
    delete: Option[OperationObject] = None,
    options: Option[OperationObject] = None,
    head: Option[OperationObject] = None,
    patch: Option[OperationObject] = None,
    trace: Option[OperationObject] = None,
    servers: Option[List[ServerObject]] = None,
    parameters: Option[List[ParameterObject]] = None
) derives ReadWriter

/**
 * Operation Object - describes a single API operation on a path
 */
case class OperationObject(
    tags: Option[List[String]] = None,
    summary: Option[String] = None,
    description: Option[String] = None,
    externalDocs: Option[ExternalDocumentationObject] = None,
    operationId: Option[String] = None,
    parameters: Option[List[ParameterObject]] = None,
    requestBody: Option[RequestBodyObject] = None,
    responses: ResponsesObject,
    callbacks: Option[Map[String, CallbackObject]] = None, // 3.1.1 webhooks support
    deprecated: Option[Boolean] = None,
    security: Option[List[SecurityRequirementObject]] = None,
    servers: Option[List[ServerObject]] = None
) derives ReadWriter

/**
 * Parameter Object - describes a single operation parameter
 */
case class ParameterObject(
    name: String,
    in: String, // "query", "header", "path", "cookie"
    description: Option[String] = None,
    required: Option[Boolean] = None,
    deprecated: Option[Boolean] = None,
    allowEmptyValue: Option[Boolean] = None,
    // Schema-based parameters
    style: Option[String] = None,
    explode: Option[Boolean] = None,
    allowReserved: Option[Boolean] = None,
    schema: Option[ujson.Value] = None,
    example: Option[ujson.Value] = None,
    examples: Option[Map[String, ExampleObject]] = None,
    // Content-based parameters
    content: Option[Map[String, MediaTypeObject]] = None
) derives ReadWriter

/**
 * Request Body Object - describes a single request body
 */
case class RequestBodyObject(
    description: Option[String] = None,
    content: Map[String, MediaTypeObject],
    required: Option[Boolean] = None
) derives ReadWriter

/**
 * Responses Object - container for the expected responses of an operation
 */
case class ResponsesObject(
    default: Option[ResponseObject] = None,
    responses: Map[String, ResponseObject] = Map.empty // HTTP status codes as keys
) derives ReadWriter

/**
 * Response Object - describes a single response from an API Operation
 */
case class ResponseObject(
    description: String,
    headers: Option[Map[String, HeaderObject]] = None,
    content: Option[Map[String, MediaTypeObject]] = None,
    links: Option[Map[String, LinkObject]] = None
) derives ReadWriter

/**
 * Callback Object - A map of possible out-of band callbacks
 */
case class CallbackObject(
    callbacks: Map[String, PathItemObject] = Map.empty
) derives ReadWriter
