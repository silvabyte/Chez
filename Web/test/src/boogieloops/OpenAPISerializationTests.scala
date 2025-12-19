package boogieloops.web

import utest.*
import upickle.default.*
import boogieloops.web.openapi.models.*

object OpenAPISerializationTests extends TestSuite {

  val tests = Tests {
    test("PathsObject serialization") {
      test("serializes as flat map without paths wrapper") {
        val pathsObject = PathsObject(
          Map(
            "/users" -> PathItemObject(summary = Some("Users endpoint")),
            "/users/{id}" -> PathItemObject(summary = Some("User by ID"))
          )
        )

        val json = writeJs(pathsObject)

        assert(json.obj.contains("/users"))
        assert(json.obj.contains("/users/{id}"))
        assert(!json.obj.contains("paths"))
        assert(json("/users")("summary").str == "Users endpoint")
      }

      test("deserializes from flat map") {
        val jsonStr = """
          {
            "/users": {"summary": "Users endpoint"},
            "/posts": {"summary": "Posts endpoint"}
          }
        """

        val pathsObject = read[PathsObject](jsonStr)

        assert(pathsObject.paths.size == 2)
        assert(pathsObject.paths("/users").summary.contains("Users endpoint"))
        assert(pathsObject.paths("/posts").summary.contains("Posts endpoint"))
      }

      test("roundtrip serialization preserves data") {
        val original = PathsObject(
          Map(
            "/api/v1/users" -> PathItemObject(
              summary = Some("User operations"),
              description = Some("CRUD operations for users")
            )
          )
        )

        val json = write(original)
        val restored = read[PathsObject](json)

        assert(restored == original)
      }
    }

    test("ResponsesObject serialization") {
      test("serializes as flat map with status codes as keys") {
        val responsesObject = ResponsesObject(
          responses = Map(
            "200" -> ResponseObject(description = "Success"),
            "404" -> ResponseObject(description = "Not found")
          )
        )

        val json = writeJs(responsesObject)

        assert(json.obj.contains("200"))
        assert(json.obj.contains("404"))
        assert(!json.obj.contains("responses"))
        assert(json("200")("description").str == "Success")
      }

      test("serializes default response correctly") {
        val responsesObject = ResponsesObject(
          default = Some(ResponseObject(description = "Default error response")),
          responses = Map("200" -> ResponseObject(description = "Success"))
        )

        val json = writeJs(responsesObject)

        assert(json.obj.contains("default"))
        assert(json.obj.contains("200"))
        assert(!json.obj.contains("responses"))
        assert(json("default")("description").str == "Default error response")
      }

      test("deserializes from flat map with default") {
        val jsonStr = """
          {
            "default": {"description": "Error"},
            "200": {"description": "OK"},
            "201": {"description": "Created"}
          }
        """

        val responsesObject = read[ResponsesObject](jsonStr)

        assert(responsesObject.default.isDefined)
        assert(responsesObject.default.get.description == "Error")
        assert(responsesObject.responses.size == 2)
        assert(responsesObject.responses("200").description == "OK")
      }

      test("roundtrip serialization preserves data") {
        val original = ResponsesObject(
          default = Some(ResponseObject(description = "Unexpected error")),
          responses = Map(
            "200" -> ResponseObject(description = "Success"),
            "400" -> ResponseObject(description = "Bad request"),
            "500" -> ResponseObject(description = "Server error")
          )
        )

        val json = write(original)
        val restored = read[ResponsesObject](json)

        assert(restored == original)
      }
    }

    test("CallbackObject serialization") {
      test("serializes as flat map without callbacks wrapper") {
        val callbackObject = CallbackObject(
          Map(
            "{$request.body#/callbackUrl}" -> PathItemObject(
              summary = Some("Callback endpoint")
            )
          )
        )

        val json = writeJs(callbackObject)

        assert(json.obj.contains("{$request.body#/callbackUrl}"))
        assert(!json.obj.contains("callbacks"))
      }

      test("deserializes from flat map") {
        val jsonStr = """
          {
            "{$request.query.callbackUrl}": {"summary": "Query callback"},
            "{$request.body#/webhook}": {"summary": "Body callback"}
          }
        """

        val callbackObject = read[CallbackObject](jsonStr)

        assert(callbackObject.callbacks.size == 2)
        assert(
          callbackObject.callbacks("{$request.query.callbackUrl}").summary.contains("Query callback")
        )
      }

      test("roundtrip serialization preserves data") {
        val original = CallbackObject(
          Map(
            "{$request.body#/url}" -> PathItemObject(
              summary = Some("Webhook callback"),
              post = Some(OperationObject(
                summary = Some("Receive webhook"),
                responses = ResponsesObject(
                  responses = Map("200" -> ResponseObject(description = "Webhook received"))
                )
              ))
            )
          )
        )

        val json = write(original)
        val restored = read[CallbackObject](json)

        assert(restored == original)
      }
    }

    test("SecurityRequirementObject serialization") {
      test("serializes as flat map without requirements wrapper") {
        val securityReq = SecurityRequirementObject(
          Map("oauth2" -> List("read", "write"))
        )

        val json = writeJs(securityReq)

        assert(json.obj.contains("oauth2"))
        assert(!json.obj.contains("requirements"))
        assert(json("oauth2").arr.map(_.str).toList == List("read", "write"))
      }

      test("serializes empty scopes correctly") {
        val securityReq = SecurityRequirementObject(
          Map("bearerAuth" -> List.empty)
        )

        val json = writeJs(securityReq)

        assert(json.obj.contains("bearerAuth"))
        assert(json("bearerAuth").arr.isEmpty)
      }

      test("serializes multiple schemes") {
        val securityReq = SecurityRequirementObject(
          Map(
            "apiKey" -> List.empty,
            "oauth2" -> List("read")
          )
        )

        val json = writeJs(securityReq)

        assert(json.obj.contains("apiKey"))
        assert(json.obj.contains("oauth2"))
        assert(json.obj.size == 2)
      }

      test("deserializes from flat map") {
        val jsonStr = """{"oauth2": ["read", "write", "admin"]}"""

        val securityReq = read[SecurityRequirementObject](jsonStr)

        assert(securityReq.requirements.size == 1)
        assert(securityReq.requirements("oauth2") == List("read", "write", "admin"))
      }

      test("roundtrip serialization preserves data") {
        val original = SecurityRequirementObject(
          Map(
            "oauth2" -> List("read", "write"),
            "apiKey" -> List.empty
          )
        )

        val json = write(original)
        val restored = read[SecurityRequirementObject](json)

        assert(restored == original)
      }
    }

    test("Full OpenAPI document serialization") {
      test("paths field contains flat map structure") {
        val doc = OpenAPIDocument(
          openapi = "3.1.1",
          info = InfoObject(
            title = "Test API",
            description = "Test",
            version = "1.0.0"
          ),
          jsonSchemaDialect = "https://json-schema.org/draft/2020-12/schema",
          paths = Some(PathsObject(
            Map("/users" -> PathItemObject(summary = Some("Users")))
          ))
        )

        val json = writeJs(doc)

        assert(json("paths").obj.contains("/users"))
        assert(!json("paths").obj.contains("paths"))
      }

      test("security field contains flat map structures") {
        val doc = OpenAPIDocument(
          openapi = "3.1.1",
          info = InfoObject(
            title = "Test API",
            description = "Test",
            version = "1.0.0"
          ),
          jsonSchemaDialect = "https://json-schema.org/draft/2020-12/schema",
          security = Some(List(
            SecurityRequirementObject(Map("oauth2" -> List("read"))),
            SecurityRequirementObject(Map("apiKey" -> List.empty))
          ))
        )

        val json = writeJs(doc)

        val securityArr = json("security").arr
        assert(securityArr(0).obj.contains("oauth2"))
        assert(!securityArr(0).obj.contains("requirements"))
        assert(securityArr(1).obj.contains("apiKey"))
      }

      test("operation responses contain flat map structure") {
        val doc = OpenAPIDocument(
          openapi = "3.1.1",
          info = InfoObject(
            title = "Test API",
            description = "Test",
            version = "1.0.0"
          ),
          jsonSchemaDialect = "https://json-schema.org/draft/2020-12/schema",
          paths = Some(PathsObject(
            Map("/users" -> PathItemObject(
              get = Some(OperationObject(
                summary = Some("List users"),
                responses = ResponsesObject(
                  responses = Map(
                    "200" -> ResponseObject(description = "Success"),
                    "500" -> ResponseObject(description = "Error")
                  )
                )
              ))
            ))
          ))
        )

        val json = writeJs(doc)

        val responses = json("paths")("/users")("get")("responses")
        assert(responses.obj.contains("200"))
        assert(responses.obj.contains("500"))
        assert(!responses.obj.contains("responses"))
      }
    }
  }
}
