package caskchez.openapi.generators

import caskchez.*
import caskchez.openapi.models.*
import upickle.default.*

/**
 * Generates OpenAPI security schemes and requirements from CaskChez SecurityRequirement
 */
object SecurityGenerator {

  /**
   * Extract security schemes from security requirements
   */
  def extractSecuritySchemes(securityRequirements: List[SecurityRequirement]): Map[String, SecuritySchemeObject] = {
    securityRequirements.flatMap(convertToSecurityScheme).toMap
  }

  /**
   * Convert CaskChez SecurityRequirement to OpenAPI SecurityRequirementObject
   */
  def convertSecurityRequirement(requirement: SecurityRequirement): SecurityRequirementObject = {
    requirement match {
      case SecurityRequirement.ApiKey(name, _) =>
        SecurityRequirementObject(Map(name -> List.empty))
      case SecurityRequirement.Bearer(_) =>
        SecurityRequirementObject(Map("bearerAuth" -> List.empty))
      case SecurityRequirement.Basic() =>
        SecurityRequirementObject(Map("basicAuth" -> List.empty))
      case SecurityRequirement.OAuth2(flows) =>
        SecurityRequirementObject(Map("oauth2" -> flows.keys.toList))
    }
  }

  /**
   * Convert CaskChez SecurityRequirement to OpenAPI SecuritySchemeObject
   */
  private def convertToSecurityScheme(requirement: SecurityRequirement): Option[(String, SecuritySchemeObject)] = {
    requirement match {
      case SecurityRequirement.ApiKey(name, in) =>
        Some(
          name -> SecuritySchemeObject(
            `type` = "apiKey",
            description = Some(s"API Key authentication via $in"),
            name = Some(name),
            in = Some(in)
          )
        )

      case SecurityRequirement.Bearer(format) =>
        Some(
          "bearerAuth" -> SecuritySchemeObject(
            `type` = "http",
            description = Some("Bearer token authentication"),
            scheme = Some("bearer"),
            bearerFormat = Some(format)
          )
        )

      case SecurityRequirement.Basic() =>
        Some(
          "basicAuth" -> SecuritySchemeObject(
            `type` = "http",
            description = Some("Basic HTTP authentication"),
            scheme = Some("basic")
          )
        )

      // TODO: add support for OAuth 2.1 dynamic client registration
      case SecurityRequirement.OAuth2(flows) =>
        Some(
          "oauth2" -> SecuritySchemeObject(
            `type` = "oauth2",
            description = Some("OAuth2 authentication"),
            flows = Some(convertOAuthFlows(flows))
          )
        )
    }
  }

  /**
   * Convert OAuth2 flows from CaskChez to OpenAPI format
   */
  private def convertOAuthFlows(flows: Map[String, String]): OAuthFlowsObject = {
    // Basic OAuth flows conversion - could be enhanced based on actual flow definitions
    OAuthFlowsObject(
      authorizationCode = flows
        .get("authorizationCode")
        .map(_ =>
          OAuthFlowObject(
            authorizationUrl = Some("https://example.com/oauth/authorize"),
            tokenUrl = Some("https://example.com/oauth/token"),
            scopes = flows
          )
        ),
      clientCredentials = flows
        .get("clientCredentials")
        .map(_ =>
          OAuthFlowObject(
            tokenUrl = Some("https://example.com/oauth/token"),
            scopes = flows
          )
        )
    )
  }
}
