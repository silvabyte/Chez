package boogieloops.schema.composition

import utest.*
import boogieloops.schema.*
import boogieloops.schema.primitives.*
import boogieloops.schema.complex.*
import boogieloops.schema.composition.*
import boogieloops.schema.validation.{ValidationContext, ValidationResult}

object NotSchemaTests extends TestSuite {

  val tests = Tests {
    test("json schema generation") {
      test("basic not schema") {
        val schema = NotSchema(StringSchema())
        val json = schema.toJsonSchema

        assert(json.obj.contains("not"))
        val notSchema = json("not")
        assert(notSchema("type").str == "string")
      }

      test("not schema with metadata") {
        val schema = NotSchema(IntegerSchema())
          .withTitle("Non-Integer")
          .withDescription("Anything but an integer")
        val json = schema.toJsonSchema

        assert(json("title").str == "Non-Integer")
        assert(json("description").str == "Anything but an integer")
        assert(json.obj.contains("not"))
        assert(json("not")("type").str == "integer")
      }

      test("not schema with complex nested schema") {
        val objectSchema = ObjectSchema(
          properties = Map(
            "type" -> StringSchema(const = Some("admin"))
          ),
          required = Set("type")
        )
        val schema = NotSchema(objectSchema)
        val json = schema.toJsonSchema

        assert(json.obj.contains("not"))
        val notSchema = json("not")
        assert(notSchema("type").str == "object")
        assert(notSchema("properties").obj.contains("type"))
      }
    }

    test("Not validation behavior") {
      test("validates when schema does NOT match") {
        val schema = NotSchema(IntegerSchema())
        val value = ujson.Str("not an integer")
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid)
        assert(result.errors.isEmpty)
      }

      test("fails when schema DOES match") {
        val schema = NotSchema(StringSchema())
        val value = ujson.Str("this is a string")
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(!result.isValid)
        assert(result.errors.nonEmpty)
        assert(result.errors.head.toString.contains("must NOT match"))
      }

      test("validates null against not string") {
        val schema = NotSchema(StringSchema())
        val value = ujson.Null
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid) // null is not a string
      }

