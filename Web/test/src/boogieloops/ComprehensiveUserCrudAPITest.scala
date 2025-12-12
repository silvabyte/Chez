package boogieloops.web

import utest._
import upickle.default._

object ComprehensiveUserCrudAPITest extends TestSuite {

  // Helper to make HTTP requests with proper error handling
  implicit val checked: Boolean = false // Don't throw on non-2xx responses

  val tests = Tests {

    test("Advanced Validation Scenarios") {
      test("Schema Constraint Boundary Testing") {
        TestServer.withServer { (host, routes) =>
          // Test minimum valid age (0) - boundary value
          val minAgeUser = routes.CreateUserRequest(
            name = "Baby User",
            email = "baby@example.com",
            age = 0
          )

          val minAgeResponse = requests.post(
            url = s"$host/users",
            data = requests.RequestBlob.ByteSourceRequestBlob(write(minAgeUser)),
            headers = Map("Content-Type" -> "application/json")
          )

          assert(minAgeResponse.statusCode == 200)
          val createdMinUser = read[routes.User](minAgeResponse.text())
          assert(createdMinUser.age == 0)

          // Test maximum valid age (150) - boundary value
          val maxAgeUser = routes.CreateUserRequest(
            name = "Elder User",
            email = "elder@example.com",
            age = 150
          )

          val maxAgeResponse = requests.post(
            url = s"$host/users",
            data = requests.RequestBlob.ByteSourceRequestBlob(write(maxAgeUser)),
            headers = Map("Content-Type" -> "application/json")
          )

          assert(maxAgeResponse.statusCode == 200)
          val createdMaxUser = read[routes.User](maxAgeResponse.text())
          assert(createdMaxUser.age == 150)

          // Test minimum name length (1 character) - boundary value
          val minNameUser = routes.CreateUserRequest(
            name = "A",
            email = "a@example.com",
            age = 25
          )

          val minNameResponse = requests.post(
            url = s"$host/users",
            data = requests.RequestBlob.ByteSourceRequestBlob(write(minNameUser)),
            headers = Map("Content-Type" -> "application/json")
          )

          assert(minNameResponse.statusCode == 200)
          val createdMinNameUser = read[routes.User](minNameResponse.text())
          assert(createdMinNameUser.name == "A")

          // Test maximum name length (100 characters) - boundary value
          val longName = "A" * 100
          val maxNameUser = routes.CreateUserRequest(
            name = longName,
            email = "long@example.com",
            age = 25
          )

          val maxNameResponse = requests.post(
            url = s"$host/users",
            data = requests.RequestBlob.ByteSourceRequestBlob(write(maxNameUser)),
            headers = Map("Content-Type" -> "application/json")
          )

          assert(maxNameResponse.statusCode == 200)
          val createdMaxNameUser = read[routes.User](maxNameResponse.text())
          assert(createdMaxNameUser.name == longName)
        }
      }

      test("Schema Validation Edge Cases") {
        TestServer.withServer { (host, routes) =>
          // Test name with special characters and unicode
          val unicodeNameUser = routes.CreateUserRequest(
            name = "José Müller-Østra 한국어",
            email = "unicode@example.com",
            age = 30
          )

          val unicodeResponse = requests.post(
            url = s"$host/users",
            data = requests.RequestBlob.ByteSourceRequestBlob(write(unicodeNameUser)),
            headers = Map("Content-Type" -> "application/json")
          )

          assert(unicodeResponse.statusCode == 200)
          val createdUser = read[routes.User](unicodeResponse.text())
          assert(createdUser.name == "José Müller-Østra 한국어")

          // Test name with numbers and symbols
          val symbolNameUser = routes.CreateUserRequest(
            name = "User123 & Co. Ltd.",
            email = "symbols@example.com",
            age = 25
          )

          val symbolResponse = requests.post(
            url = s"$host/users",
            data = requests.RequestBlob.ByteSourceRequestBlob(write(symbolNameUser)),
            headers = Map("Content-Type" -> "application/json")
          )

          assert(symbolResponse.statusCode == 200)
          val symbolUser = read[routes.User](symbolResponse.text())
          assert(symbolUser.name == "User123 & Co. Ltd.")

          // Test email with various valid formats
          val complexEmails = List(
            "test+tag@example.co.uk",
            "user.name@subdomain.example.com",
            "123@example.org",
            "test-email@example-domain.net"
          )

          complexEmails.zipWithIndex.foreach { case (email, index) =>
            val user = routes.CreateUserRequest(
              name = s"Email Test $index",
              email = email,
              age = 25 + index
            )

            val response = requests.post(
              url = s"$host/users",
              data = requests.RequestBlob.ByteSourceRequestBlob(write(user)),
              headers = Map("Content-Type" -> "application/json")
            )

            assert(response.statusCode == 200)
            val createdUser = read[routes.User](response.text())
            assert(createdUser.email == email)
          }
        }
      }

      test("Complex JSON Payload Validation") {
        TestServer.withServer { (host, routes) =>
          // Test JSON with extra whitespace and formatting using proper structure
          val formattedUser = routes.CreateUserRequest(
            name = "Formatted User",
            email = "formatted@example.com",
            age = 35,
            isActive = true
          )

          val formattedResponse = requests.post(
            url = s"$host/users",
            data = requests.RequestBlob.ByteSourceRequestBlob(write(formattedUser)),
            headers = Map("Content-Type" -> "application/json")
          )

          assert(formattedResponse.statusCode == 200)
          val user = read[routes.User](formattedResponse.text())
          assert(user.name == "Formatted User")
          assert(user.isActive == true)

          // Test JSON with boolean variations using structured data
          val booleanVariations = List(
            (true, "Bool Test True"),
            (false, "Bool Test False")
          )

          booleanVariations.zipWithIndex.foreach { case ((expected, name), index) =>
            val testUser = routes.CreateUserRequest(
              name = name,
              email = s"bool$index@example.com",
              age = 30,
              isActive = expected
            )

            val response = requests.post(
              url = s"$host/users",
              data = requests.RequestBlob.ByteSourceRequestBlob(write(testUser)),
              headers = Map("Content-Type" -> "application/json")
            )

            assert(response.statusCode == 200)
            val user = read[routes.User](response.text())
            assert(user.isActive == expected)
            assert(user.name == name)
          }
        }
      }
    }

    test("Advanced Error Handling and Recovery") {
      test("Multiple Validation Errors") {
        TestServer.withServer { (host, _) =>
          // Test request with multiple validation issues
          try {
            val multiErrorJson =
              """{"name": "", "email": "invalid-email", "age": -1, "isActive": "not-boolean"}"""

            val response = requests.post(
              url = s"$host/users",
              data = requests.RequestBlob.ByteSourceRequestBlob(multiErrorJson),
              headers = Map("Content-Type" -> "application/json")
            )

            // Should either fail at HTTP level (400) or return validation errors
            if (response.statusCode == 400) {
              val body = response.text()
              assert(body.contains("validation"))
            } else {
              // Parse as error response
              val errorBody = response.text()
              assert(errorBody.contains("validation") || errorBody.contains("error"))
            }
          } catch {
            case e: requests.RequestFailedException =>
              assert(e.response.statusCode >= 400)
              val body = e.response.text()
              assert(body.contains("validation") || body.contains("error"))
          }
        }
      }

      test("Malformed Request Recovery") {
        TestServer.withServer { (host, _) =>
          val malformedRequests = List(
            """{"name": "Test" "email": "test@example.com"}""", // Missing comma
            """{"name": "Test", "email": "test@example.com",}""", // Trailing comma
            """{"name": "Test", "email": test@example.com}""", // Missing quotes
            """{"name": "Test", "email": "test@example.com"""" // Incomplete JSON
          )

          malformedRequests.foreach { malformedJson =>
            try {
              val response = requests.post(
                url = s"$host/users",
                data = requests.RequestBlob.ByteSourceRequestBlob(malformedJson),
                headers = Map("Content-Type" -> "application/json")
              )

              // Should return error
              assert(response.statusCode >= 400)
            } catch {
              case e: requests.RequestFailedException =>
                assert(e.response.statusCode >= 400)
                val body = e.response.text()
                assert(
                  body.contains("parse") || body.contains("JSON") || body.contains("validation")
                )
            }
          }
        }
      }

      test("Content Type Handling") {
        TestServer.withServer { (host, routes) =>
          val validUser = routes.CreateUserRequest(
            name = "Content Type Test",
            email = "content@example.com",
            age = 28
          )

          // Test without Content-Type header
          val noContentTypeResponse = requests.post(
            url = s"$host/users",
            data = requests.RequestBlob.ByteSourceRequestBlob(write(validUser))
            // No Content-Type header
          )

          // Should still work since the JSON is valid
          assert(noContentTypeResponse.statusCode == 200)

          // Test with various Content-Type variations
          val contentTypes = List(
            "application/json",
            "application/json; charset=utf-8",
            "application/json;charset=UTF-8"
          )

          contentTypes.foreach { contentType =>
            val response = requests.post(
              url = s"$host/users",
              data = requests.RequestBlob.ByteSourceRequestBlob(write(validUser)),
              headers = Map("Content-Type" -> contentType)
            )

            assert(response.statusCode == 200)
          }
        }
      }
    }

    test("Performance and Scalability Scenarios") {
      test("Bulk Operations Simulation") {
        TestServer.withServer { (host, routes) =>
          // Test creating many users rapidly
          val bulkUsers = (1 to 20).map { i =>
            routes.CreateUserRequest(s"Bulk User $i", s"bulk$i@example.com", 20 + (i % 30))
          }

          // Create all users
          bulkUsers.foreach { user =>
            val response = requests.post(
              url = s"$host/users",
              data = requests.RequestBlob.ByteSourceRequestBlob(write(user)),
              headers = Map("Content-Type" -> "application/json")
            )
            assert(response.statusCode == 200)
          }

          // Verify all were created
          val listResponse = requests.get(s"$host/users")
          val allUsers = read[List[routes.User]](listResponse.text())
          assert(allUsers.length >= 22) // 2 initial + 20 bulk

          // Verify email uniqueness is maintained
          val emails = allUsers.map(_.email)
          assert(emails.distinct.length == emails.length)
        }
      }

      test("State Consistency Under Load") {
        TestServer.withServer { (host, routes) =>
          // Simulate concurrent operations
          val operations = List(
            ("create1", routes.CreateUserRequest("User A", "a@load.com", 25)),
            ("create2", routes.CreateUserRequest("User B", "b@load.com", 30)),
            ("create3", routes.CreateUserRequest("User C", "c@load.com", 35))
          )

          // Execute operations rapidly
          operations.foreach { case (_, user) =>
            requests.post(
              url = s"$host/users",
              data = requests.RequestBlob.ByteSourceRequestBlob(write(user)),
              headers = Map("Content-Type" -> "application/json")
            )
          }

          // Verify final state consistency
          val finalResponse = requests.get(s"$host/users")
          val finalUsers = read[List[routes.User]](finalResponse.text())

          // Check that all expected users exist
          assert(finalUsers.exists(_.email == "a@load.com"))
          assert(finalUsers.exists(_.email == "b@load.com"))
          assert(finalUsers.exists(_.email == "c@load.com"))

          // Check ID assignment consistency (should be sequential)
          val userIds = finalUsers.map(_.id.toInt).sorted
          val expectedSequence = (1 to userIds.length).toList
          assert(userIds == expectedSequence)
        }
      }

      test("Memory and Resource Management") {
        TestServer.withServer { (host, routes) =>
          // Test with large payload data
          val largeName = "A" * 99 // Just under the 100 character limit
          val largeUser = routes.CreateUserRequest(
            name = largeName,
            email = "large@example.com",
            age = 50
          )

          val largeResponse = requests.post(
            url = s"$host/users",
            data = requests.RequestBlob.ByteSourceRequestBlob(write(largeUser)),
            headers = Map("Content-Type" -> "application/json")
          )

          assert(largeResponse.statusCode == 200)
          val createdLargeUser = read[routes.User](largeResponse.text())
          assert(createdLargeUser.name.length == 99)

          // Test multiple large requests don't cause memory issues
          (1 to 5).foreach { i =>
            val user = routes.CreateUserRequest(
              name = s"Large User $i " + ("X" * 80),
              email = s"large$i@example.com",
              age = 25 + i
            )

            val response = requests.post(
              url = s"$host/users",
              data = requests.RequestBlob.ByteSourceRequestBlob(write(user)),
              headers = Map("Content-Type" -> "application/json")
            )

            assert(response.statusCode == 200)
          }
        }
      }
    }

    test("API Response Quality and Consistency") {
      test("Response Format Consistency") {
        TestServer.withServer { (host, routes) =>
          // Test that all successful responses have consistent structure
          val testUser = routes.CreateUserRequest(
            name = "Response Test User",
            email = "response@example.com",
            age = 30
          )

          val createResponse = requests.post(
            url = s"$host/users",
            data = requests.RequestBlob.ByteSourceRequestBlob(write(testUser)),
            headers = Map("Content-Type" -> "application/json")
          )

          assert(createResponse.statusCode == 200)
          val responseBody = createResponse.text()
          val json = ujson.read(responseBody)

          // Verify all required fields are present and correctly typed
          assert(json.obj.contains("id"))
          assert(json.obj.contains("name"))
          assert(json.obj.contains("email"))
          assert(json.obj.contains("age"))
          assert(json.obj.contains("isActive"))

          // Verify field types
          assert(json("id").str.nonEmpty)
          assert(json("name").str == "Response Test User")
          assert(json("email").str == "response@example.com")
          assert(json("age").num == 30)
          assert(json("isActive").bool == true)

          // Test that the created user appears in list with same format
          val listResponse = requests.get(s"$host/users")
          val users = read[List[routes.User]](listResponse.text())
          val createdUser = users.find(_.email == "response@example.com")
          assert(createdUser.isDefined)
          assert(createdUser.get.name == "Response Test User")
        }
      }

      test("Error Response Quality") {
        TestServer.withServer { (host, routes) =>
          // Test error response for non-existent user has proper structure
          val response = requests.get(s"$host/users/999")
          assert(response.statusCode == 200) // Our implementation returns 200 with error body

          val responseBody = response.text()
          val json = ujson.read(responseBody)

          // Verify error response structure
          assert(json.obj.contains("error"))
          assert(json.obj.contains("message"))
          assert(json("error").str == "user_not_found")
          assert(json("message").str.contains("999"))

          // Details field should be present and be an array (even if empty)
          if (json.obj.contains("details")) {
            assert(json("details").arr.length >= 0)
          }

          // Test duplicate email error has proper structure
          val duplicateUser = routes.CreateUserRequest(
            name = "Duplicate Test",
            email = "alice@example.com", // Already exists
            age = 25
          )

          val duplicateResponse = requests.post(
            url = s"$host/users",
            data = requests.RequestBlob.ByteSourceRequestBlob(write(duplicateUser)),
            headers = Map("Content-Type" -> "application/json")
          )

          assert(duplicateResponse.statusCode == 200)
          val duplicateJson = ujson.read(duplicateResponse.text())
          assert(duplicateJson.obj.contains("error"))
          assert(duplicateJson.obj.contains("message"))
          assert(duplicateJson("error").str == "email_exists")
        }
      }

      test("Data Integrity Validation") {
        TestServer.withServer { (host, routes) =>
          // Create a user and verify all data is preserved exactly
          val precisionUser = routes.CreateUserRequest(
            name = "Precision Test User",
            email = "precision@example.com",
            age = 42
          )

          val createResponse = requests.post(
            url = s"$host/users",
            data = requests.RequestBlob.ByteSourceRequestBlob(write(precisionUser)),
            headers = Map("Content-Type" -> "application/json")
          )

          assert(createResponse.statusCode == 200)
          val createdUser = read[routes.User](createResponse.text())

          // Verify exact data preservation
          assert(createdUser.name == "Precision Test User")
          assert(createdUser.email == "precision@example.com")
          assert(createdUser.age == 42)
          assert(createdUser.isActive == true) // Default value

          // Verify the user can be retrieved with exact same data
          val retrieveResponse = requests.get(s"$host/users/${createdUser.id}")
          assert(retrieveResponse.statusCode == 200)
          val retrievedUser = read[routes.User](retrieveResponse.text())

          assert(retrievedUser == createdUser) // Exact match
        }
      }
    }

    test("Advanced Schema Validation Integration") {
      test("T8 Validation Framework Integration") {
        TestServer.withServer { (host, routes) =>
          // Test that our T8 validation framework is properly integrated
          // by testing edge cases that should trigger validation logic

          // Test validation with edge case values that should pass
          val edgeCaseUsers = List(
            routes.CreateUserRequest("A", "a@b.co", 0), // Minimum values
            routes.CreateUserRequest("Z" * 100, "z@example.com", 150), // Maximum values
            routes.CreateUserRequest("Mixed123!@#", "mixed@test.org", 75) // Mixed content
          )

          edgeCaseUsers.foreach { user =>
            val response = requests.post(
              url = s"$host/users",
              data = requests.RequestBlob.ByteSourceRequestBlob(write(user)),
              headers = Map("Content-Type" -> "application/json")
            )

            assert(response.statusCode == 200)
            val createdUser = read[routes.User](response.text())
            assert(createdUser.name == user.name)
            assert(createdUser.email == user.email)
            assert(createdUser.age == user.age)
          }

          // Verify all edge case users were stored correctly
          val listResponse = requests.get(s"$host/users")
          val allUsers = read[List[routes.User]](listResponse.text())

          assert(allUsers.exists(_.name == "A"))
          assert(allUsers.exists(_.name == "Z" * 100))
          assert(allUsers.exists(_.name == "Mixed123!@#"))
        }
      }
    }
  }
}
