package caskchez.openapi.config

import upickle.default.*

/**
 * Configuration for OpenAPI 3.1.1 document generation
 */
case class OpenAPIConfig(
  // Info Object (OpenAPI 3.1.1)
  title: String = "API Documentation",
  description: String = "Generated from CaskChez route schemas", 
  summary: Option[String] = Some("CaskChez Auto-Generated API"),
  version: String = "1.0.0",
  termsOfService: Option[String] = None,
  contact: Option[ContactObject] = None,
  license: Option[LicenseObject] = None,
  
  // Root level fields
  jsonSchemaDialect: String = "https://json-schema.org/draft/2020-12/schema",
  servers: List[ServerObject] = List.empty,
  externalDocs: Option[ExternalDocumentationObject] = None,
  
  // Generation options
  includeWebhooks: Boolean = false,
  extractComponents: Boolean = true,
  generateOperationIds: Boolean = true
) derives ReadWriter

case class ServerObject(
  url: String, 
  description: Option[String] = None,
  variables: Map[String, ServerVariableObject] = Map.empty
) derives ReadWriter

case class ServerVariableObject(
  `enum`: Option[List[String]] = None,
  default: String,
  description: Option[String] = None
) derives ReadWriter

case class ContactObject(
  name: Option[String] = None, 
  url: Option[String] = None, 
  email: Option[String] = None
) derives ReadWriter

case class LicenseObject(
  name: String, 
  identifier: Option[String] = None,  // OpenAPI 3.1.1 addition
  url: Option[String] = None
) derives ReadWriter

case class ExternalDocumentationObject(
  description: Option[String] = None,
  url: String
) derives ReadWriter