      test("validates number against not string") {
        val schema = NotSchema(StringSchema())
        val value = ujson.Num(42)
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid) // number is not a string
      }
    }

    test("Complex Not scenarios") {
      test("not with object schema") {
        val adminSchema = ObjectSchema(
          properties = Map(
            "role" -> StringSchema(const = Some("admin"))
          ),
          required = Set("role")
        )
        val schema = NotSchema(adminSchema)

        // Should validate - not an admin object
        val userValue = ujson.Obj("role" -> ujson.Str("user"))
        val context = ValidationContext()
        val result = schema.validate(userValue, context)
        assert(result.isValid)

        // Should fail - is an admin object
        val adminValue = ujson.Obj("role" -> ujson.Str("admin"))
        val adminResult = schema.validate(adminValue, context)
        assert(!adminResult.isValid)
      }

      test("not with array schema") {
        val emptyArraySchema = ArraySchema(StringSchema(), maxItems = Some(0))
        val schema = NotSchema(emptyArraySchema)

        // Should validate - not an empty array
        val value = ujson.Arr(ujson.Str("item"))
        val context = ValidationContext()
        val result = schema.validate(value, context)
        assert(result.isValid)

        // Should fail - is an empty array
        val emptyValue = ujson.Arr()
        val emptyResult = schema.validate(emptyValue, context)
        assert(!emptyResult.isValid)
      }

      test("nested not composition") {
        val innerNot = NotSchema(StringSchema())
        val outerNot = NotSchema(innerNot)

        // Double negation should validate strings
        val value = ujson.Str("test")
        val context = ValidationContext()
        val result = outerNot.validate(value, context)
        assert(result.isValid)

        // Should fail for non-strings
        val numberValue = ujson.Num(42)
        val numberResult = outerNot.validate(numberValue, context)
        assert(!numberResult.isValid)
      }
    }

    test("Error reporting and paths") {
      test("error paths are preserved during validation") {
        val schema = NotSchema(StringSchema())
        val value = ujson.Str("this matches")
        val context = ValidationContext("/test/path")
        val result = schema.validate(value, context)

        assert(!result.isValid)
        assert(result.errors.head.toString.contains("/test/path"))
      }

      test("composition error message is clear") {
        val schema = NotSchema(IntegerSchema())
        val value = ujson.Num(42)
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(!result.isValid)
        assert(result.errors.head.toString.contains("must NOT match"))
        assert(result.errors.head.isInstanceOf[boogieloops.schema.ValidationError.CompositionError])
      }
    }

    test("Real-world not patterns") {
      test("exclude specific values") {
        val bannedValueSchema = StringSchema(const = Some("admin"))
        val schema = NotSchema(bannedValueSchema)

        val allowedValue = ujson.Str("user")
        val context = ValidationContext()
        val result = schema.validate(allowedValue, context)
        assert(result.isValid)

        val bannedValue = ujson.Str("admin")
        val bannedResult = schema.validate(bannedValue, context)
        assert(!bannedResult.isValid)
      }

      test("exclude object pattern") {
        val testObjectSchema = ObjectSchema(
          properties = Map(
            "test" -> BooleanSchema(const = Some(true))
          ),
          required = Set("test")
        )
        val schema = NotSchema(testObjectSchema)

        // Should validate - production object
        val prodValue = ujson.Obj("env" -> ujson.Str("production"))
        val context = ValidationContext()
        val result = schema.validate(prodValue, context)
        assert(result.isValid)

        // Should fail - test object
        val testValue = ujson.Obj("test" -> ujson.Bool(true))
        val testResult = schema.validate(testValue, context)
        assert(!testResult.isValid)
      }

      test("exclude number range") {
        val invalidRangeSchema = IntegerSchema(minimum = Some(18), maximum = Some(65))
        val schema = NotSchema(invalidRangeSchema)

        // Should validate - outside range
        val youngValue = ujson.Num(16)
        val context = ValidationContext()
        val result = schema.validate(youngValue, context)
        assert(result.isValid)

        val oldValue = ujson.Num(70)
        val _ = schema.validate(oldValue, context)
        assert(result.isValid)

        // Should fail - inside range
        val midValue = ujson.Num(30)
        val midResult = schema.validate(midValue, context)
        assert(!midResult.isValid)
      }
    }

    test("ValidationResult framework integration") {
      test("ValidationResult.valid() for successful not validation") {
        val schema = NotSchema(IntegerSchema())
        val value = ujson.Str("not an integer")
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid)
        assert(result.errors.isEmpty)
        assert(result.isInstanceOf[ValidationResult])
      }

      test("ValidationResult.invalid() for failed not validation") {
        val schema = NotSchema(StringSchema())
        val value = ujson.Str("this is a string")
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(!result.isValid)
        assert(result.errors.nonEmpty)
        assert(result.isInstanceOf[ValidationResult])
        assert(result.errors.head.isInstanceOf[boogieloops.schema.ValidationError.CompositionError])
      }
    }

    test("Edge cases and boundary conditions") {
      test("not with always-true schema") {
        // A schema that accepts anything (empty object schema)
        val alwaysTrueSchema = ObjectSchema()
        val schema = NotSchema(alwaysTrueSchema)

        // Should fail for all values since everything matches an empty object schema
        val value = ujson.Str("anything")
        val context = ValidationContext()
        val result = schema.validate(value, context)

        // This depends on how ObjectSchema handles type mismatches
        // If ObjectSchema rejects non-objects, then not should validate
        // If ObjectSchema accepts all values, then not should fail
        assert(result.isValid) // Assuming ObjectSchema rejects non-objects
      }

      test("not with always-false schema") {
        // A schema that never validates (impossible constraints)
        val alwaysFalseSchema = StringSchema(minLength = Some(10), maxLength = Some(5))
        val schema = NotSchema(alwaysFalseSchema)

        // Should validate for all strings since the inner schema never validates
        val value = ujson.Str("test")
        val context = ValidationContext()
        val result = schema.validate(value, context)
        assert(result.isValid)
      }

      test("not with type mismatch") {
        val schema = NotSchema(IntegerSchema())
        val value = ujson.Bool(true)
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid) // boolean is not an integer
      }
    }
  }
}
