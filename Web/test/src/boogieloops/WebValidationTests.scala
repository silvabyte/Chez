package boogieloops.web

import utest.*
import _root_.boogieloops.schema.*
import _root_.boogieloops.schema.{Schema as bl}
import upickle.default.*
import java.io.ByteArrayInputStream

object WebValidationTests extends TestSuite {

  // Mock cask.Request for testing
  class MockRequest(
      bodyContent: String = "",
      queryParamsMap: Map[String, Seq[String]] = Map.empty,
      headersMap: Map[String, Seq[String]] = Map.empty
  ) extends cask.Request(null, Seq.empty, Map.empty) {
    override lazy val data = new ByteArrayInputStream(bodyContent.getBytes("UTF-8"))
    override lazy val queryParams = queryParamsMap
    override lazy val headers = headersMap
    override lazy val cookies = Map.empty[String, cask.Cookie]
  }

  val tests = Tests {
    test("ValidationError conversion") {
      test("convert Schema ValidationError to Web ValidationError") {
        val schemaError = boogieloops.schema.ValidationError.TypeMismatch("string", "number", "/test")
        val webError = boogieloops.web.ValidationError.fromSchemaError(schemaError, "body")

        assert(webError.isInstanceOf[boogieloops.web.ValidationError.RequestBodyError])
        assert(webError.message.contains("Type mismatch"))
        assert(webError.path == "/test")
      }

      test("convert different error types with different contexts") {
        val schemaError = boogieloops.schema.ValidationError.MissingField("name", "/user")

        val bodyError = boogieloops.web.ValidationError.fromSchemaError(schemaError, "body")
        assert(bodyError.isInstanceOf[boogieloops.web.ValidationError.RequestBodyError])

        val queryError = boogieloops.web.ValidationError.fromSchemaError(schemaError, "query")
        assert(queryError.isInstanceOf[boogieloops.web.ValidationError.QueryParamError])

        val paramError = boogieloops.web.ValidationError.fromSchemaError(schemaError, "params")
        assert(paramError.isInstanceOf[boogieloops.web.ValidationError.PathParamError])

        val headerError = boogieloops.web.ValidationError.fromSchemaError(schemaError, "headers")
        assert(headerError.isInstanceOf[boogieloops.web.ValidationError.HeaderError])
      }
    }

    test("Request body validation") {
      test("valid JSON body validates against schema") {
        val bodySchema = bl.Object(
          properties = Map(
            "name" -> bl.String(minLength = Some(1)),
            "age" -> bl.Integer(minimum = Some(0))
          ),
          required = Set("name", "age")
        )

        val validBody = """{"name": "John", "age": 25}"""
        val request = new MockRequest(bodyContent = validBody)

        val result = ValidationHelpers.validateRequestBody(request, bodySchema)
        assert(result.isRight)

        val bodyJson = result.getOrElse(throw new Exception("Expected Right"))
        assert(bodyJson("name").str == "John")
        assert(bodyJson("age").num == 25)
      }

      test("invalid JSON body fails validation") {
        val bodySchema = bl.Object(
          properties = Map("name" -> bl.String(minLength = Some(1))),
          required = Set("name")
        )

        val invalidBody = """{"name": ""}""" // Empty name fails minLength
        val request = new MockRequest(bodyContent = invalidBody)

        val result = ValidationHelpers.validateRequestBody(request, bodySchema)
        assert(result.isLeft)

        val errors = result.left.getOrElse(throw new Exception("Expected Left"))
        assert(errors.nonEmpty)
        assert(errors.head.isInstanceOf[boogieloops.web.ValidationError.RequestBodyError])
      }

      test("malformed JSON body fails with parse error") {
        val bodySchema = bl.Object(properties = Map("name" -> bl.String()))

        val malformedBody = """{"name": invalid json}"""
        val request = new MockRequest(bodyContent = malformedBody)

        val result = ValidationHelpers.validateRequestBody(request, bodySchema)
        assert(result.isLeft)

        val errors = result.left.getOrElse(throw new Exception("Expected Left"))
        assert(errors.head.message.contains("Failed to parse JSON body"))
      }
    }

    test("Query parameter validation") {
      test("valid query parameters validate against schema") {
        val querySchema = bl.Object(
          properties = Map(
            "page" -> bl.Integer(minimum = Some(1)),
            "limit" -> bl.Integer(minimum = Some(1), maximum = Some(100))
          ),
          required = Set("page")
        )

        val queryParams = Map(
          "page" -> Seq("2"),
          "limit" -> Seq("10")
        )
        val request = new MockRequest(queryParamsMap = queryParams)

        val result = ValidationHelpers.validateQueryParams(request, querySchema)
        assert(result.isRight)

        val params = result.getOrElse(throw new Exception("Expected Right"))
        assert(params("page") == ujson.Num(2))
        assert(params("limit") == ujson.Num(10))
      }

      test("invalid query parameters fail validation") {
        val querySchema = bl.Object(
          properties = Map("page" -> bl.Integer(minimum = Some(1))),
          required = Set("page")
        )

        val queryParams = Map("page" -> Seq("0")) // Below minimum
        val request = new MockRequest(queryParamsMap = queryParams)

        val result = ValidationHelpers.validateQueryParams(request, querySchema)
        assert(result.isLeft)

        val errors = result.left.getOrElse(throw new Exception("Expected Left"))
        assert(errors.nonEmpty)
        assert(errors.head.isInstanceOf[boogieloops.web.ValidationError.QueryParamError])
      }

      test("type conversion for query parameters") {
        val querySchema = bl.Object(
          properties = Map(
            "enabled" -> bl.Boolean(),
            "count" -> bl.Integer(),
            "name" -> bl.String()
          )
        )

        val queryParams = Map(
          "enabled" -> Seq("true"),
          "count" -> Seq("42"),
          "name" -> Seq("test")
        )
        val request = new MockRequest(queryParamsMap = queryParams)

        val result = ValidationHelpers.validateQueryParams(request, querySchema)
        assert(result.isRight)

        val params = result.getOrElse(throw new Exception("Expected Right"))
        assert(params("enabled") == ujson.Bool(true))
        assert(params("count") == ujson.Num(42))
        assert(params("name") == ujson.Str("test"))
      }
    }

    test("Path parameter validation") {
      test("valid path parameters validate against schema") {
        val paramsSchema = bl.Object(
          properties = Map(
            "id" -> bl.Integer(minimum = Some(1)),
            "category" -> bl.String(minLength = Some(1))
          ),
          required = Set("id", "category")
        )

        val pathParams = Map(
          "id" -> "123",
          "category" -> "books"
        )

        val result = ValidationHelpers.validatePathParams(pathParams, paramsSchema)
        assert(result.isRight)

        val params = result.getOrElse(throw new Exception("Expected Right"))
        assert(params("id") == ujson.Num(123))
        assert(params("category") == ujson.Str("books"))
      }

      test("invalid path parameters fail validation") {
        val paramsSchema = bl.Object(
          properties = Map("id" -> bl.Integer(minimum = Some(1))),
          required = Set("id")
        )

        val pathParams = Map("id" -> "0") // Below minimum

        val result = ValidationHelpers.validatePathParams(pathParams, paramsSchema)
        assert(result.isLeft)

        val errors = result.left.getOrElse(throw new Exception("Expected Left"))
        assert(errors.nonEmpty)
        assert(errors.head.isInstanceOf[boogieloops.web.ValidationError.PathParamError])
      }
    }

    test("Header validation") {
      test("valid headers validate against schema") {
        val headersSchema = bl.Object(
          properties = Map(
            "Authorization" -> bl.String(pattern = Some("Bearer .*")),
            "Content-Type" -> bl.String(const = Some("application/json"))
          ),
          required = Set("Authorization")
        )

        val headers = Map(
          "Authorization" -> Seq("Bearer token123"),
          "Content-Type" -> Seq("application/json")
        )
        val request = new MockRequest(headersMap = headers)

        val result = ValidationHelpers.validateHeaders(request, headersSchema)
        assert(result.isRight)

        val validatedHeaders = result.getOrElse(throw new Exception("Expected Right"))
        assert(validatedHeaders("Authorization") == ujson.Str("Bearer token123"))
        assert(validatedHeaders("Content-Type") == ujson.Str("application/json"))
      }

      test("invalid headers fail validation") {
        val headersSchema = bl.Object(
          properties = Map("Authorization" -> bl.String(pattern = Some("Bearer .*"))),
          required = Set("Authorization")
        )

        val headers = Map("Authorization" -> Seq("InvalidToken")) // Doesn't match pattern
        val request = new MockRequest(headersMap = headers)

        val result = ValidationHelpers.validateHeaders(request, headersSchema)
        assert(result.isLeft)

        val errors = result.left.getOrElse(throw new Exception("Expected Left"))
        assert(errors.nonEmpty)
        assert(errors.head.isInstanceOf[boogieloops.web.ValidationError.HeaderError])
      }
    }

    test("Complete request validation") {
      test("valid complete request validates successfully") {
        val routeSchema = RouteSchema(
          body = Some(bl.Object(
            properties = Map("name" -> bl.String()),
            required = Set("name")
          )),
          query = Some(bl.Object(
            properties = Map("page" -> bl.Integer(minimum = Some(1)))
          )),
          params = Some(bl.Object(
            properties = Map("id" -> bl.Integer()),
            required = Set("id")
          )),
          headers = Some(bl.Object(
            properties = Map("Content-Type" -> bl.String())
          ))
        )

        val request = new MockRequest(
          bodyContent = """{"name": "John"}""",
          queryParamsMap = Map("page" -> Seq("2")),
          headersMap = Map("Content-Type" -> Seq("application/json"))
        )
        val pathParams = Map("id" -> "123")

        val result = SchemaValidator.validateRequest(request, routeSchema, pathParams)
        assert(result.isRight)

        val validatedRequest = result.getOrElse(throw new Exception("Expected Right"))
        assert(validatedRequest.validatedBody.isDefined)
        assert(validatedRequest.validatedQuery.isDefined)
        assert(validatedRequest.validatedParams.isDefined)
        assert(validatedRequest.validatedHeaders.isDefined)
      }

      test("invalid request fails with multiple errors") {
        val routeSchema = RouteSchema(
          body = Some(bl.Object(
            properties = Map("name" -> bl.String(minLength = Some(1))),
            required = Set("name")
          )),
          query = Some(bl.Object(
            properties = Map("page" -> bl.Integer(minimum = Some(1))),
            required = Set("page")
          ))
        )

        val request = new MockRequest(
          bodyContent = """{"name": ""}""", // Empty name fails validation
          queryParamsMap = Map("page" -> Seq("0")) // Below minimum
        )

        val result = SchemaValidator.validateRequest(request, routeSchema)
        assert(result.isLeft)

        val errors = result.left.getOrElse(throw new Exception("Expected Left"))
        assert(errors.length >= 2) // Should have errors from both body and query validation

        val hasBodyError = errors.exists(_.isInstanceOf[boogieloops.web.ValidationError.RequestBodyError])
        val hasQueryError = errors.exists(_.isInstanceOf[boogieloops.web.ValidationError.QueryParamError])
        assert(hasBodyError)
        assert(hasQueryError)
      }

      test("partial validation when only some schemas are provided") {
        val routeSchema = RouteSchema(
          body = Some(bl.Object(
            properties = Map("name" -> bl.String()),
            required = Set("name")
          ))
          // No query, params, or headers schemas
        )

        val request = new MockRequest(
          bodyContent = """{"name": "John"}""",
          queryParamsMap = Map("anything" -> Seq("value")), // Should be ignored
          headersMap = Map("Custom" -> Seq("header")) // Should be ignored
        )

        val result = SchemaValidator.validateRequest(request, routeSchema)
        assert(result.isRight)

        val validatedRequest = result.getOrElse(throw new Exception("Expected Right"))
        assert(validatedRequest.validatedBody.isDefined)
        assert(validatedRequest.validatedQuery.isEmpty) // No schema provided
        assert(validatedRequest.validatedParams.isEmpty) // No schema provided
        assert(validatedRequest.validatedHeaders.isEmpty) // No schema provided
      }
    }

    test("ValidatedRequest data access") {
      test("getBody extracts typed data correctly") {
        case class User(name: String, age: Int)
        implicit val userRW: ReadWriter[User] = macroRW

        val validatedRequest = ValidatedRequest(
          original = new MockRequest(),
          validatedBody = Some(ujson.Obj("name" -> ujson.Str("John"), "age" -> ujson.Num(25)))
        )

        val userResult = validatedRequest.getBody[User]
        assert(userResult.isRight)

        val user = userResult.getOrElse(throw new Exception("Expected Right"))
        assert(user.name == "John")
        assert(user.age == 25)
      }

      test("getParam extracts path parameter") {
        val validatedRequest = ValidatedRequest(
          original = new MockRequest(),
          validatedParams = Some(Map("id" -> "123", "category" -> "books"))
        )

        assert(validatedRequest.getParam("id").contains("123"))
        assert(validatedRequest.getParam("category").contains("books"))
        assert(validatedRequest.getParam("nonexistent").isEmpty)
      }

      test("getQueryParam extracts query parameter") {
        val validatedRequest = ValidatedRequest(
          original = new MockRequest(),
          validatedQuery = Some(Map("page" -> "2", "limit" -> "10"))
        )

        assert(validatedRequest.getQueryParam("page").contains("2"))
        assert(validatedRequest.getQueryParam("limit").contains("10"))
        assert(validatedRequest.getQueryParam("nonexistent").isEmpty)
      }

      test("getHeader extracts header value") {
        val validatedRequest = ValidatedRequest(
          original = new MockRequest(),
          validatedHeaders =
            Some(Map("Authorization" -> "Bearer token", "Content-Type" -> "application/json"))
        )

        assert(validatedRequest.getHeader("Authorization").contains("Bearer token"))
        assert(validatedRequest.getHeader("Content-Type").contains("application/json"))
        assert(validatedRequest.getHeader("nonexistent").isEmpty)
      }
    }

    test("Error response generation") {
      test("createErrorResponse generates proper JSON response") {
        val errors = List(
          boogieloops.web.ValidationError.RequestBodyError("Invalid name", "/name", Some("name")),
          boogieloops.web.ValidationError.QueryParamError("Invalid page", "/page", Some("page"))
        )

        val response = SchemaValidator.createErrorResponse(errors, 400)

        assert(response.statusCode == 400)
        assert(response.headers.contains("Content-Type" -> "application/json"))

        val responseData = response.data
        assert(responseData("error").str == "Validation failed")
        assert(responseData("details").arr.length == 2)

        val firstDetail = responseData("details")(0)
        assert(firstDetail("message").str == "Invalid name")
        assert(firstDetail("path").str == "/name")
        assert(firstDetail("field").str == "name")
      }
    }

    test("String to JSON type conversion") {
      test("converts various string types correctly") {
        // This tests the private convertStringToJson method indirectly through query param validation
        val querySchema = bl.Object(
          properties = Map(
            "bool_true" -> bl.Boolean(),
            "bool_false" -> bl.Boolean(),
            "integer" -> bl.Integer(),
            "number" -> bl.Number(),
            "string" -> bl.String(),
            "empty" -> bl.String()
          )
        )

        val queryParams = Map(
          "bool_true" -> Seq("true"),
          "bool_false" -> Seq("false"),
          "integer" -> Seq("42"),
          "number" -> Seq("3.14"),
          "string" -> Seq("hello"),
          "empty" -> Seq("")
        )
        val request = new MockRequest(queryParamsMap = queryParams)

        val result = ValidationHelpers.validateQueryParams(request, querySchema)
        assert(result.isRight)

        val params = result.getOrElse(throw new Exception("Expected Right"))
        assert(params("bool_true") == ujson.Bool(true))
        assert(params("bool_false") == ujson.Bool(false))
        assert(params("integer") == ujson.Num(42))
        assert(params("number") == ujson.Num(3.14))
        assert(params("string") == ujson.Str("hello"))
        assert(params("empty") == ujson.Str(""))
      }
    }
  }
}
