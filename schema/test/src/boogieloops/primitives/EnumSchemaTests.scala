package boogieloops.schema.primitives

import utest.*

import boogieloops.schema.primitives.EnumSchema
import boogieloops.schema.validation.ValidationContext
import boogieloops.schema.*
import boogieloops.schema.{Schema as bl}

object EnumSchemaTests extends TestSuite {

  val tests = Tests {
    test("String enum creation and validation") {
      test("fromStrings factory method") {
        val enumSchema = EnumSchema.fromStrings("red", "green", "blue")

        assert(enumSchema.isStringEnum)
        assert(enumSchema.getStringValues.contains(List("red", "green", "blue")))

        // Valid string validation
        val result1 = enumSchema.validate(ujson.Str("red"), ValidationContext())
        assert(result1.isValid)
        val result2 = enumSchema.validate(ujson.Str("green"), ValidationContext())
        assert(result2.isValid)
        val result3 = enumSchema.validate(ujson.Str("blue"), ValidationContext())
        assert(result3.isValid)

        // Invalid string validation
        val result4 = enumSchema.validate(ujson.Str("yellow"), ValidationContext())
        assert(!result4.isValid)
        val result5 = enumSchema.validate(ujson.Str(""), ValidationContext())
        assert(!result5.isValid)
      }

      test("fromStrings with List") {
        val colors = List("red", "green", "blue")
        val enumSchema = EnumSchema.fromStrings(colors)

        assert(enumSchema.isStringEnum)
        assert(enumSchema.getStringValues.contains(colors))
      }
    }

    test("Mixed enum creation and validation") {
      test("mixed factory method") {
        val enumSchema = EnumSchema.mixed("active", 1, true, null)

        assert(!enumSchema.isStringEnum)
        assert(enumSchema.getStringValues.isEmpty)

        // Valid mixed validation
        val result1 = enumSchema.validate(ujson.Str("active"), ValidationContext())
        assert(result1.isValid)
        val result2 = enumSchema.validate(ujson.Num(1), ValidationContext())
        assert(result2.isValid)
        val result3 = enumSchema.validate(ujson.Bool(true), ValidationContext())
        assert(result3.isValid)
        val result4 = enumSchema.validate(ujson.Null, ValidationContext())
        assert(result4.isValid)

        // Invalid validation
        val result5 = enumSchema.validate(ujson.Str("inactive"), ValidationContext())
        assert(!result5.isValid)
        val result6 = enumSchema.validate(ujson.Num(2), ValidationContext())
        assert(!result6.isValid)
        val result7 = enumSchema.validate(ujson.Bool(false), ValidationContext())
        assert(!result7.isValid)
      }

      test("fromValues with ujson.Value") {
        val enumSchema = EnumSchema.fromValues(
          ujson.Str("hello"),
          ujson.Num(42),
          ujson.True,
          ujson.Null
        )

        assert(!enumSchema.isStringEnum)

        // Validate using ujson values directly
        val result1 = enumSchema.validate(ujson.Str("hello"), ValidationContext())
        assert(result1.isValid)
        val result2 = enumSchema.validate(ujson.Num(42), ValidationContext())
        assert(result2.isValid)
        val result3 = enumSchema.validate(ujson.True, ValidationContext())
        assert(result3.isValid)
        val result4 = enumSchema.validate(ujson.Null, ValidationContext())
        assert(result4.isValid)

        // Invalid values
        val result5 = enumSchema.validate(ujson.Str("world"), ValidationContext())
        assert(!result5.isValid)
        val result6 = enumSchema.validate(ujson.Num(43), ValidationContext())
        assert(!result6.isValid)
        val result7 = enumSchema.validate(ujson.False, ValidationContext())
        assert(!result7.isValid)
      }
    }

    test("Numeric enum creation") {
      test("fromNumbers factory method") {
        val enumSchema = EnumSchema.fromNumbers(1.0, 2.5, 3.14)

        assert(!enumSchema.isStringEnum)

        // Valid numeric validation
        val result1 = enumSchema.validate(ujson.Num(1.0), ValidationContext())
        assert(result1.isValid)
        val result2 = enumSchema.validate(ujson.Num(2.5), ValidationContext())
        assert(result2.isValid)
        val result3 = enumSchema.validate(ujson.Num(3.14), ValidationContext())
        assert(result3.isValid)

        // Invalid validation
        val result4 = enumSchema.validate(ujson.Num(4.0), ValidationContext())
        assert(!result4.isValid)
        val result5 = enumSchema.validate(ujson.Str("1.0"), ValidationContext())
        assert(!result5.isValid)
      }

      test("fromInts factory method") {
        val enumSchema = EnumSchema.fromInts(1, 2, 3)

        assert(!enumSchema.isStringEnum)

        // Valid integer validation (converted to double in ujson)
        val result1 = enumSchema.validate(ujson.Num(1), ValidationContext())
        assert(result1.isValid)
        val result2 = enumSchema.validate(ujson.Num(2), ValidationContext())
        assert(result2.isValid)
        val result3 = enumSchema.validate(ujson.Num(3), ValidationContext())
        assert(result3.isValid)

        // Invalid validation
        val result4 = enumSchema.validate(ujson.Num(4), ValidationContext())
        assert(!result4.isValid)
      }
    }

    test("Boolean enum creation") {
      test("fromBooleans factory method") {
        val enumSchema = EnumSchema.fromBooleans(true, false)

        assert(!enumSchema.isStringEnum)

        // Valid boolean validation
        val result1 = enumSchema.validate(ujson.Bool(true), ValidationContext())
        assert(result1.isValid)
        val result2 = enumSchema.validate(ujson.Bool(false), ValidationContext())
        assert(result2.isValid)

        // Type checking
        val types = enumSchema.getValueTypes
        assert(types.contains("boolean"))
        assert(types.size == 1)
      }

      test("single boolean value") {
        val enumSchema = EnumSchema.fromBooleans(true)

        val result1 = enumSchema.validate(ujson.Bool(true), ValidationContext())
        assert(result1.isValid)
        val result2 = enumSchema.validate(ujson.Bool(false), ValidationContext())
        assert(!result2.isValid)
      }
    }

    test("JSON Schema generation") {
      test("string enum schema") {
        val enumSchema = EnumSchema.fromStrings("red", "green", "blue")
        val jsonSchema = enumSchema.toJsonSchema

        // Should have type: string for string enums
        assert(jsonSchema("type").str == "string")

        // Should have enum values
        assert(jsonSchema.obj.contains("enum"))
        val enumValues = jsonSchema("enum").arr.map(_.str).toSet
        assert(enumValues == Set("red", "green", "blue"))
      }

      test("mixed enum schema") {
        val enumSchema = EnumSchema.mixed("active", 1, true)
        val jsonSchema = enumSchema.toJsonSchema

        // Should NOT have type field for mixed enums
        assert(!jsonSchema.obj.contains("type"))

        // Should have enum values
        assert(jsonSchema.obj.contains("enum"))
        val enumArray = jsonSchema("enum").arr
        assert(enumArray.length == 3)
      }

      test("schema with metadata") {
        val enumSchema = EnumSchema
          .fromStrings("red", "green", "blue")
          .withTitle("Color")
          .withDescription("Available colors")

        val jsonSchema = enumSchema.toJsonSchema

        assert(jsonSchema("title").str == "Color")
        assert(jsonSchema("description").str == "Available colors")
        assert(jsonSchema("type").str == "string")
        assert(jsonSchema.obj.contains("enum"))
      }
    }

    test("Validation with context paths") {
      test("validate with context provides correct error paths") {
        val enumSchema = EnumSchema.fromStrings("red", "green", "blue")

        val result =
          enumSchema.validate(ujson.Str("yellow"), ValidationContext("/properties/color"))
        assert(!result.isValid)

        val error = result.errors.head
        assert(error.isInstanceOf[boogieloops.schema.ValidationError.TypeMismatch])

        val typeMismatch = error.asInstanceOf[boogieloops.schema.ValidationError.TypeMismatch]
        assert(typeMismatch.path == "/properties/color")
        assert(typeMismatch.actual.contains("yellow"))
        assert(typeMismatch.expected.contains("red"))
      }
    }

    test("Type validation and introspection") {
      test("isValidType checks") {
        val enumSchema = EnumSchema.mixed("hello", 42, true, null)

        // Valid types
        assert(enumSchema.isValidType(ujson.Str("world")))
        assert(enumSchema.isValidType(ujson.Num(99)))
        assert(enumSchema.isValidType(ujson.True))
        assert(enumSchema.isValidType(ujson.Null))

        // Invalid types
        assert(!enumSchema.isValidType(ujson.Obj()))
        assert(!enumSchema.isValidType(ujson.Arr()))
      }

      test("getValueTypes provides type information") {
        val enumSchema = EnumSchema.mixed("hello", 42, true, null)
        val types = enumSchema.getValueTypes

        assert(types.contains("string"))
        assert(types.contains("number"))
        assert(types.contains("boolean"))
        assert(types.contains("null"))
        assert(types.size == 4)
      }

      test("string-only enum type information") {
        val enumSchema = EnumSchema.fromStrings("red", "green", "blue")
        val types = enumSchema.getValueTypes

        assert(types.contains("string"))
        assert(types.size == 1)
      }
    }

    test("Enum schema as validator function") {
      test("creates reusable validator") {
        val enumSchema = EnumSchema.fromStrings("active", "inactive", "pending")

        // Valid values
        val result1 = enumSchema.validate(ujson.Str("active"), ValidationContext())
        assert(result1.isValid)
        val result2 = enumSchema.validate(ujson.Str("inactive"), ValidationContext())
        assert(result2.isValid)
        val result3 = enumSchema.validate(ujson.Str("pending"), ValidationContext())
        assert(result3.isValid)

        // Invalid values
        val result4 = enumSchema.validate(ujson.Str("unknown"), ValidationContext())
        assert(!result4.isValid)
        val result5 = enumSchema.validate(ujson.Num(1), ValidationContext())
        assert(!result5.isValid)
      }
    }

    test("Edge cases and error conditions") {
      test("empty enum values") {
        val enumSchema = EnumSchema(List.empty)

        // Any value should be invalid
        val result1 = enumSchema.validate(ujson.Str("test"), ValidationContext())
        assert(!result1.isValid)
        val result2 = enumSchema.validate(ujson.Str("test"), ValidationContext())
        assert(!result2.isValid)
        val result3 = enumSchema.validate(ujson.Null, ValidationContext())
        assert(!result3.isValid)
      }

      test("single value enum") {
        val enumSchema = EnumSchema.fromStrings("only")

        val result1 = enumSchema.validate(ujson.Str("only"), ValidationContext())
        assert(result1.isValid)
        val result2 = enumSchema.validate(ujson.Str("other"), ValidationContext())
        assert(!result2.isValid)

        assert(enumSchema.isStringEnum)
        assert(enumSchema.getStringValues.contains(List("only")))
      }

      test("duplicate values in enum") {
        val enumSchema = EnumSchema.fromStrings("red", "red", "blue")

        // Should still work (duplicates are allowed in JSON Schema)
        val result1 = enumSchema.validate(ujson.Str("red"), ValidationContext())
        assert(result1.isValid)
        val result2 = enumSchema.validate(ujson.Str("blue"), ValidationContext())
        assert(result2.isValid)
        val result3 = enumSchema.validate(ujson.Str("green"), ValidationContext())
        assert(!result3.isValid)
      }

      test("mixed factory with union type safety") {
        val enumSchema = EnumSchema.mixed("string", 42, true, 3.14)
        assert(enumSchema.enumValues.length == 4)
        val result1 = enumSchema.validate(ujson.Str("string"), ValidationContext())
        assert(result1.isValid)
        val result2 = enumSchema.validate(ujson.Num(42), ValidationContext())
        assert(result2.isValid)
        val result3 = enumSchema.validate(ujson.Bool(true), ValidationContext())
        assert(result3.isValid)
        val result4 = enumSchema.validate(ujson.Num(3.14), ValidationContext())
        assert(result4.isValid)
        val result5 = enumSchema.validate(ujson.Str("unsupported"), ValidationContext())
        assert(!result5.isValid)
      }
    }

    test("Integration with bl factory methods") {
      test("bl.StringEnum factory method") {
        val enumSchema = bl.StringEnum("red", "green", "blue")

        assert(enumSchema.isInstanceOf[EnumSchema])
        assert(enumSchema.isStringEnum)
        val result1 = enumSchema.validate(ujson.Str("red"), ValidationContext())
        assert(result1.isValid)
        val result2 = enumSchema.validate(ujson.Str("yellow"), ValidationContext())
        assert(!result2.isValid)
      }

      test("bl.StringEnum with List") {
        val colors = List("red", "green", "blue")
        val enumSchema = bl.StringEnum(colors)

        assert(enumSchema.isInstanceOf[EnumSchema])
        assert(enumSchema.getStringValues.contains(colors))
      }

      test("bl.MixedEnum factory method") {
        val enumSchema = bl.MixedEnum(
          ujson.Str("active"),
          ujson.Num(1),
          ujson.True
        )

        assert(enumSchema.isInstanceOf[EnumSchema])
        assert(!enumSchema.isStringEnum)
        val result1 = enumSchema.validate(ujson.Str("active"), ValidationContext())
        assert(result1.isValid)
        val result2 = enumSchema.validate(ujson.Num(1), ValidationContext())
        assert(result2.isValid)
        val result3 = enumSchema.validate(ujson.True, ValidationContext())
        assert(result3.isValid)
      }
    }

    test("Real world enum scenarios") {
      test("HTTP status codes") {
        val statusEnum = EnumSchema.fromInts(200, 201, 400, 401, 404, 500)

        val result1 = statusEnum.validate(ujson.Num(200), ValidationContext())
        assert(result1.isValid)
        val result2 = statusEnum.validate(ujson.Num(404), ValidationContext())
        assert(result2.isValid)
        val result3 = statusEnum.validate(ujson.Num(999), ValidationContext())
        assert(!result3.isValid)

        val types = statusEnum.getValueTypes
        assert(types.contains("number"))
        assert(types.size == 1)
      }

      test("Log levels with mixed types") {
        val logLevelEnum = EnumSchema.mixed("DEBUG", "INFO", "WARN", "ERROR", 0, 1, 2, 3)

        // String log levels
        val result1 = logLevelEnum.validate(ujson.Str("DEBUG"), ValidationContext())
        assert(result1.isValid)
        val result2 = logLevelEnum.validate(ujson.Str("INFO"), ValidationContext())
        assert(result2.isValid)

        // Numeric log levels
        val result3 = logLevelEnum.validate(ujson.Num(0), ValidationContext())
        assert(result3.isValid)
        val result4 = logLevelEnum.validate(ujson.Num(3), ValidationContext())
        assert(result4.isValid)

        // Invalid values
        val result5 = logLevelEnum.validate(ujson.Str("TRACE"), ValidationContext())
        assert(!result5.isValid)
        val result6 = logLevelEnum.validate(ujson.Num(5), ValidationContext())
        assert(!result6.isValid)

        val types = logLevelEnum.getValueTypes
        assert(types.contains("string"))
        assert(types.contains("number"))
        assert(types.size == 2)
      }

      test("Feature flags") {
        val featureFlagEnum = EnumSchema.fromBooleans(true, false)

        val result1 = featureFlagEnum.validate(ujson.Bool(true), ValidationContext())
        assert(result1.isValid)
        val result2 = featureFlagEnum.validate(ujson.Bool(false), ValidationContext())
        assert(result2.isValid)

        // Should reject other types
        val result3 = featureFlagEnum.validate(ujson.Str("true"), ValidationContext())
        assert(!result3.isValid)
        val result4 = featureFlagEnum.validate(ujson.Num(1), ValidationContext())
        assert(!result4.isValid)
      }
    }
  }
}
