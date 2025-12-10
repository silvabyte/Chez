package boogieloops.schema.complex

import utest.*
import boogieloops.schema.*
import boogieloops.schema.primitives.*
import boogieloops.schema.complex.*
import boogieloops.schema.validation.*

object ObjectSchemaTests extends TestSuite {

  val tests = Tests {
    test("json schema generation") {
      test("empty object schema") {
        val schema = ObjectSchema()
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(!json.obj.contains("properties") || json("properties").obj.isEmpty)
        assert(!json.obj.contains("required"))
      }

      test("basic object with properties") {
        val schema = ObjectSchema(properties = {
          Map(
            "name" -> StringSchema(),
            "age" -> IntegerSchema(),
            "active" -> BooleanSchema()
          )
        })
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(json("properties")("name")("type").str == "string")
        assert(json("properties")("age")("type").str == "integer")
        assert(json("properties")("active")("type").str == "boolean")
      }

      test("object with required fields") {
        val schema = ObjectSchema(
          properties = Map("name" -> StringSchema(), "age" -> IntegerSchema()),
          required = Set("name")
        )
        val json = schema.toJsonSchema

        assert(json("required").arr.map(_.str).toSet == Set("name"))
      }

      test("object with property count constraints") {
        val schema = ObjectSchema(
          properties = Map("name" -> StringSchema()),
          minProperties = Some(1),
          maxProperties = Some(5)
        )
        val json = schema.toJsonSchema

        assert(json("minProperties").num == 1)
        assert(json("maxProperties").num == 5)
      }

      test("object with additionalProperties") {
        val schema = ObjectSchema(
          properties = Map("name" -> StringSchema()),
          additionalProperties = Some(false)
        )
        val json = schema.toJsonSchema

        assert(json("additionalProperties").bool == false)
      }

      test("object with metadata") {
        val schema = ObjectSchema(
          properties = Map("name" -> StringSchema())
        ).withTitle("User Object")
          .withDescription("A user profile object")
          .withDefault(ujson.Obj("name" -> "anonymous"))

        val json = schema.toJsonSchema

        assert(json("title").str == "User Object")
        assert(json("description").str == "A user profile object")
        assert(json("default")("name").str == "anonymous")
      }
    }

    test("advanced json schema 2020-12 features") {
      test("pattern properties") {
        val schema = ObjectSchema(
          patternProperties = Map(
            "^str_" -> StringSchema(),
            "^num_" -> IntegerSchema()
          )
        )
        val json = schema.toJsonSchema

        assert(json("patternProperties")("^str_")("type").str == "string")
        assert(json("patternProperties")("^num_")("type").str == "integer")
      }

      test("property names validation") {
        val schema = ObjectSchema(
          propertyNames = Some(StringSchema(pattern = Some("^[a-zA-Z]+$")))
        )
        val json = schema.toJsonSchema

        assert(json("propertyNames")("type").str == "string")
        assert(json("propertyNames")("pattern").str == "^[a-zA-Z]+$")
      }

      test("dependent required") {
        val schema = ObjectSchema(
          dependentRequired = Map(
            "billing_address" -> Set("first_name", "last_name")
          )
        )
        val json = schema.toJsonSchema

        val depRequired = json("dependentRequired")("billing_address").arr.map(_.str).toSet
        assert(depRequired == Set("first_name", "last_name"))
      }

      test("dependent schemas") {
        val schema = ObjectSchema(
          dependentSchemas = Map(
            "credit_card" -> ObjectSchema(properties = Map("number" -> StringSchema()))
          )
        )
        val json = schema.toJsonSchema

        assert(json("dependentSchemas")("credit_card")("type").str == "object")
        assert(
          json("dependentSchemas")("credit_card")("properties")("number")("type").str == "string"
        )
      }

      test("unevaluated properties") {
        val schema = ObjectSchema(
          unevaluatedProperties = Some(false)
        )
        val json = schema.toJsonSchema

        assert(json("unevaluatedProperties").bool == false)
      }
    }

    test("validation behavior") {
      test("valid object validation") {
        val schema = ObjectSchema(
          properties = Map("name" -> StringSchema()),
          required = Set("name"),
          minProperties = Some(1),
          maxProperties = Some(3)
        )
        val obj = ujson.Obj("name" -> "test", "optional" -> "value")
        val result = schema.validate(obj, ValidationContext())
        assert(result.isValid)
      }

      test("missing required field validation") {
        val schema = ObjectSchema(
          properties = Map("name" -> StringSchema()),
          additionalProperties = Some(false)
        )
        val obj = ujson.Obj("age" -> 25)
        val result = schema.validate(obj, ValidationContext())
        assert(result.errors.length == 1)
          assert(result.errors.head.isInstanceOf[boogieloops.schema.ValidationError.MissingField])
      }

      test("min properties validation") {
        val schema = ObjectSchema(minProperties = Some(2))
        val obj = ujson.Obj("name" -> "test")
        val result = schema.validate(obj, ValidationContext())
        assert(result.errors.length == 1)
        assert(result.errors.head.isInstanceOf[boogieloops.schema.ValidationError.MinPropertiesViolation])
      }

      test("max properties validation") {
        val schema = ObjectSchema(maxProperties = Some(1))
        val obj = ujson.Obj("name" -> "test", "age" -> 25)
        val result = schema.validate(obj, ValidationContext())
        assert(result.errors.length == 1)
        assert(result.errors.head.isInstanceOf[boogieloops.schema.ValidationError.MaxPropertiesViolation])
      }

      test("additional properties validation") {
        val schema = ObjectSchema(
          properties = Map("name" -> StringSchema()),
          additionalProperties = Some(false)
        )
        val obj = ujson.Obj("name" -> "test", "unknown" -> "value")
        val result = schema.validate(obj, ValidationContext())
        assert(result.errors.length == 1)
        assert(result.errors.head.isInstanceOf[boogieloops.schema.ValidationError.AdditionalProperty])
      }

      test("multiple validation errors") {
        val schema = ObjectSchema(
          properties = Map("name" -> StringSchema()),
          required = Set("name")
        )
        val obj = ujson.Obj("unknown" -> "value")
        val result = schema.validate(obj, ValidationContext())
        assert(result.errors.length >= 3) // missing fields + min properties + additional property
      }
    }

    test("nested object schemas") {
      test("object with nested object properties") {
        val addressSchema = ObjectSchema(properties = {
          Map(
            "street" -> StringSchema(),
            "city" -> StringSchema(),
            "zipCode" -> StringSchema()
          )
        })
        val userSchema = ObjectSchema(properties = {
          Map(
            "name" -> StringSchema(),
            "address" -> addressSchema
          )
        })
        val json = userSchema.toJsonSchema

        assert(json("type").str == "object")
        assert(json("properties")("address")("type").str == "object")
        assert(json("properties")("address")("properties")("street")("type").str == "string")
      }

      test("deeply nested objects") {
        val level3 = ObjectSchema(properties = Map("value" -> StringSchema()))
        val level2 = ObjectSchema(properties = Map("level3" -> level3))
        val level1 = ObjectSchema(properties = Map("level2" -> level2))
        val json = level1.toJsonSchema

        assert(json("properties")("level2")("properties")("level3")("properties")("value")(
          "type"
        ).str == "string")
      }
    }

    test("real world object patterns") {
      test("user profile schema") {
        val schema = ObjectSchema(
          properties = Map(
            "id" -> StringSchema(format = Some("uuid")),
            "username" -> StringSchema(minLength = Some(3), maxLength = Some(20)),
            "email" -> StringSchema(format = Some("email")),
            "age" -> IntegerSchema(minimum = Some(0), maximum = Some(150)),
            "preferences" -> ObjectSchema(additionalProperties = Some(true))
          ),
          required = Set("id", "username", "email")
        )
        val json = schema.toJsonSchema

        assert(json("properties")("id")("format").str == "uuid")
        assert(json("properties")("email")("format").str == "email")
        assert(json("required").arr.map(_.str).toSet == Set("id", "username", "email"))
      }

      test("api configuration schema") {
        val schema = ObjectSchema(
          properties = Map(
            "apiKey" -> StringSchema(pattern = Some("^[A-Za-z0-9]{32}$")),
            "endpoints" -> ObjectSchema(
              patternProperties = Map(".*" -> StringSchema(format = Some("uri")))
            ),
            "retryCount" -> IntegerSchema(minimum = Some(0), maximum = Some(10)),
            "timeout" -> IntegerSchema(minimum = Some(1000))
          ),
          required = Set("apiKey")
        )
        val json = schema.toJsonSchema

        assert(json("properties")("apiKey")("pattern").str == "^[A-Za-z0-9]{32}$")
        assert(json("properties")("endpoints")("patternProperties")(".*")("format").str == "uri")
      }
    }

    test("edge cases and error conditions") {
      test("empty required set") {
        val schema = ObjectSchema(
          properties = Map("optional" -> StringSchema()),
          required = Set.empty
        )
        val json = schema.toJsonSchema

        assert(!json.obj.contains("required"))
      }

      test("required field not in properties") {
        val schema = ObjectSchema(
          properties = Map("name" -> StringSchema()),
          additionalProperties = Some(false)
        )
        val json = schema.toJsonSchema

        // Should still include the required field even if not in properties
        assert(json("required").arr.map(_.str).toSet == Set("name", "missing"))
      }

      test("zero property constraints") {
        val schema = ObjectSchema(
          minProperties = Some(0),
          maxProperties = Some(0)
        )
        val json = schema.toJsonSchema

        assert(json("minProperties").num == 0)
        assert(json("maxProperties").num == 0)
      }

      test("conflicting property constraints") {
        val schema = ObjectSchema(
          minProperties = Some(5),
          maxProperties = Some(2)
        )
        val json = schema.toJsonSchema

        // Schema should serialize even with conflicting constraints
        assert(json("minProperties").num == 5)
        assert(json("maxProperties").num == 2)
      }
    }

    test("examples serialization") {
      test("object with examples using withExamples") {
        val schema = ObjectSchema(
          properties = Map("name" -> StringSchema(), "age" -> IntegerSchema())
        ).withExamples(
          ujson.Obj("name" -> "John", "age" -> 30),
          ujson.Obj("name" -> "Jane", "age" -> 25)
        )
        val json = schema.toJsonSchema

        assert(json.obj.contains("examples"))
        val examples = json("examples").arr
        assert(examples.length == 2)
        assert(examples(0)("name").str == "John")
        assert(examples(0)("age").num == 30)
        assert(examples(1)("name").str == "Jane")
        assert(examples(1)("age").num == 25)
      }

      test("object examples serialize as valid JSON array") {
        val schema = ObjectSchema(
          properties = Map("id" -> StringSchema(), "active" -> BooleanSchema())
        ).withExamples(
          ujson.Obj("id" -> "1", "active" -> true),
          ujson.Obj("id" -> "2", "active" -> false)
        )
        val json = schema.toJsonSchema

        // Verify examples is a proper JSON array
        assert(json("examples").isInstanceOf[ujson.Arr])
        val examples = json("examples").arr
        assert(examples.length == 2)

        // Verify each example is an object
        assert(examples(0).isInstanceOf[ujson.Obj])
        assert(examples(1).isInstanceOf[ujson.Obj])

        // Verify content
        assert(examples(0)("id").str == "1")
        assert(examples(0)("active").bool == true)
        assert(examples(1)("id").str == "2")
        assert(examples(1)("active").bool == false)
      }

      test("withExamples calls work without errors") {
        // This test ensures withExamples() doesn't throw runtime errors
        val schema1 = ObjectSchema().withExamples(ujson.Obj("test" -> "value"))
        val schema2 = ObjectSchema(
          properties = Map("name" -> StringSchema())
        ).withExamples(
          ujson.Obj("name" -> "test1"),
          ujson.Obj("name" -> "test2")
        )

        // If we get here without exceptions, the test passes
        assert(schema1.toJsonSchema("examples").arr.length == 1)
        assert(schema2.toJsonSchema("examples").arr.length == 2)
      }
    }

    test("ujson.Value validation with ValidationContext") {
      test("ObjectSchema validates ujson.Obj correctly") {
        val schema = ObjectSchema(
          properties = Map(
            "name" -> StringSchema(minLength = Some(1)),
            "age" -> IntegerSchema(minimum = Some(0))
          ),
          required = Set("name")
        )

        test("valid object") {
          val validObj = ujson.Obj(
            "name" -> ujson.Str("Alice"),
            "age" -> ujson.Num(25)
          )
          val result = schema.validate(validObj: ujson.Value, ValidationContext())
          assert(result.isValid)
          assert(result.errors.isEmpty)
        }

        test("missing required field") {
          val invalidObj = ujson.Obj("age" -> ujson.Num(25))
          val result = schema.validate(invalidObj: ujson.Value, ValidationContext())
          assert(!result.isValid)
          assert(result.errors.length == 1)
        assert(result.errors.head.isInstanceOf[boogieloops.schema.ValidationError.MissingField])
        }

        test("property validation failure") {
          val invalidObj = ujson.Obj(
            "name" -> ujson.Str(""), // violates minLength
            "age" -> ujson.Num(-5) // violates minimum
          )
          val result = schema.validate(invalidObj: ujson.Value, ValidationContext())
          assert(!result.isValid)
          assert(result.errors.length == 2)
        }
      }

      test("ObjectSchema rejects non-object ujson.Value types") {
        val schema = ObjectSchema(properties = Map("name" -> StringSchema()))

        test("string value") {
          val result = schema.validate(ujson.Str("not an object"): ujson.Value, ValidationContext())
          assert(!result.isValid)
          assert(result.errors.length == 1)
          val error = result.errors.head.asInstanceOf[boogieloops.schema.ValidationError.TypeMismatch]
          assert(error.expected == "object")
          assert(error.actual == "string")
        }

        test("number value") {
          val result = schema.validate(ujson.Num(42): ujson.Value, ValidationContext())
          assert(!result.isValid)
          val error = result.errors.head.asInstanceOf[boogieloops.schema.ValidationError.TypeMismatch]
          assert(error.expected == "object")
          assert(error.actual == "number")
        }

        test("array value") {
          val result =
            schema.validate(ujson.Arr(ujson.Str("test")): ujson.Value, ValidationContext())
          assert(!result.isValid)
          val error = result.errors.head.asInstanceOf[boogieloops.schema.ValidationError.TypeMismatch]
          assert(error.expected == "object")
          assert(error.actual == "array")
        }
      }
    }

    test("Property validation with error path tracking") {
      test("nested object error paths") {
        val addressSchema = ObjectSchema(
          properties = Map(
            "street" -> StringSchema(minLength = Some(1)),
            "city" -> StringSchema(minLength = Some(1))
          ),
          required = Set("street", "city")
        )

        val userSchema = ObjectSchema(
          properties = Map(
            "name" -> StringSchema(minLength = Some(1)),
            "address" -> addressSchema
          ),
          required = Set("name", "address")
        )

        test("valid nested object") {
          val validUser = ujson.Obj(
            "name" -> ujson.Str("John"),
            "address" -> ujson.Obj(
              "street" -> ujson.Str("123 Main St"),
              "city" -> ujson.Str("Boston")
            )
          )
          val result = userSchema.validate(validUser: ujson.Value, ValidationContext())
          assert(result.isValid)
        }

        test("nested property validation error with correct path") {
          val invalidUser = ujson.Obj(
            "name" -> ujson.Str("John"),
            "address" -> ujson.Obj(
              "street" -> ujson.Str(""), // empty street violates minLength
              "city" -> ujson.Str("Boston")
            )
          )
          val result = userSchema.validate(invalidUser: ujson.Value, ValidationContext())
          assert(!result.isValid)

          val streetError = result.errors.find {
            case boogieloops.schema.ValidationError.MinLengthViolation(_, _, path) => path == "/address/street"
            case _ => false
          }
          assert(streetError.isDefined)
        }

        test("missing nested required field with correct path") {
          val invalidUser = ujson.Obj(
            "name" -> ujson.Str("John"),
            "address" -> ujson.Obj(
              "street" -> ujson.Str("123 Main St")
              // missing city
            )
          )
          val result = userSchema.validate(invalidUser: ujson.Value, ValidationContext())
          assert(!result.isValid)

          val missingFieldError = result.errors.find {
            case boogieloops.schema.ValidationError.MissingField(field, path) => field == "city" && path == "/address"
            case _ => false
          }
          assert(missingFieldError.isDefined)
        }
      }

      test("custom context path") {
        val schema = ObjectSchema(
          properties = Map("name" -> StringSchema(minLength = Some(5))),
          required = Set("name")
        )

        val context = ValidationContext("/api/users/123")
        val invalidObj = ujson.Obj("name" -> ujson.Str("hi")) // too short
        val result = schema.validate(invalidObj: ujson.Value, context)

        assert(!result.isValid)
          val error = result.errors.head.asInstanceOf[boogieloops.schema.ValidationError.MinLengthViolation]
        assert(error.path == "/api/users/123/name")
      }
    }

    test("Pattern properties validation") {
      test("pattern properties matching and validation") {
        val schema = ObjectSchema(
          properties = Map("name" -> StringSchema()),
          patternProperties = Map(
            "^str_" -> StringSchema(minLength = Some(3)),
            "^num_" -> IntegerSchema(minimum = Some(0))
          )
        )

        test("valid pattern properties") {
          val obj = ujson.Obj(
            "name" -> ujson.Str("Alice"),
            "str_field" -> ujson.Str("hello"),
            "num_field" -> ujson.Num(42)
          )
          val result = schema.validate(obj: ujson.Value, ValidationContext())
          assert(result.isValid)
        }

        test("pattern property validation failure") {
          val obj = ujson.Obj(
            "name" -> ujson.Str("Alice"),
            "str_field" -> ujson.Str("hi"), // too short for minLength=3
            "num_field" -> ujson.Num(-5) // below minimum=0
          )
          val result = schema.validate(obj: ujson.Value, ValidationContext())
          assert(!result.isValid)
          assert(result.errors.length == 2)

          val hasStrError = result.errors.exists {
            case boogieloops.schema.ValidationError.MinLengthViolation(_, _, path) => path == "/str_field"
            case _ => false
          }
          assert(hasStrError)

          val hasNumError = result.errors.exists {
            case boogieloops.schema.ValidationError.OutOfRange(_, _, _, path) => path == "/num_field"
            case _ => false
          }
          assert(hasNumError)
        }
      }
    }

    test("Additional properties validation") {
      test("additional properties with schema validation") {
        val schema = ObjectSchema(
          properties = Map("name" -> StringSchema()),
          additionalPropertiesSchema = Some(StringSchema(minLength = Some(2)))
        )

        test("valid additional properties") {
          val obj = ujson.Obj(
            "name" -> ujson.Str("Alice"),
            "extra1" -> ujson.Str("hello"),
            "extra2" -> ujson.Str("world")
          )
          val result = schema.validate(obj: ujson.Value, ValidationContext())
          assert(result.isValid)
        }

        test("invalid additional properties") {
          val obj = ujson.Obj(
            "name" -> ujson.Str("Alice"),
            "extra1" -> ujson.Str("h"), // too short for minLength=2
            "extra2" -> ujson.Str("ok")
          )
          val result = schema.validate(obj: ujson.Value, ValidationContext())
          assert(!result.isValid)
          assert(result.errors.length == 1)

        val error = result.errors.head.asInstanceOf[boogieloops.schema.ValidationError.MinLengthViolation]
          assert(error.path == "/extra1")
        }
      }

      test("additional properties false with pattern properties") {
        val schema = ObjectSchema(
          properties = Map("name" -> StringSchema()),
          patternProperties = Map("^allowed_" -> StringSchema()),
          additionalProperties = Some(false)
        )

        test("pattern properties are allowed") {
          val obj = ujson.Obj(
            "name" -> ujson.Str("Alice"),
            "allowed_field" -> ujson.Str("ok")
          )
          val result = schema.validate(obj: ujson.Value, ValidationContext())
          assert(result.isValid)
        }

        test("non-pattern additional properties are rejected") {
          val obj = ujson.Obj(
            "name" -> ujson.Str("Alice"),
            "allowed_field" -> ujson.Str("ok"),
            "forbidden_field" -> ujson.Str("not allowed")
          )
          val result = schema.validate(obj: ujson.Value, ValidationContext())
          assert(!result.isValid)

          val additionalPropError = result.errors.find {
            case boogieloops.schema.ValidationError.AdditionalProperty(prop, path) => prop == "forbidden_field"
            case _ => false
          }
          assert(additionalPropError.isDefined)
        }
      }
    }

    test("ValidationResult framework integration") {
      test("ValidationResult.valid() for successful validation") {
        val schema = ObjectSchema(properties = Map("name" -> StringSchema()))
        val obj = ujson.Obj("name" -> ujson.Str("test"))
        val result = schema.validate(obj: ujson.Value, ValidationContext())

        assert(result.isValid)
        assert(result.errors.isEmpty)
        assert(result.isInstanceOf[ValidationResult.Valid.type])
      }

      test("ValidationResult.invalid() for failed validation") {
        val schema = ObjectSchema(
          properties = Map("name" -> StringSchema(minLength = Some(5))),
          required = Set("name", "age")
        )
        val obj = ujson.Obj("name" -> ujson.Str("hi")) // too short + missing age
        val result = schema.validate(obj: ujson.Value, ValidationContext())

        assert(!result.isValid)
        assert(result.errors.nonEmpty)
        assert(result.isInstanceOf[ValidationResult.Invalid])
        assert(result.errors.length == 2)
      }
    }

    test("Complex nested validation scenarios") {
      test("deeply nested objects with multiple constraint violations") {
        val level3Schema = ObjectSchema(
          properties = Map("value" -> StringSchema(minLength = Some(3))),
          required = Set("value")
        )
        val level2Schema = ObjectSchema(
          properties = Map("level3" -> level3Schema),
          required = Set("level3")
        )
        val level1Schema = ObjectSchema(
          properties = Map("level2" -> level2Schema),
          required = Set("level2")
        )

        test("valid deeply nested structure") {
          val validObj = ujson.Obj(
            "level2" -> ujson.Obj(
              "level3" -> ujson.Obj(
                "value" -> ujson.Str("hello")
              )
            )
          )
          val result = level1Schema.validate(validObj: ujson.Value, ValidationContext())
          assert(result.isValid)
        }

        test("nested validation error with correct deep path") {
          val invalidObj = ujson.Obj(
            "level2" -> ujson.Obj(
              "level3" -> ujson.Obj(
                "value" -> ujson.Str("hi") // too short
              )
            )
          )
          val result = level1Schema.validate(invalidObj: ujson.Value, ValidationContext())
          assert(!result.isValid)

          val deepError = result.errors.find {
            case boogieloops.schema.ValidationError.MinLengthViolation(_, _, path) => path == "/level2/level3/value"
            case _ => false
          }
          assert(deepError.isDefined)
        }
      }
    }
  }
}
