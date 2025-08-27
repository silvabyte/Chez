package chez.composition

import utest.*
import chez.*
import chez.primitives.*
import chez.complex.*
import chez.composition.*
import chez.validation.{ValidationContext, ValidationResult}

object AllOfChezTests extends TestSuite {

  val tests = Tests {
    test("json schema generation") {
      test("basic allOf schema") {
        val schema = AllOfChez(List(
          StringChez(minLength = Some(5)),
          StringChez(maxLength = Some(10)),
          StringChez(pattern = Some("^[A-Z].*"))
        ))
        val json = schema.toJsonSchema

        assert(json.obj.contains("allOf"))
        val allOfArray = json("allOf").arr
        assert(allOfArray.length == 3)
        assert(allOfArray(0)("minLength").num == 5)
        assert(allOfArray(1)("maxLength").num == 10)
        assert(allOfArray(2)("pattern").str == "^[A-Z].*")
      }

      test("allOf schema with metadata") {
        val schema = AllOfChez(List(
          StringChez(),
          IntegerChez()
        )).withTitle("Mixed Type").withDescription("String and integer constraints")
        val json = schema.toJsonSchema

        assert(json("title").str == "Mixed Type")
        assert(json("description").str == "String and integer constraints")
        assert(json.obj.contains("allOf"))
      }
    }

    test("AllOf validation behavior") {
      test("validates when all schemas match") {
        val schema = AllOfChez(List(
          StringChez(minLength = Some(3)),
          StringChez(maxLength = Some(10)),
          StringChez(pattern = Some("^[A-Z].*"))
        ))
        val value = ujson.Str("Hello")
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid)
        assert(result.errors.isEmpty)
      }

      test("fails when any schema doesn't match") {
        val schema = AllOfChez(List(
          StringChez(minLength = Some(3)),
          StringChez(maxLength = Some(5)),
          StringChez(pattern = Some("^[A-Z].*"))
        ))
        val value = ujson.Str("Hello World") // Too long and doesn't start with capital
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(!result.isValid)
        assert(result.errors.nonEmpty)
      }

      test("collects all validation errors") {
        val schema = AllOfChez(List(
          StringChez(minLength = Some(10)), // Will fail - too short
          StringChez(maxLength = Some(3)), // Will fail - too long
          StringChez(pattern = Some("^\\d+$")) // Will fail - not numeric
        ))
        val value = ujson.Str("Hello")
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(!result.isValid)
        assert(result.errors.length >= 3) // Should have errors from all three schemas
      }
    }

    test("Complex AllOf scenarios") {
      test("allOf with object schemas") {
        val schema = AllOfChez(List(
          ObjectChez(
            properties = Map(
              "name" -> StringChez(),
              "age" -> IntegerChez()
            ),
            required = Set("name")
          ),
          ObjectChez(
            properties = Map(
              "age" -> IntegerChez(minimum = Some(18))
            ),
            required = Set("age")
          )
        ))
        val value = ujson.Obj("name" -> ujson.Str("John"), "age" -> ujson.Num(25))
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid)
      }

      test("allOf object validation failure") {
        val schema = AllOfChez(List(
          ObjectChez(
            properties = Map(
              "name" -> StringChez(),
              "age" -> IntegerChez()
            ),
            required = Set("name")
          ),
          ObjectChez(
            properties = Map(
              "age" -> IntegerChez(minimum = Some(18))
            ),
            required = Set("age")
          )
        ))
        val value = ujson.Obj("name" -> ujson.Str("John"), "age" -> ujson.Num(16)) // Age too low
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(!result.isValid)
        assert(result.errors.exists(_.toString.contains("OutOfRange")))
      }

      test("nested allOf composition") {
        val innerAllOf = AllOfChez(List(
          StringChez(minLength = Some(3)),
          StringChez(maxLength = Some(10))
        ))
        val outerAllOf = AllOfChez(List(
          innerAllOf,
          StringChez(pattern = Some("^[A-Z].*"))
        ))
        val value = ujson.Str("Hello")
        val context = ValidationContext()
        val result = outerAllOf.validate(value, context)

        assert(result.isValid)
      }
    }

    test("Error reporting and paths") {
      test("error paths are preserved during validation") {
        val schema = AllOfChez(List(
          StringChez(minLength = Some(10)),
          StringChez(pattern = Some("^\\d+$"))
        ))
        val value = ujson.Str("short")
        val context = ValidationContext("/test/path")
        val result = schema.validate(value, context)

        assert(!result.isValid)
        assert(result.errors.exists(_.toString.contains("/test/path")))
      }

      test("multiple validation errors from different schemas") {
        val schema = AllOfChez(List(
          StringChez(minLength = Some(10)),
          StringChez(pattern = Some("^\\d+$")),
          StringChez(maxLength = Some(3))
        ))
        val value = ujson.Str("test")
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(!result.isValid)
        assert(result.errors.length >= 2) // Should have errors from multiple schemas
      }
    }

    test("Real-world allOf patterns") {
      test("string with multiple constraints") {
        val schema = AllOfChez(List(
          StringChez(minLength = Some(8)),
          StringChez(pattern = Some(".*[A-Z].*")), // Must contain uppercase
          StringChez(pattern = Some(".*[0-9].*")) // Must contain digit
        ))
        val validValue = ujson.Str("Password123")
        val context = ValidationContext()
        val result = schema.validate(validValue, context)

        assert(result.isValid)

        val invalidValue = ujson.Str("password") // No uppercase or digit
        val invalidResult = schema.validate(invalidValue, context)
        assert(!invalidResult.isValid)
      }

      test("object inheritance pattern") {
        val baseSchema = ObjectChez(
          properties = Map(
            "id" -> StringChez(),
            "created" -> StringChez()
          ),
          required = Set("id")
        )

        val specificSchema = ObjectChez(
          properties = Map(
            "name" -> StringChez(minLength = Some(1)),
            "email" -> StringChez(format = Some("email"))
          ),
          required = Set("name", "email")
        )

        val schema = AllOfChez(List(baseSchema, specificSchema))

        val value = ujson.Obj(
          "id" -> ujson.Str("123"),
          "created" -> ujson.Str("2023-01-01"),
          "name" -> ujson.Str("John"),
          "email" -> ujson.Str("john@example.com")
        )
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid)
      }
    }

    test("ValidationResult framework integration") {
      test("ValidationResult.valid() for successful allOf validation") {
        val schema = AllOfChez(List(
          StringChez(),
          StringChez(minLength = Some(1))
        ))
        val value = ujson.Str("test")
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid)
        assert(result.errors.isEmpty)
        assert(result.isInstanceOf[ValidationResult])
      }

      test("ValidationResult.invalid() for failed allOf validation") {
        val schema = AllOfChez(List(
          StringChez(minLength = Some(10)),
          StringChez(pattern = Some("^\\d+$"))
        ))
        val value = ujson.Str("short")
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(!result.isValid)
        assert(result.errors.nonEmpty)
        assert(result.isInstanceOf[ValidationResult])
      }
    }

    test("Edge cases and boundary conditions") {
      test("empty schemas list") {
        val schema = AllOfChez(List.empty)
        val value = ujson.Str("anything")
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid) // Empty allOf should validate as true
      }

      test("single schema in allOf") {
        val schema = AllOfChez(List(StringChez(minLength = Some(3))))
        val value = ujson.Str("test")
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid)
      }

      test("allOf with identical schemas") {
        val stringSchema = StringChez(minLength = Some(3))
        val schema = AllOfChez(List(stringSchema, stringSchema, stringSchema))
        val value = ujson.Str("test")
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid)
      }
    }
  }
}
