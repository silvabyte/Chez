package boogieloops.web.openapi.models

import upickle.default.*
import boogieloops.web.openapi.config.*

/**
 * Components Object - holds reusable objects for different aspects of the OAS
 */
case class ComponentsObject(
    schemas: Option[Map[String, ujson.Value]] = None,
    responses: Option[Map[String, ResponseObject]] = None,
    parameters: Option[Map[String, ParameterObject]] = None,
    examples: Option[Map[String, ExampleObject]] = None,
    requestBodies: Option[Map[String, RequestBodyObject]] = None,
    headers: Option[Map[String, HeaderObject]] = None,
    securitySchemes: Option[Map[String, SecuritySchemeObject]] = None,
    links: Option[Map[String, LinkObject]] = None,
    callbacks: Option[Map[String, CallbackObject]] = None,
    pathItems: Option[Map[String, PathItemObject]] = None // OpenAPI 3.1.1 addition
) derives ReadWriter

/**
 * Schema Object - OpenAPI 3.1.1 uses JSON Schema 2020-12 directly
 */
case class SchemaObject(
    // JSON Schema 2020-12 content (from BoogieLoops)
    schema: ujson.Value,

    // OpenAPI 3.1.1 specific annotations
    example: Option[ujson.Value] = None,
    examples: Option[List[ujson.Value]] = None,
    externalDocs: Option[ExternalDocumentationObject] = None,
    discriminator: Option[DiscriminatorObject] = None,
    xml: Option[XMLObject] = None
) derives ReadWriter

/**
 * Media Type Object - provides schema and examples for the media type
 */
case class MediaTypeObject(
    schema: Option[ujson.Value] = None,
    example: Option[ujson.Value] = None,
    examples: Option[Map[String, ExampleObject]] = None, // 3.1.1 enhancement
    encoding: Option[Map[String, EncodingObject]] = None
) derives ReadWriter

/**
 * Example Object - OpenAPI 3.1.1 enhanced examples
 */
case class ExampleObject(
    summary: Option[String] = None,
    description: Option[String] = None,
    value: Option[ujson.Value] = None,
    externalValue: Option[String] = None
) derives ReadWriter

/**
 * Header Object - describes a single header
 */
case class HeaderObject(
    description: Option[String] = None,
    required: Option[Boolean] = None,
    deprecated: Option[Boolean] = None,
    allowEmptyValue: Option[Boolean] = None,
    // Schema-based headers
    style: Option[String] = None,
    explode: Option[Boolean] = None,
    allowReserved: Option[Boolean] = None,
    schema: Option[ujson.Value] = None,
    example: Option[ujson.Value] = None,
    examples: Option[Map[String, ExampleObject]] = None,
    // Content-based headers
    content: Option[Map[String, MediaTypeObject]] = None
) derives ReadWriter

/**
 * Link Object - represents a possible design-time link
 */
case class LinkObject(
    operationRef: Option[String] = None,
    operationId: Option[String] = None,
    parameters: Option[Map[String, ujson.Value]] = None,
    requestBody: Option[ujson.Value] = None,
    description: Option[String] = None,
    server: Option[ServerObject] = None
) derives ReadWriter

/**
 * Encoding Object - defines serialization strategy for application/x-www-form-urlencoded
 */
case class EncodingObject(
    contentType: Option[String] = None,
    headers: Option[Map[String, HeaderObject]] = None,
    style: Option[String] = None,
    explode: Option[Boolean] = None,
    allowReserved: Option[Boolean] = None
) derives ReadWriter

/**
 * Discriminator Object - used for polymorphism
 */
case class DiscriminatorObject(
    propertyName: String,
    mapping: Option[Map[String, String]] = None
) derives ReadWriter

/**
 * XML Object - metadata for XML serialization
 */
case class XMLObject(
    name: Option[String] = None,
    namespace: Option[String] = None,
    prefix: Option[String] = None,
    attribute: Option[Boolean] = None,
    wrapped: Option[Boolean] = None
) derives ReadWriter
