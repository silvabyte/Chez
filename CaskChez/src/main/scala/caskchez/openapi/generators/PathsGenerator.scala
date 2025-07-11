package caskchez.openapi.generators

import caskchez.*
import caskchez.openapi.models.*
import caskchez.openapi.config.*
import _root_.chez.Chez
import upickle.default.*

/**
 * Generates OpenAPI Paths Object from registered routes
 */
object PathsGenerator {
  
  /**
   * Convert all registered routes to OpenAPI Paths Object
   */
  def convertPathsFromRegistry(
    allRoutes: Map[String, RouteSchema], 
    config: OpenAPIConfig
  ): PathsObject = {
    val pathGroups = allRoutes.groupBy { case (methodPath, _) => 
      extractPath(methodPath) 
    }
    
    PathsObject(
      paths = pathGroups.map { case (path, methodRoutes) =>
        path -> createPathItemObject(path, methodRoutes, config)
      }
    )
  }
  
  /**
   * Create PathItemObject for a single path with all its operations
   */
  private def createPathItemObject(
    path: String, 
    methodRoutes: Map[String, RouteSchema],
    config: OpenAPIConfig
  ): PathItemObject = {
    val operations = methodRoutes.map { case (methodPath, schema) =>
      val method = extractMethod(methodPath)
      method.toLowerCase -> convertToOperation(method, path, schema, config)
    }
    
    PathItemObject(
      summary = inferPathSummary(methodRoutes),
      description = inferPathDescription(methodRoutes),
      parameters = extractCommonParameters(path, methodRoutes),
      get = operations.get("get"),
      put = operations.get("put"),
      post = operations.get("post"),
      delete = operations.get("delete"),
      options = operations.get("options"),
      head = operations.get("head"),
      patch = operations.get("patch"),
      trace = operations.get("trace")
    )
  }
  
  /**
   * Convert RouteSchema to OpenAPI Operation Object
   */
  private def convertToOperation(
    method: String, 
    path: String, 
    schema: RouteSchema, 
    config: OpenAPIConfig
  ): OperationObject = {
    OperationObject(
      tags = if (schema.tags.nonEmpty) Some(schema.tags) else None,
      summary = schema.summary,
      description = schema.description,
      operationId = if (config.generateOperationIds) 
        Some(generateOperationId(method, path)) else None,
      parameters = convertParameters(path, schema),
      requestBody = schema.body.map(convertToRequestBody),
      responses = convertResponses(schema.responses),
      security = convertSecurity(schema.security),
      deprecated = None  // Could be extracted from schema metadata
    )
  }
  
  /**
   * Generate operation ID from method and path
   */
  private def generateOperationId(method: String, path: String): String = {
    val cleanPath = path.replaceAll("[{}]", "")
      .split("/")
      .filter(_.nonEmpty)
      .map(_.toLowerCase.capitalize)
      .mkString("")
    
    s"${method.toLowerCase}$cleanPath"
  }
  
  /**
   * Convert RouteSchema parameters to OpenAPI parameters
   */
  private def convertParameters(path: String, schema: RouteSchema): Option[List[ParameterObject]] = {
    val pathParams = extractPathParameters(path, schema.params)
    val queryParams = schema.query.map(convertQueryParameters).getOrElse(List.empty)
    val headerParams = schema.headers.map(convertHeaderParameters).getOrElse(List.empty)
    
    val allParams = pathParams ++ queryParams ++ headerParams
    if (allParams.nonEmpty) Some(allParams) else None
  }
  
  /**
   * Extract path parameters from URL template
   */
  private def extractPathParameters(path: String, paramsSchema: Option[Chez]): List[ParameterObject] = {
    val pattern = """\{([^}]+)\}""".r
    val paramNames = pattern.findAllMatchIn(path).map(_.group(1)).toList
    
    paramNames.map(name => 
      ParameterObject(
        name = name,
        in = "path",
        required = Some(true),
        description = Some(s"Path parameter: $name"),
        schema = paramsSchema.map(_.toJsonSchema)
      )
    )
  }
  
  /**
   * Convert query schema to parameter objects
   */
  private def convertQueryParameters(querySchema: Chez): List[ParameterObject] = {
    // For now, create a single query parameter
    // Could be enhanced to extract individual properties from object schema
    List(
      ParameterObject(
        name = "query",
        in = "query", 
        description = Some("Query parameters"),
        schema = Some(querySchema.toJsonSchema)
      )
    )
  }
  
  /**
   * Convert headers schema to parameter objects
   */
  private def convertHeaderParameters(headersSchema: Chez): List[ParameterObject] = {
    // Similar to query parameters - could be enhanced
    List(
      ParameterObject(
        name = "headers",
        in = "header",
        description = Some("Custom headers"),
        schema = Some(headersSchema.toJsonSchema)
      )
    )
  }
  
  /**
   * Convert body schema to RequestBody object
   */
  private def convertToRequestBody(bodySchema: Chez): RequestBodyObject = {
    RequestBodyObject(
      description = Some("Request body"),
      content = Map(
        "application/json" -> MediaTypeObject(
          schema = Some(bodySchema.toJsonSchema)
        )
      ),
      required = Some(true)
    )
  }
  
  /**
   * Convert RouteSchema responses to OpenAPI Responses Object
   */
  private def convertResponses(responses: Map[StatusCode, ApiResponse]): ResponsesObject = {
    val convertedResponses = responses.map { case (code, apiResponse) =>
      code.toString -> ResponseObject(
        description = apiResponse.description,
        content = Some(Map(
          "application/json" -> MediaTypeObject(
            schema = Some(apiResponse.schema.toJsonSchema)
          )
        )),
        headers = apiResponse.headers.map(convertHeadersSchema)
      )
    }
    
    ResponsesObject(
      responses = convertedResponses
    )
  }
  
  /**
   * Convert headers schema to header objects
   */
  private def convertHeadersSchema(headersSchema: Chez): Map[String, HeaderObject] = {
    // For now, create a generic header
    // Could be enhanced to extract individual header definitions
    Map(
      "X-Custom-Header" -> HeaderObject(
        description = Some("Custom response header"),
        schema = Some(headersSchema.toJsonSchema)
      )
    )
  }
  
  /**
   * Convert security requirements
   */
  private def convertSecurity(security: List[SecurityRequirement]): Option[List[SecurityRequirementObject]] = {
    if (security.nonEmpty) {
      Some(security.map(SecurityGenerator.convertSecurityRequirement))
    } else None
  }
  
  // Helper methods for path processing
  
  private def extractPath(methodPath: String): String = {
    methodPath.split(":").lastOption.getOrElse(methodPath)
  }
  
  private def extractMethod(methodPath: String): String = {
    methodPath.split(":").headOption.getOrElse("GET")
  }
  
  private def inferPathSummary(methodRoutes: Map[String, RouteSchema]): Option[String] = {
    methodRoutes.values.flatMap(_.summary).headOption
  }
  
  private def inferPathDescription(methodRoutes: Map[String, RouteSchema]): Option[String] = {
    methodRoutes.values.flatMap(_.description).headOption
  }
  
  private def extractCommonParameters(path: String, methodRoutes: Map[String, RouteSchema]): Option[List[ParameterObject]] = {
    // Could extract parameters common to all operations on this path
    None
  }
}