package caskchez.openapi.models

import upickle.default.*
import caskchez.openapi.config.*

/**
 * OpenAPI 3.1.1 Root Document Structure
 */
case class OpenAPIDocument(
    openapi: String,
    info: InfoObject,
    jsonSchemaDialect: String,
    servers: Option[List[ServerObject]] = None,
    paths: Option[PathsObject] = None,
    webhooks: Option[Map[String, PathItemObject]] = None,
    components: Option[ComponentsObject] = None,
    security: Option[List[SecurityRequirementObject]] = None,
    tags: Option[List[TagObject]] = None,
    externalDocs: Option[ExternalDocumentationObject] = None
) derives ReadWriter

/**
 * Info Object (OpenAPI 3.1.1)
 */
case class InfoObject(
    title: String,
    summary: Option[String] = None, // OpenAPI 3.1.1 feature
    description: String,
    version: String,
    termsOfService: Option[String] = None,
    contact: Option[ContactObject] = None,
    license: Option[LicenseObject] = None
) derives ReadWriter

/**
 * Paths Object - container for all path items
 */
case class PathsObject(
    paths: Map[String, PathItemObject]
) derives ReadWriter

/**
 * Tag Object for categorizing operations
 */
case class TagObject(
    name: String,
    description: Option[String] = None,
    externalDocs: Option[ExternalDocumentationObject] = None
) derives ReadWriter
