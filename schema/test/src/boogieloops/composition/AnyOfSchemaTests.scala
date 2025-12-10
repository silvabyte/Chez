package boogieloops.schema.composition

import utest.*
import boogieloops.schema.*
import boogieloops.schema.primitives.*
import boogieloops.schema.complex.*
import boogieloops.schema.composition.*
import boogieloops.schema.validation.{ValidationContext, ValidationResult}

object AnyOfSchemaTests extends TestSuite {

  val tests = Tests {
    test("json schema generation") {
      test("basic anyOf schema") {
        val schema = AnyOfSchema(List(
          StringSchema(),
          IntegerSchema(),
          BooleanSchema()
        ))
        val json = schema.toJsonSchema

        assert(json.obj.contains("anyOf"))
        val anyOfArray = json("anyOf").arr
        assert(anyOfArray.length == 3)
        assert(anyOfArray(0)("type").str == "string")
        assert(anyOfArray(1)("type").str == "integer")
        assert(anyOfArray(2)("type").str == "boolean")
      }

      test("anyOf schema with metadata") {
        val schema = AnyOfSchema(List(
          StringSchema(),
          IntegerSchema()
        )).withTitle("String or Integer")
          .withDescription("Either a string or an integer value")

        val json = schema.toJsonSchema

        assert(json("title").str == "String or Integer")
        assert(json("description").str == "Either a string or an integer value")
        assert(json("anyOf").arr.length == 2)
      }
    }

    test("AnyOf validation behavior") {
      test("validates when at least one schema matches") {
        val schema = AnyOfSchema(List(
          StringSchema(minLength = Some(5)),
          IntegerSchema(minimum = Some(10)),
          BooleanSchema()
        ))

        // Valid string (matches first schema)
        val result1 = schema.validate(ujson.Str("hello"), ValidationContext())
        assert(result1.isValid)

        // Valid integer (matches second schema)
        val result2 = schema.validate(ujson.Num(15), ValidationContext())
        assert(result2.isValid)

        // Valid boolean (matches third schema)
        val result3 = schema.validate(ujson.Bool(true), ValidationContext())
        assert(result3.isValid)
      }

      test("validates when multiple schemas match") {
        val schema = AnyOfSchema(List(
          StringSchema(), // matches any string
          StringSchema(minLength = Some(3)) // matches strings >= 3 chars
        ))

        // This should match both schemas (anyOf allows multiple matches)
        val result = schema.validate(ujson.Str("hello"), ValidationContext())
        assert(result.isValid)
      }

      test("fails when no schemas match") {
        val schema = AnyOfSchema(List(
          StringSchema(minLength = Some(10)), // requires string >= 10 chars
          IntegerSchema(minimum = Some(100)), // requires integer >= 100
          BooleanSchema() // only accepts booleans
        ))

        // Short string fails all schemas
        val result1 = schema.validate(ujson.Str("hi"), ValidationContext())
        assert(!result1.isValid)
        assert(result1.errors.length >= 2) // Should collect errors from failed schemas

        // Small integer fails all schemas
        val result2 = schema.validate(ujson.Num(5), ValidationContext())
        assert(!result2.isValid)
        assert(result2.errors.length >= 2)

        // Object fails all schemas
        val result3 = schema.validate(ujson.Obj(), ValidationContext())
        assert(!result3.isValid)
        assert(result3.errors.length >= 3)
      }
    }

    test("Complex AnyOf scenarios") {
      test("anyOf with object schemas") {
        val personSchema = ObjectSchema(
          properties = Map(
            "name" -> StringSchema(),
            "age" -> IntegerSchema()
          ),
          required = Set("name")
        )

        val companySchema = ObjectSchema(
          properties = Map(
            "companyName" -> StringSchema(),
            "employees" -> IntegerSchema()
          ),
          required = Set("companyName")
        )

        val schema = AnyOfSchema(List(personSchema, companySchema))

        // Valid person object
        val person = ujson.Obj("name" -> ujson.Str("John"), "age" -> ujson.Num(30))
        val result1 = schema.validate(person, ValidationContext())
        assert(result1.isValid)

        // Valid company object
        val company =
          ujson.Obj("companyName" -> ujson.Str("Acme Corp"), "employees" -> ujson.Num(100))
        val result2 = schema.validate(company, ValidationContext())
        assert(result2.isValid)

        // Invalid object (missing required fields for both schemas)
        val invalid = ujson.Obj("description" -> ujson.Str("neither person nor company"))
        val result3 = schema.validate(invalid, ValidationContext())
        assert(!result3.isValid)
      }

      test("nested anyOf composition") {
        val innerAnyOf = AnyOfSchema(List(
          StringSchema(),
          IntegerSchema()
        ))

        val outerAnyOf = AnyOfSchema(List(
          innerAnyOf,
          BooleanSchema(),
          ArraySchema(StringSchema())
        ))

        // Valid through inner anyOf (string)
        val result1 = outerAnyOf.validate(ujson.Str("test"), ValidationContext())
        assert(result1.isValid)

        // Valid through inner anyOf (integer)
        val result2 = outerAnyOf.validate(ujson.Num(42), ValidationContext())
        assert(result2.isValid)

        // Valid through outer anyOf (boolean)
        val result3 = outerAnyOf.validate(ujson.Bool(true), ValidationContext())
        assert(result3.isValid)

        // Valid through outer anyOf (array)
        val result4 = outerAnyOf.validate(ujson.Arr(ujson.Str("hello")), ValidationContext())
        assert(result4.isValid)

        // Invalid (object doesn't match any schema)
        val result5 = outerAnyOf.validate(ujson.Obj(), ValidationContext())
        assert(!result5.isValid)
      }
    }

    test("Error reporting and paths") {
      test("error paths are preserved during validation") {
        val schema = AnyOfSchema(List(
          StringSchema(minLength = Some(10)),
          IntegerSchema(minimum = Some(100))
        ))

        val context = ValidationContext("/data/value")
        val result = schema.validate(ujson.Str("short"), context)

        assert(!result.isValid)
        // Should have errors from both schemas with correct path
        val hasCorrectPath = result.errors.exists(_.toString.contains("/data/value"))
        assert(hasCorrectPath)
      }

      test("multiple validation errors are collected") {
        val schema = AnyOfSchema(List(
          StringSchema(minLength = Some(5), maxLength = Some(3)), // impossible constraint
          IntegerSchema(minimum = Some(10), maximum = Some(5)), // impossible constraint
          BooleanSchema()
        ))

        val result = schema.validate(ujson.Str("test"), ValidationContext())
        // Should be valid because boolean schema will fail but string schema errors are collected
        // Actually this should be invalid because "test" doesn't match boolean
        assert(!result.isValid)
        // Should collect multiple constraint errors
        assert(result.errors.length >= 1)
      }
    }

    test("Real-world anyOf patterns") {
      test("string or null pattern") {
        val schema = AnyOfSchema(List(
          StringSchema(minLength = Some(1)),
          NullSchema()
        ))

        // Valid string
        val result1 = schema.validate(ujson.Str("hello"), ValidationContext())
        assert(result1.isValid)

        // Valid null
        val result2 = schema.validate(ujson.Null, ValidationContext())
        assert(result2.isValid)

        // Invalid empty string
        val result3 = schema.validate(ujson.Str(""), ValidationContext())
        assert(!result3.isValid)

        // Invalid number
        val result4 = schema.validate(ujson.Num(42), ValidationContext())
        assert(!result4.isValid)
      }

      test("multiple data format pattern") {
        val schema = AnyOfSchema(List(
          StringSchema(format = Some("email")),
          StringSchema(format = Some("uuid")), // Use UUID instead of URI for stricter validation
          StringSchema(pattern = Some("^\\+[0-9]{10,15}$")) // phone number pattern
        ))

        // Valid email format
        val result1 = schema.validate(ujson.Str("test@example.com"), ValidationContext())
        assert(result1.isValid)

        // Valid UUID format
        val result2 =
          schema.validate(ujson.Str("550e8400-e29b-41d4-a716-446655440000"), ValidationContext())
        assert(result2.isValid)

        // Valid phone number
        val result3 = schema.validate(ujson.Str("+1234567890"), ValidationContext())
        assert(result3.isValid)

        // Invalid format (string that matches none of the schemas)
        val result4 = schema.validate(ujson.Str("invalid-text"), ValidationContext())
        assert(!result4.isValid)
      }
    }

    test("ValidationResult framework integration") {
      test("ValidationResult.valid() for successful anyOf validation") {
        val schema = AnyOfSchema(List(
          StringSchema(),
          IntegerSchema()
        ))

        val result = schema.validate(ujson.Str("test"), ValidationContext())
        assert(result.isValid)
        assert(result.errors.isEmpty)
        assert(result.isInstanceOf[ValidationResult.Valid.type])
      }

      test("ValidationResult.invalid() for failed anyOf validation") {
        val schema = AnyOfSchema(List(
          StringSchema(minLength = Some(10)),
          IntegerSchema(minimum = Some(100))
        ))

        val result = schema.validate(ujson.Str("short"), ValidationContext())
        assert(!result.isValid)
        assert(result.errors.nonEmpty)
        assert(result.isInstanceOf[ValidationResult.Invalid])
      }
    }

    test("Edge cases and boundary conditions") {
      test("empty schemas list") {
        val schema = AnyOfSchema(List.empty)

        // Should always fail with empty schema list
        val result1 = schema.validate(ujson.Str("test"), ValidationContext())
        assert(!result1.isValid)

        val result2 = schema.validate(ujson.Null, ValidationContext())
        assert(!result2.isValid)
      }

      test("single schema in anyOf") {
        val schema = AnyOfSchema(List(StringSchema(minLength = Some(3))))

        // Should behave like the single schema
        val result1 = schema.validate(ujson.Str("hello"), ValidationContext())
        assert(result1.isValid)

        val result2 = schema.validate(ujson.Str("hi"), ValidationContext())
        assert(!result2.isValid)
      }
    }
  }
}
