package chez.primitives

import utest.*
import upickle.default.*
import chez.primitives.EnumChez

object EnumChezTests extends TestSuite {

  val tests = Tests {
    test("String enum creation and validation") {
      test("fromStrings factory method") {
        val enumSchema = EnumChez.fromStrings("red", "green", "blue")

        assert(enumSchema.isStringEnum)
        assert(enumSchema.getStringValues.contains(List("red", "green", "blue")))

        // Valid string validation
        assert(enumSchema.validateString("red").isEmpty)
        assert(enumSchema.validateString("green").isEmpty)
        assert(enumSchema.validateString("blue").isEmpty)

        // Invalid string validation
        assert(enumSchema.validateString("yellow").nonEmpty)
        assert(enumSchema.validateString("").nonEmpty)
      }

      test("fromStrings with List") {
        val colors = List("red", "green", "blue")
        val enumSchema = EnumChez.fromStrings(colors)

        assert(enumSchema.isStringEnum)
        assert(enumSchema.getStringValues.contains(colors))
      }
    }

    test("Mixed enum creation and validation") {
      test("mixed factory method") {
        val enumSchema = EnumChez.mixed("active", 1, true, null)

        assert(!enumSchema.isStringEnum)
        assert(enumSchema.getStringValues.isEmpty)

        // Valid mixed validation
        assert(enumSchema.validateString("active").isEmpty)
        assert(enumSchema.validateInt(1).isEmpty)
        assert(enumSchema.validateBoolean(true).isEmpty)
        assert(enumSchema.validateNull().isEmpty)

        // Invalid validation
        assert(enumSchema.validateString("inactive").nonEmpty)
        assert(enumSchema.validateInt(2).nonEmpty)
        assert(enumSchema.validateBoolean(false).nonEmpty)
      }

      test("fromValues with ujson.Value") {
        val enumSchema = EnumChez.fromValues(
          ujson.Str("hello"),
          ujson.Num(42),
          ujson.True,
          ujson.Null
        )

        assert(!enumSchema.isStringEnum)

        // Validate using ujson values directly
        assert(enumSchema.validate(ujson.Str("hello")).isEmpty)
        assert(enumSchema.validate(ujson.Num(42)).isEmpty)
        assert(enumSchema.validate(ujson.True).isEmpty)
        assert(enumSchema.validate(ujson.Null).isEmpty)

        // Invalid values
        assert(enumSchema.validate(ujson.Str("world")).nonEmpty)
        assert(enumSchema.validate(ujson.Num(43)).nonEmpty)
        assert(enumSchema.validate(ujson.False).nonEmpty)
      }
    }

    test("Numeric enum creation") {
      test("fromNumbers factory method") {
        val enumSchema = EnumChez.fromNumbers(1.0, 2.5, 3.14)

        assert(!enumSchema.isStringEnum)

        // Valid numeric validation
        assert(enumSchema.validateNumber(1.0).isEmpty)
        assert(enumSchema.validateNumber(2.5).isEmpty)
        assert(enumSchema.validateNumber(3.14).isEmpty)

        // Invalid validation
        assert(enumSchema.validateNumber(4.0).nonEmpty)
        assert(enumSchema.validateString("1.0").nonEmpty)
      }

      test("fromInts factory method") {
        val enumSchema = EnumChez.fromInts(1, 2, 3)

        assert(!enumSchema.isStringEnum)

        // Valid integer validation (converted to double in ujson)
        assert(enumSchema.validateInt(1).isEmpty)
        assert(enumSchema.validateInt(2).isEmpty)
        assert(enumSchema.validateInt(3).isEmpty)

        // Invalid validation
        assert(enumSchema.validateInt(4).nonEmpty)
      }
    }

    test("Boolean enum creation") {
      test("fromBooleans factory method") {
        val enumSchema = EnumChez.fromBooleans(true, false)

        assert(!enumSchema.isStringEnum)

        // Valid boolean validation
        assert(enumSchema.validateBoolean(true).isEmpty)
        assert(enumSchema.validateBoolean(false).isEmpty)

        // Type checking
        val types = enumSchema.getValueTypes
        assert(types.contains("boolean"))
        assert(types.size == 1)
      }

      test("single boolean value") {
        val enumSchema = EnumChez.fromBooleans(true)

        assert(enumSchema.validateBoolean(true).isEmpty)
        assert(enumSchema.validateBoolean(false).nonEmpty)
      }
    }

    test("JSON Schema generation") {
      test("string enum schema") {
        val enumSchema = EnumChez.fromStrings("red", "green", "blue")
        val jsonSchema = enumSchema.toJsonSchema

        // Should have type: string for string enums
        assert(jsonSchema("type").str == "string")

        // Should have enum values
        assert(jsonSchema.obj.contains("enum"))
        val enumValues = jsonSchema("enum").arr.map(_.str).toSet
        assert(enumValues == Set("red", "green", "blue"))
      }

      test("mixed enum schema") {
        val enumSchema = EnumChez.mixed("active", 1, true)
        val jsonSchema = enumSchema.toJsonSchema

        // Should NOT have type field for mixed enums
        assert(!jsonSchema.obj.contains("type"))

        // Should have enum values
        assert(jsonSchema.obj.contains("enum"))
        val enumArray = jsonSchema("enum").arr
        assert(enumArray.length == 3)
      }

      test("schema with metadata") {
        val enumSchema = EnumChez
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
      test("validateAtPath provides correct error paths") {
        val enumSchema = EnumChez.fromStrings("red", "green", "blue")

        val errors = enumSchema.validateAtPath(ujson.Str("yellow"), "/properties/color")
        assert(errors.nonEmpty)

        val error = errors.head
        assert(error.isInstanceOf[chez.ValidationError.TypeMismatch])

        val typeMismatch = error.asInstanceOf[chez.ValidationError.TypeMismatch]
        assert(typeMismatch.path == "/properties/color")
        assert(typeMismatch.actual.contains("yellow"))
        assert(typeMismatch.expected.contains("red"))
      }
    }

    test("Type validation and introspection") {
      test("isValidType checks") {
        val enumSchema = EnumChez.mixed("hello", 42, true, null)

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
        val enumSchema = EnumChez.mixed("hello", 42, true, null)
        val types = enumSchema.getValueTypes

        assert(types.contains("string"))
        assert(types.contains("number"))
        assert(types.contains("boolean"))
        assert(types.contains("null"))
        assert(types.size == 4)
      }

      test("string-only enum type information") {
        val enumSchema = EnumChez.fromStrings("red", "green", "blue")
        val types = enumSchema.getValueTypes

        assert(types.contains("string"))
        assert(types.size == 1)
      }
    }

    test("Validator helper function") {
      test("creates reusable validator") {
        val validator = EnumChez.validator(
          ujson.Str("active"),
          ujson.Str("inactive"),
          ujson.Str("pending")
        )

        // Valid values
        assert(validator(ujson.Str("active")).isEmpty)
        assert(validator(ujson.Str("inactive")).isEmpty)
        assert(validator(ujson.Str("pending")).isEmpty)

        // Invalid values
        assert(validator(ujson.Str("unknown")).nonEmpty)
        assert(validator(ujson.Num(1)).nonEmpty)
      }
    }

    test("Edge cases and error conditions") {
      test("empty enum values") {
        val enumSchema = EnumChez(List.empty)

        // Any value should be invalid
        assert(enumSchema.validateString("test").nonEmpty)
        assert(enumSchema.validate(ujson.Str("test")).nonEmpty)
        assert(enumSchema.validate(ujson.Null).nonEmpty)
      }

      test("single value enum") {
        val enumSchema = EnumChez.fromStrings("only")

        assert(enumSchema.validateString("only").isEmpty)
        assert(enumSchema.validateString("other").nonEmpty)

        assert(enumSchema.isStringEnum)
        assert(enumSchema.getStringValues.contains(List("only")))
      }

      test("duplicate values in enum") {
        val enumSchema = EnumChez.fromStrings("red", "red", "blue")

        // Should still work (duplicates are allowed in JSON Schema)
        assert(enumSchema.validateString("red").isEmpty)
        assert(enumSchema.validateString("blue").isEmpty)
        assert(enumSchema.validateString("green").nonEmpty)
      }

      test("mixed factory with union type safety") {
        val enumSchema = EnumChez.mixed("string", 42, true, 3.14)
        assert(enumSchema.enumValues.length == 4)
        assert(enumSchema.validateString("string").isEmpty)
        assert(enumSchema.validateInt(42).isEmpty)
        assert(enumSchema.validateBoolean(true).isEmpty)
        assert(enumSchema.validateNumber(3.14).isEmpty)
        assert(enumSchema.validateString("unsupported").nonEmpty)
      }
    }

    test("Integration with Chez factory methods") {
      test("Chez.StringEnum factory method") {
        val enumSchema = chez.Chez.StringEnum("red", "green", "blue")

        assert(enumSchema.isInstanceOf[EnumChez])
        assert(enumSchema.isStringEnum)
        assert(enumSchema.validateString("red").isEmpty)
        assert(enumSchema.validateString("yellow").nonEmpty)
      }

      test("Chez.StringEnum with List") {
        val colors = List("red", "green", "blue")
        val enumSchema = chez.Chez.StringEnum(colors)

        assert(enumSchema.isInstanceOf[EnumChez])
        assert(enumSchema.getStringValues.contains(colors))
      }

      test("Chez.MixedEnum factory method") {
        val enumSchema = chez.Chez.MixedEnum(
          ujson.Str("active"),
          ujson.Num(1),
          ujson.True
        )

        assert(enumSchema.isInstanceOf[EnumChez])
        assert(!enumSchema.isStringEnum)
        assert(enumSchema.validate(ujson.Str("active")).isEmpty)
        assert(enumSchema.validate(ujson.Num(1)).isEmpty)
        assert(enumSchema.validate(ujson.True).isEmpty)
      }
    }

    test("Real world enum scenarios") {
      test("HTTP status codes") {
        val statusEnum = EnumChez.fromInts(200, 201, 400, 401, 404, 500)

        assert(statusEnum.validateInt(200).isEmpty)
        assert(statusEnum.validateInt(404).isEmpty)
        assert(statusEnum.validateInt(999).nonEmpty)

        val types = statusEnum.getValueTypes
        assert(types.contains("number"))
        assert(types.size == 1)
      }

      test("Log levels with mixed types") {
        val logLevelEnum = EnumChez.mixed("DEBUG", "INFO", "WARN", "ERROR", 0, 1, 2, 3)

        // String log levels
        assert(logLevelEnum.validateString("DEBUG").isEmpty)
        assert(logLevelEnum.validateString("INFO").isEmpty)

        // Numeric log levels
        assert(logLevelEnum.validateInt(0).isEmpty)
        assert(logLevelEnum.validateInt(3).isEmpty)

        // Invalid values
        assert(logLevelEnum.validateString("TRACE").nonEmpty)
        assert(logLevelEnum.validateInt(5).nonEmpty)

        val types = logLevelEnum.getValueTypes
        assert(types.contains("string"))
        assert(types.contains("number"))
        assert(types.size == 2)
      }

      test("Feature flags") {
        val featureFlagEnum = EnumChez.fromBooleans(true, false)

        assert(featureFlagEnum.validateBoolean(true).isEmpty)
        assert(featureFlagEnum.validateBoolean(false).isEmpty)

        // Should reject other types
        assert(featureFlagEnum.validateString("true").nonEmpty)
        assert(featureFlagEnum.validateInt(1).nonEmpty)
      }
    }
  }
}
