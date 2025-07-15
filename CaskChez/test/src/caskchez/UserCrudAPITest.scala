package caskchez

import utest._
import upickle.default._

object UserCrudAPITest extends TestSuite {

  // Helper to make HTTP requests with proper error handling
  implicit val checked: Boolean = false // Don't throw on non-2xx responses

  val tests = Tests {

    test("Health Check") {
      TestServer.withServer { (host, routes) =>
        val response = requests.get(s"$host/health")
        assert(response.statusCode == 200)
        val body = ujson.read(response.text())
        assert(body("status").str == "ok")
      }
    }

    test("List Users") {
      TestServer.withServer { (host, routes) =>
        val response = requests.get(s"$host/users")
        assert(response.statusCode == 200)
        
        val users = read[List[routes.User]](response.text())
        assert(users.length == 2)
        assert(users.exists(_.name == "Alice Smith"))
        assert(users.exists(_.name == "Bob Johnson"))
      }
    }

    test("Get User by ID") {
      TestServer.withServer { (host, routes) =>
        // Test existing user
        val response = requests.get(s"$host/users/1")
        assert(response.statusCode == 200)
        
        val user = read[routes.User](response.text())
        assert(user.id == "1")
        assert(user.name == "Alice Smith")
        assert(user.email == "alice@example.com")

        // Test non-existent user
        val notFoundResponse = requests.get(s"$host/users/999")
        assert(notFoundResponse.statusCode == 200) // API returns 200 with error body
        
        val error = read[routes.ErrorResponse](notFoundResponse.text())
        assert(error.error == "user_not_found")
        assert(error.message.contains("999"))
      }
    }

    test("Create User - Valid Request") {
      TestServer.withServer { (host, routes) =>
        val newUser = routes.CreateUserRequest(
          name = "John Doe",
          email = "john@example.com",
          age = 30,
          isActive = true
        )
        
        val response = requests.post(
          url = s"$host/users",
          data = requests.RequestBlob.ByteSourceRequestBlob(write(newUser)),
          headers = Map("Content-Type" -> "application/json")
        )
        
        assert(response.statusCode == 200)
        val createdUser = read[routes.User](response.text())
        assert(createdUser.name == "John Doe")
        assert(createdUser.email == "john@example.com")
        assert(createdUser.age == 30)
        assert(createdUser.isActive == true)
        
        // Verify user was actually created
        val listResponse = requests.get(s"$host/users")
        val users = read[List[routes.User]](listResponse.text())
        assert(users.length == 3)
        assert(users.exists(_.email == "john@example.com"))
      }
    }

    test("Create User - Validation Errors") {
      TestServer.withServer { (host, routes) =>
        // Test empty name - should fail validation
        val emptyNameUser = routes.CreateUserRequest(
          name = "",
          email = "test@example.com",
          age = 25
        )
        
        try {
          val emptyNameResponse = requests.post(
            url = s"$host/users",
            data = requests.RequestBlob.ByteSourceRequestBlob(write(emptyNameUser)),
            headers = Map("Content-Type" -> "application/json")
          )
          
          // If no exception, check for validation error response
          assert(emptyNameResponse.statusCode == 400)
          val responseBody = emptyNameResponse.text()
          assert(responseBody.contains("Validation failed"))
        } catch {
          case e: requests.RequestFailedException =>
            assert(e.response.statusCode == 400)
            val responseBody = e.response.text()
            assert(responseBody.contains("Validation failed"))
        }

        // Test age out of range - should fail validation
        val invalidAgeUser = routes.CreateUserRequest(
          name = "Test User",
          email = "test2@example.com",
          age = 200 // Over maximum
        )
        
        try {
          val invalidAgeResponse = requests.post(
            url = s"$host/users",
            data = requests.RequestBlob.ByteSourceRequestBlob(write(invalidAgeUser)),
            headers = Map("Content-Type" -> "application/json")
          )
          
          assert(invalidAgeResponse.statusCode == 400)
          val responseBody = invalidAgeResponse.text()
          assert(responseBody.contains("Validation failed"))
        } catch {
          case e: requests.RequestFailedException =>
            assert(e.response.statusCode == 400)
            val responseBody = e.response.text()
            assert(responseBody.contains("Validation failed"))
        }
      }
    }

    test("Create User - Duplicate Email") {
      TestServer.withServer { (host, routes) =>
        val duplicateUser = routes.CreateUserRequest(
          name = "Another Alice",
          email = "alice@example.com", // This email already exists
          age = 28
        )
        
        val response = requests.post(
          url = s"$host/users",
          data = requests.RequestBlob.ByteSourceRequestBlob(write(duplicateUser)),
          headers = Map("Content-Type" -> "application/json")
        )
        
        assert(response.statusCode == 200) // The endpoint returns 200 even for business logic errors
        val error = read[routes.ErrorResponse](response.text())
        assert(error.error == "email_exists")
        assert(error.message.contains("alice@example.com"))
      }
    }

    test("Delete User") {
      TestServer.withServer { (host, routes) =>
        // Test deleting existing user
        val response = requests.delete(s"$host/users/1")
        assert(response.statusCode == 200)
        
        val deletedUser = read[routes.User](response.text())
        assert(deletedUser.id == "1")
        assert(deletedUser.name == "Alice Smith")

        // Verify user was actually deleted
        val getResponse = requests.get(s"$host/users/1")
        assert(getResponse.statusCode == 200) // API returns 200 with error body
        val deleteError = read[routes.ErrorResponse](getResponse.text())
        assert(deleteError.error == "user_not_found")

        // Verify user list is updated
        val listResponse = requests.get(s"$host/users")
        val users = read[List[routes.User]](listResponse.text())
        assert(users.length == 1)
        assert(!users.exists(_.id == "1"))

        // Test deleting non-existent user
        val notFoundResponse = requests.delete(s"$host/users/999")
        assert(notFoundResponse.statusCode == 200) // Endpoint returns 200 with error body
        
        val error = read[routes.ErrorResponse](notFoundResponse.text())
        assert(error.error == "user_not_found")
        assert(error.message.contains("999"))
      }
    }

    test("Malformed JSON Request") {
      TestServer.withServer { (host, routes) =>
        try {
          val response = requests.post(
            url = s"$host/users",
            data = requests.RequestBlob.ByteSourceRequestBlob("""{"name": "John", "email": invalid json}"""),
            headers = Map("Content-Type" -> "application/json")
          )
          
          assert(response.statusCode == 400)
          val responseBody = response.text()
          assert(responseBody.contains("Validation failed"))
        } catch {
          case e: requests.RequestFailedException =>
            assert(e.response.statusCode == 400)
            val responseBody = e.response.text()
            assert(responseBody.contains("Validation failed"))
            assert(responseBody.contains("parse"))
        }
      }
    }

    test("Empty Request Body") {
      TestServer.withServer { (host, routes) =>
        try {
          val response = requests.post(
            url = s"$host/users",
            data = requests.RequestBlob.ByteSourceRequestBlob(""),
            headers = Map("Content-Type" -> "application/json")
          )
          
          assert(response.statusCode == 400)
          val responseBody = response.text()
          assert(responseBody.contains("Validation failed"))
        } catch {
          case e: requests.RequestFailedException =>
            assert(e.response.statusCode == 400)
            val responseBody = e.response.text()
            assert(responseBody.contains("Validation failed"))
        }
      }
    }
  }
}