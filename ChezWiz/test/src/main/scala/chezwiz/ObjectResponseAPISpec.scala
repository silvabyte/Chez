import utest.*
import chezwiz.agent.{ObjectResponse, Usage}
import ujson.Value
import upickle.default.*

object ObjectResponseAPISpec extends TestSuite:

  case class TestData(name: String, age: Int) derives ReadWriter

  val tests = Tests {

    test("ObjectResponse provides clean API") {
      val testObj = TestData("Alice", 30)
      val response = ObjectResponse(
        data = testObj,
        usage = Some(Usage(10, 20, 30)),
        model = "test-model",
        finishReason = Some("stop")
      )

      // Simple, clean access to data
      assert(response.data == testObj)
      assert(response.data.name == "Alice")
      assert(response.data.age == 30)
    }

    test("ObjectResponse with ujson.Value works correctly") {
      val jsonData = ujson.Obj("name" -> "Bob", "age" -> 25)
      val jsonResponse = ObjectResponse[ujson.Value](
        data = jsonData,
        usage = Some(Usage(5, 15, 20)),
        model = "test-model",
        finishReason = Some("stop")
      )

      // Simple access to raw JSON data
      assert(jsonResponse.data == jsonData)

      // Test conversion to typed response
      val typedResponse = ObjectResponse[TestData](
        data = read[TestData](jsonResponse.data),
        usage = jsonResponse.usage,
        model = jsonResponse.model,
        finishReason = jsonResponse.finishReason
      )
      assert(typedResponse.data.name == "Bob")
      assert(typedResponse.data.age == 25)
    }

    test("API demonstrates clean usage patterns") {
      val jsonData = ujson.Obj("name" -> "Charlie", "age" -> 35)
      val jsonResponse = ObjectResponse[ujson.Value](
        data = jsonData,
        usage = None,
        model = "test-model",
        finishReason = Some("stop")
      )

      // Simple conversion pattern
      val person = read[TestData](jsonResponse.data)

      assert(person.name == "Charlie")
      assert(person.age == 35)

      // Direct access to raw data
      assert(jsonResponse.data("name").str == "Charlie")
      assert(jsonResponse.data("age").num == 35)
    }
  }
