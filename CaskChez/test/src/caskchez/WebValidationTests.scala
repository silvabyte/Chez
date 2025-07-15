package caskchez

import utest.*
import _root_.chez.*
import _root_.chez.primitives.*
import _root_.chez.complex.*
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
      test("convert Chez ValidationError to CaskChez ValidationError") {
        val chezError = chez.ValidationError.TypeMismatch("string", "number", "/test")
        val caskChezError = caskchez.ValidationError.fromChezError(chezError, "body")
        
        assert(caskChezError.isInstanceOf[caskchez.ValidationError.RequestBodyError])
        assert(caskChezError.message.contains("Type mismatch"))
        assert(caskChezError.path == "/test")
      }

      test("convert different error types with different contexts") {
        val chezError = chez.ValidationError.MissingField("name", "/user")
        
        val bodyError = caskchez.ValidationError.fromChezError(chezError, "body")
        assert(bodyError.isInstanceOf[caskchez.ValidationError.RequestBodyError])
        
        val queryError = caskchez.ValidationError.fromChezError(chezError, "query")
        assert(queryError.isInstanceOf[caskchez.ValidationError.QueryParamError])
        
        val paramError = caskchez.ValidationError.fromChezError(chezError, "params")
        assert(paramError.isInstanceOf[caskchez.ValidationError.PathParamError])
        
        val headerError = caskchez.ValidationError.fromChezError(chezError, "headers")
        assert(headerError.isInstanceOf[caskchez.ValidationError.HeaderError])
      }
    }

    test("Request body validation") {
      test("valid JSON body validates against schema") {
        val bodySchema = Chez.Object(
          properties = Map(
            "name" -> Chez.String(minLength = Some(1)),
            "age" -> Chez.Integer(minimum = Some(0))
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
        val bodySchema = Chez.Object(
          properties = Map("name" -> Chez.String(minLength = Some(1))),
          required = Set("name")
        )
        
        val invalidBody = """{"name": ""}""" // Empty name fails minLength
        val request = new MockRequest(bodyContent = invalidBody)
        
        val result = ValidationHelpers.validateRequestBody(request, bodySchema)
        assert(result.isLeft)
        
        val errors = result.left.getOrElse(throw new Exception("Expected Left"))
        assert(errors.nonEmpty)
        assert(errors.head.isInstanceOf[caskchez.ValidationError.RequestBodyError])
      }

      test("malformed JSON body fails with parse error") {
        val bodySchema = Chez.Object(properties = Map("name" -> Chez.String()))
        
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
        val querySchema = Chez.Object(
          properties = Map(
            "page" -> Chez.Integer(minimum = Some(1)),
            "limit" -> Chez.Integer(minimum = Some(1), maximum = Some(100))
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
        val querySchema = Chez.Object(
          properties = Map("page" -> Chez.Integer(minimum = Some(1))),
          required = Set("page")
        )
        
        val queryParams = Map("page" -> Seq("0")) // Below minimum
        val request = new MockRequest(queryParamsMap = queryParams)
        
        val result = ValidationHelpers.validateQueryParams(request, querySchema)
        assert(result.isLeft)
        
        val errors = result.left.getOrElse(throw new Exception("Expected Left"))
        assert(errors.nonEmpty)
        assert(errors.head.isInstanceOf[caskchez.ValidationError.QueryParamError])
      }

      test("type conversion for query parameters") {
        val querySchema = Chez.Object(
          properties = Map(
            "enabled" -> Chez.Boolean(),
            "count" -> Chez.Integer(),
            "name" -> Chez.String()
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
        val paramsSchema = Chez.Object(
          properties = Map(
            "id" -> Chez.Integer(minimum = Some(1)),
            "category" -> Chez.String(minLength = Some(1))
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
        val paramsSchema = Chez.Object(
          properties = Map("id" -> Chez.Integer(minimum = Some(1))),
          required = Set("id")
        )
        
        val pathParams = Map("id" -> "0") // Below minimum
        
        val result = ValidationHelpers.validatePathParams(pathParams, paramsSchema)
        assert(result.isLeft)
        
        val errors = result.left.getOrElse(throw new Exception("Expected Left"))
        assert(errors.nonEmpty)
        assert(errors.head.isInstanceOf[caskchez.ValidationError.PathParamError])
      }
    }

    test("Header validation") {
      test("valid headers validate against schema") {
        val headersSchema = Chez.Object(
          properties = Map(
            "Authorization" -> Chez.String(pattern = Some("Bearer .*")),
            "Content-Type" -> Chez.String(const = Some("application/json"))
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
        val headersSchema = Chez.Object(
          properties = Map("Authorization" -> Chez.String(pattern = Some("Bearer .*"))),
          required = Set("Authorization")
        )
        
        val headers = Map("Authorization" -> Seq("InvalidToken")) // Doesn't match pattern
        val request = new MockRequest(headersMap = headers)
        
        val result = ValidationHelpers.validateHeaders(request, headersSchema)
        assert(result.isLeft)
        
        val errors = result.left.getOrElse(throw new Exception("Expected Left"))
        assert(errors.nonEmpty)
        assert(errors.head.isInstanceOf[caskchez.ValidationError.HeaderError])
      }
    }

    test("Complete request validation") {
      test("valid complete request validates successfully") {
        val routeSchema = RouteSchema(
          body = Some(Chez.Object(
            properties = Map("name" -> Chez.String()),
            required = Set("name")
          )),
          query = Some(Chez.Object(
            properties = Map("page" -> Chez.Integer(minimum = Some(1)))
          )),
          params = Some(Chez.Object(
            properties = Map("id" -> Chez.Integer()),
            required = Set("id")
          )),
          headers = Some(Chez.Object(
            properties = Map("Content-Type" -> Chez.String())
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
          body = Some(Chez.Object(
            properties = Map("name" -> Chez.String(minLength = Some(1))),
            required = Set("name")
          )),
          query = Some(Chez.Object(
            properties = Map("page" -> Chez.Integer(minimum = Some(1))),
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
        
        val hasBodyError = errors.exists(_.isInstanceOf[caskchez.ValidationError.RequestBodyError])
        val hasQueryError = errors.exists(_.isInstanceOf[caskchez.ValidationError.QueryParamError])
        assert(hasBodyError)
        assert(hasQueryError)
      }

      test("partial validation when only some schemas are provided") {
        val routeSchema = RouteSchema(
          body = Some(Chez.Object(
            properties = Map("name" -> Chez.String()),
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
          validatedHeaders = Some(Map("Authorization" -> "Bearer token", "Content-Type" -> "application/json"))
        )
        
        assert(validatedRequest.getHeader("Authorization").contains("Bearer token"))
        assert(validatedRequest.getHeader("Content-Type").contains("application/json"))
        assert(validatedRequest.getHeader("nonexistent").isEmpty)
      }
    }

    test("Error response generation") {
      test("createErrorResponse generates proper JSON response") {
        val errors = List(
          caskchez.ValidationError.RequestBodyError("Invalid name", "/name", Some("name")),
          caskchez.ValidationError.QueryParamError("Invalid page", "/page", Some("page"))
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
        val querySchema = Chez.Object(
          properties = Map(
            "bool_true" -> Chez.Boolean(),
            "bool_false" -> Chez.Boolean(),
            "integer" -> Chez.Integer(),
            "number" -> Chez.Number(),
            "string" -> Chez.String(),
            "empty" -> Chez.String()
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