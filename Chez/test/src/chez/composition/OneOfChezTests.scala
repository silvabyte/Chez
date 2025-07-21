package chez.composition

import utest.*
import chez.*
import chez.primitives.*
import chez.complex.*
import chez.composition.*
import chez.validation.{ValidationContext, ValidationResult}
import upickle.default.*

object OneOfChezTests extends TestSuite {

  val tests = Tests {
    test("json schema generation") {
      test("basic oneOf schema") {
        val schema = OneOfChez(List(
          StringChez(),
          IntegerChez(),
          BooleanChez()
        ))
        val json = schema.toJsonSchema

        assert(json.obj.contains("oneOf"))
        val oneOfArray = json("oneOf").arr
        assert(oneOfArray.length == 3)
        assert(oneOfArray(0)("type").str == "string")
        assert(oneOfArray(1)("type").str == "integer")
        assert(oneOfArray(2)("type").str == "boolean")
      }

      test("oneOf schema with metadata") {
        val schema = OneOfChez(List(
          StringChez(),
          IntegerChez()
        )).withTitle("Exactly String or Integer")
          .withDescription("Must match exactly one: string or integer")

        val json = schema.toJsonSchema

        assert(json("title").str == "Exactly String or Integer")
        assert(json("description").str == "Must match exactly one: string or integer")
        assert(json("oneOf").arr.length == 2)
      }
    }

    test("OneOf validation behavior") {
      test("validates when exactly one schema matches") {
        val schema = OneOfChez(List(
          StringChez(),
          IntegerChez(),
          BooleanChez()
        ))

        // Valid string (matches only first schema)
        val result1 = schema.validate(ujson.Str("hello"), ValidationContext())
        assert(result1.isValid)

        // Valid integer (matches only second schema)
        val result2 = schema.validate(ujson.Num(42), ValidationContext())
        assert(result2.isValid)

        // Valid boolean (matches only third schema)
        val result3 = schema.validate(ujson.Bool(true), ValidationContext())
        assert(result3.isValid)
      }

      test("fails when no schemas match") {
        val schema = OneOfChez(List(
          StringChez(minLength = Some(10)), // requires string >= 10 chars
          IntegerChez(minimum = Some(100)), // requires integer >= 100
          BooleanChez() // only accepts booleans
        ))

        // Short string fails all schemas
        val result1 = schema.validate(ujson.Str("hi"), ValidationContext())
        assert(!result1.isValid)
        assert(result1.errors.length >= 2) // Should collect errors from failed schemas

        // Check for "does not match any" error
        val hasNoMatchError = result1.errors.exists {
          case chez.ValidationError.CompositionError(msg, _) =>
            msg.contains("does not match any")
          case _ => false
        }
        assert(hasNoMatchError)

        // Object fails all schemas
        val result2 = schema.validate(ujson.Obj(), ValidationContext())
        assert(!result2.isValid)
      }

      test("fails when multiple schemas match") {
        val schema = OneOfChez(List(
          StringChez(), // matches any string
          StringChez(minLength = Some(1)), // also matches non-empty strings
          IntegerChez()
        ))

        // String matches both first and second schema - should fail
        val result = schema.validate(ujson.Str("hello"), ValidationContext())
        assert(!result.isValid)

        // Check for "matches more than one" error
        val hasMultipleMatchError = result.errors.exists {
          case chez.ValidationError.CompositionError(msg, _) =>
            msg.contains("matches more than one")
          case _ => false
        }
        assert(hasMultipleMatchError)
      }
    }

    test("Complex OneOf scenarios") {
      test("oneOf with distinct object schemas") {
        val personSchema = ObjectChez(
          properties = Map(
            "type" -> StringChez(const = Some("person")),
            "name" -> StringChez(),
            "age" -> IntegerChez()
          ),
          required = Set("type", "name")
        )

        val companySchema = ObjectChez(
          properties = Map(
            "type" -> StringChez(const = Some("company")),
            "companyName" -> StringChez(),
            "employees" -> IntegerChez()
          ),
          required = Set("type", "companyName")
        )

        val schema = OneOfChez(List(personSchema, companySchema))

        // Valid person object (matches only person schema)
        val person = ujson.Obj(
          "type" -> ujson.Str("person"),
          "name" -> ujson.Str("John"),
          "age" -> ujson.Num(30)
        )
        val result1 = schema.validate(person, ValidationContext())
        assert(result1.isValid)

        // Valid company object (matches only company schema)
        val company = ujson.Obj(
          "type" -> ujson.Str("company"),
          "companyName" -> ujson.Str("Acme Corp"),
          "employees" -> ujson.Num(100)
        )
        val result2 = schema.validate(company, ValidationContext())
        assert(result2.isValid)

        // Invalid object (could match both if not for discriminator)
        val ambiguous = ujson.Obj(
          "name" -> ujson.Str("Could be person"),
          "companyName" -> ujson.Str("Or company")
        )
        val result3 = schema.validate(ambiguous, ValidationContext())
        assert(!result3.isValid) // Should fail due to missing required "type" field
      }

      test("nested oneOf composition") {
        val innerOneOf = OneOfChez(List(
          StringChez(minLength = Some(5)),
          IntegerChez(minimum = Some(10))
        ))

        val outerOneOf = OneOfChez(List(
          innerOneOf,
          BooleanChez(),
          ArrayChez(StringChez(), minItems = Some(1))
        ))

        // Valid through inner oneOf (long string)
        val result1 = outerOneOf.validate(ujson.Str("hello"), ValidationContext())
        assert(result1.isValid)

        // Valid through inner oneOf (large integer)
        val result2 = outerOneOf.validate(ujson.Num(15), ValidationContext())
        assert(result2.isValid)

        // Valid through outer oneOf (boolean)
        val result3 = outerOneOf.validate(ujson.Bool(true), ValidationContext())
        assert(result3.isValid)

        // Valid through outer oneOf (array)
        val result4 = outerOneOf.validate(ujson.Arr(ujson.Str("item")), ValidationContext())
        assert(result4.isValid)

        // Invalid (short string doesn't match inner oneOf requirements)
        val result5 = outerOneOf.validate(ujson.Str("hi"), ValidationContext())
        assert(!result5.isValid)
      }
    }

    test("Error reporting and paths") {
      test("error paths are preserved during validation") {
        val schema = OneOfChez(List(
          StringChez(minLength = Some(10)),
          IntegerChez(minimum = Some(100))
        ))

        val context = ValidationContext("/config/value")
        val result = schema.validate(ujson.Str("short"), context)

        assert(!result.isValid)
        // Should have errors with correct path
        val hasCorrectPath = result.errors.exists(_.toString.contains("/config/value"))
        assert(hasCorrectPath)
      }

      test("composition errors include context") {
        val schema = OneOfChez(List(
          StringChez(minLength = Some(10)),
          IntegerChez(minimum = Some(100))
        ))

        val context = ValidationContext("/api/data")
        val result = schema.validate(ujson.Str("test"), context)

        assert(!result.isValid)
        val compositionError = result.errors.find {
          case chez.ValidationError.CompositionError(_, path) => path == "/api/data"
          case _ => false
        }
        assert(compositionError.isDefined)
      }
    }

    test("Real-world oneOf patterns") {
      test("discriminated union with type field") {
        val circleSchema = ObjectChez(
          properties = Map(
            "shape" -> StringChez(const = Some("circle")),
            "radius" -> NumberChez(minimum = Some(0))
          ),
          required = Set("shape", "radius")
        )

        val rectangleSchema = ObjectChez(
          properties = Map(
            "shape" -> StringChez(const = Some("rectangle")),
            "width" -> NumberChez(minimum = Some(0)),
            "height" -> NumberChez(minimum = Some(0))
          ),
          required = Set("shape", "width", "height")
        )

        val shapeSchema = OneOfChez(List(circleSchema, rectangleSchema))

        // Valid circle
        val circle = ujson.Obj(
          "shape" -> ujson.Str("circle"),
          "radius" -> ujson.Num(5.0)
        )
        val result1 = shapeSchema.validate(circle, ValidationContext())
        assert(result1.isValid)

        // Valid rectangle
        val rectangle = ujson.Obj(
          "shape" -> ujson.Str("rectangle"),
          "width" -> ujson.Num(10.0),
          "height" -> ujson.Num(5.0)
        )
        val result2 = shapeSchema.validate(rectangle, ValidationContext())
        assert(result2.isValid)

        // Invalid shape type
        val invalid = ujson.Obj(
          "shape" -> ujson.Str("triangle"),
          "sides" -> ujson.Num(3)
        )
        val result3 = shapeSchema.validate(invalid, ValidationContext())
        assert(!result3.isValid)
      }

      test("API response format selection") {
        val successSchema = ObjectChez(
          properties = Map(
            "status" -> StringChez(const = Some("success")),
            "data" -> ObjectChez()
          ),
          required = Set("status", "data")
        )

        val errorSchema = ObjectChez(
          properties = Map(
            "status" -> StringChez(const = Some("error")),
            "message" -> StringChez(),
            "code" -> IntegerChez()
          ),
          required = Set("status", "message")
        )

        val responseSchema = OneOfChez(List(successSchema, errorSchema))

        // Valid success response
        val success = ujson.Obj(
          "status" -> ujson.Str("success"),
          "data" -> ujson.Obj("result" -> ujson.Str("OK"))
        )
        val result1 = responseSchema.validate(success, ValidationContext())
        assert(result1.isValid)

        // Valid error response
        val error = ujson.Obj(
          "status" -> ujson.Str("error"),
          "message" -> ujson.Str("Not found"),
          "code" -> ujson.Num(404)
        )
        val result2 = responseSchema.validate(error, ValidationContext())
        assert(result2.isValid)
      }
    }

    test("ValidationResult framework integration") {
      test("ValidationResult.valid() for successful oneOf validation") {
        val schema = OneOfChez(List(
          StringChez(),
          IntegerChez()
        ))

        val result = schema.validate(ujson.Str("test"), ValidationContext())
        assert(result.isValid)
        assert(result.errors.isEmpty)
        assert(result.isInstanceOf[ValidationResult.Valid.type])
      }

      test("ValidationResult.invalid() for failed oneOf validation") {
        val schema = OneOfChez(List(
          StringChez(minLength = Some(10)),
          IntegerChez(minimum = Some(100))
        ))

        val result = schema.validate(ujson.Str("short"), ValidationContext())
        assert(!result.isValid)
        assert(result.errors.nonEmpty)
        assert(result.isInstanceOf[ValidationResult.Invalid])
      }

      test("ValidationResult.invalid() for multiple matches") {
        val schema = OneOfChez(List(
          StringChez(),
          StringChez(maxLength = Some(100))
        ))

        val result = schema.validate(ujson.Str("test"), ValidationContext())
        assert(!result.isValid)
        assert(result.errors.nonEmpty)
        assert(result.isInstanceOf[ValidationResult.Invalid])
      }
    }

    test("Edge cases and boundary conditions") {
      test("empty schemas list") {
        val schema = OneOfChez(List.empty)

        // Should always fail with empty schema list
        val result1 = schema.validate(ujson.Str("test"), ValidationContext())
        assert(!result1.isValid)

        val result2 = schema.validate(ujson.Null, ValidationContext())
        assert(!result2.isValid)
      }

      test("single schema in oneOf") {
        val schema = OneOfChez(List(StringChez(minLength = Some(3))))

        // Should behave like the single schema
        val result1 = schema.validate(ujson.Str("hello"), ValidationContext())
        assert(result1.isValid)

        val result2 = schema.validate(ujson.Str("hi"), ValidationContext())
        assert(!result2.isValid)

        val result3 = schema.validate(ujson.Num(42), ValidationContext())
        assert(!result3.isValid)
      }

      test("oneOf with identical schemas") {
        val schema = OneOfChez(List(
          StringChez(),
          StringChez() // Identical schema
        ))

        // Should fail because string matches both identical schemas
        val result = schema.validate(ujson.Str("test"), ValidationContext())
        assert(!result.isValid)

        val hasMultipleMatchError = result.errors.exists {
          case chez.ValidationError.CompositionError(msg, _) =>
            msg.contains("matches more than one")
          case _ => false
        }
        assert(hasMultipleMatchError)
      }
    }
  }
}
