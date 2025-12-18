package boogieloops.web.openapi.models

import upickle.default.*

//TODO: add support for OAuth 2.1 dynamic client registration

/**
 * Security Scheme Object - defines security schemes that can be used by operations
 */
case class SecuritySchemeObject(
    `type`: String, // "apiKey", "http", "mutualTLS", "oauth2", "openIdConnect"
    description: Option[String] = None,
    // For apiKey
    name: Option[String] = None,
    in: Option[String] = None, // "query", "header", "cookie"
    // For http
    scheme: Option[String] = None, // "basic", "bearer", etc.
    bearerFormat: Option[String] = None,
    // For oauth2
    flows: Option[OAuthFlowsObject] = None,
    // For openIdConnect
    openIdConnectUrl: Option[String] = None
) derives ReadWriter

/**
 * OAuth Flows Object - configuration details for supported OAuth Flow types
 */
case class OAuthFlowsObject(
    `implicit`: Option[OAuthFlowObject] = None,
    password: Option[OAuthFlowObject] = None,
    clientCredentials: Option[OAuthFlowObject] = None,
    authorizationCode: Option[OAuthFlowObject] = None
) derives ReadWriter

/**
 * OAuth Flow Object - configuration for a supported OAuth Flow
 */
case class OAuthFlowObject(
    authorizationUrl: Option[String] = None, // Required for implicit, authorizationCode
    tokenUrl: Option[String] = None, // Required for password, clientCredentials, authorizationCode
    refreshUrl: Option[String] = None,
    scopes: Map[String, String] = Map.empty
) derives ReadWriter

/**
 * Security Requirement Object - lists required security schemes for operation
 * OpenAPI spec requires this to serialize as a flat map, not wrapped in a "requirements" field
 */
case class SecurityRequirementObject(
    requirements: Map[String, List[String]] = Map.empty
)

object SecurityRequirementObject {
  given ReadWriter[SecurityRequirementObject] =
    readwriter[Map[String, List[String]]].bimap(
      _.requirements,
      SecurityRequirementObject(_)
    )
}
