package chez.complex

import utest.*
import chez.*
import chez.primitives.*
import chez.complex.*
import upickle.default.*

object ObjectChezTests extends TestSuite {

  val tests = Tests {
    test("json schema generation") {
      test("empty object schema") {
        val schema = ObjectChez()
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(!json.obj.contains("properties") || json("properties").obj.isEmpty)
        assert(!json.obj.contains("required"))
      }

      test("basic object with properties") {
        val schema = ObjectChez(properties = Map(
          "name" -> StringChez(),
          "age" -> IntegerChez(),
          "active" -> BooleanChez()
        ))
        val json = schema.toJsonSchema

        assert(json("type").str == "object")
        assert(json("properties")("name")("type").str == "string")
        assert(json("properties")("age")("type").str == "integer")
        assert(json("properties")("active")("type").str == "boolean")
      }

      test("object with required fields") {
        val schema = ObjectChez(
          properties = Map("name" -> StringChez(), "age" -> IntegerChez()),
          required = Set("name")
        )
        val json = schema.toJsonSchema

        assert(json("required").arr.map(_.str).toSet == Set("name"))
      }

      test("object with property count constraints") {
        val schema = ObjectChez(
          properties = Map("name" -> StringChez()),
          minProperties = Some(1),
          maxProperties = Some(5)
        )
        val json = schema.toJsonSchema

        assert(json("minProperties").num == 1)
        assert(json("maxProperties").num == 5)
      }

      test("object with additionalProperties") {
        val schema = ObjectChez(
          properties = Map("name" -> StringChez()),
          additionalProperties = Some(false)
        )
        val json = schema.toJsonSchema

        assert(json("additionalProperties").bool == false)
      }

      test("object with metadata") {
        val schema = ObjectChez(
          properties = Map("name" -> StringChez())
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
        val schema = ObjectChez(
          patternProperties = Map(
            "^str_" -> StringChez(),
            "^num_" -> IntegerChez()
          )
        )
        val json = schema.toJsonSchema

        assert(json("patternProperties")("^str_")("type").str == "string")
        assert(json("patternProperties")("^num_")("type").str == "integer")
      }

      test("property names validation") {
        val schema = ObjectChez(
          propertyNames = Some(StringChez(pattern = Some("^[a-zA-Z]+$")))
        )
        val json = schema.toJsonSchema

        assert(json("propertyNames")("type").str == "string")
        assert(json("propertyNames")("pattern").str == "^[a-zA-Z]+$")
      }

      test("dependent required") {
        val schema = ObjectChez(
          dependentRequired = Map(
            "billing_address" -> Set("first_name", "last_name")
          )
        )
        val json = schema.toJsonSchema

        val depRequired = json("dependentRequired")("billing_address").arr.map(_.str).toSet
        assert(depRequired == Set("first_name", "last_name"))
      }

      test("dependent schemas") {
        val schema = ObjectChez(
          dependentSchemas = Map(
            "credit_card" -> ObjectChez(properties = Map("number" -> StringChez()))
          )
        )
        val json = schema.toJsonSchema

        assert(json("dependentSchemas")("credit_card")("type").str == "object")
        assert(json("dependentSchemas")("credit_card")("properties")("number")("type").str == "string")
      }

      test("unevaluated properties") {
        val schema = ObjectChez(
          unevaluatedProperties = Some(false)
        )
        val json = schema.toJsonSchema

        assert(json("unevaluatedProperties").bool == false)
      }
    }

    test("validation behavior") {
      test("valid object validation") {
        val schema = ObjectChez(
          properties = Map("name" -> StringChez()),
          required = Set("name"),
          minProperties = Some(1),
          maxProperties = Some(3)
        )
        val obj = ujson.Obj("name" -> "test", "optional" -> "value")
        val errors = schema.validate(obj)
        assert(errors.isEmpty)
      }

      test("missing required field validation") {
        val schema = ObjectChez(
          properties = Map("name" -> StringChez()),
          required = Set("name")
        )
        val obj = ujson.Obj("age" -> 25)
        val errors = schema.validate(obj)
        assert(errors.length == 1)
        assert(errors.head.isInstanceOf[chez.ValidationError.MissingField])
      }

      test("min properties validation") {
        val schema = ObjectChez(minProperties = Some(2))
        val obj = ujson.Obj("name" -> "test")
        val errors = schema.validate(obj)
        assert(errors.length == 1)
        assert(errors.head.isInstanceOf[chez.ValidationError.MinPropertiesViolation])
      }

      test("max properties validation") {
        val schema = ObjectChez(maxProperties = Some(1))
        val obj = ujson.Obj("name" -> "test", "age" -> 25)
        val errors = schema.validate(obj)
        assert(errors.length == 1)
        assert(errors.head.isInstanceOf[chez.ValidationError.MaxPropertiesViolation])
      }

      test("additional properties validation") {
        val schema = ObjectChez(
          properties = Map("name" -> StringChez()),
          additionalProperties = Some(false)
        )
        val obj = ujson.Obj("name" -> "test", "unknown" -> "value")
        val errors = schema.validate(obj)
        assert(errors.length == 1)
        assert(errors.head.isInstanceOf[chez.ValidationError.AdditionalProperty])
      }

      test("multiple validation errors") {
        val schema = ObjectChez(
          properties = Map("name" -> StringChez()),
          required = Set("name", "age"),
          minProperties = Some(3),
          additionalProperties = Some(false)
        )
        val obj = ujson.Obj("unknown" -> "value")
        val errors = schema.validate(obj)
        assert(errors.length >= 3) // missing fields + min properties + additional property
      }
    }

    test("nested object schemas") {
      test("object with nested object properties") {
        val addressSchema = ObjectChez(properties = Map(
          "street" -> StringChez(),
          "city" -> StringChez(),
          "zipCode" -> StringChez()
        ))
        val userSchema = ObjectChez(properties = Map(
          "name" -> StringChez(),
          "address" -> addressSchema
        ))
        val json = userSchema.toJsonSchema

        assert(json("type").str == "object")
        assert(json("properties")("address")("type").str == "object")
        assert(json("properties")("address")("properties")("street")("type").str == "string")
      }

      test("deeply nested objects") {
        val level3 = ObjectChez(properties = Map("value" -> StringChez()))
        val level2 = ObjectChez(properties = Map("level3" -> level3))
        val level1 = ObjectChez(properties = Map("level2" -> level2))
        val json = level1.toJsonSchema

        assert(json("properties")("level2")("properties")("level3")("properties")("value")("type").str == "string")
      }
    }

    test("real world object patterns") {
      test("user profile schema") {
        val schema = ObjectChez(
          properties = Map(
            "id" -> StringChez(format = Some("uuid")),
            "username" -> StringChez(minLength = Some(3), maxLength = Some(20)),
            "email" -> StringChez(format = Some("email")),
            "age" -> IntegerChez(minimum = Some(0), maximum = Some(150)),
            "preferences" -> ObjectChez(additionalProperties = Some(true))
          ),
          required = Set("id", "username", "email")
        )
        val json = schema.toJsonSchema

        assert(json("properties")("id")("format").str == "uuid")
        assert(json("properties")("email")("format").str == "email")
        assert(json("required").arr.map(_.str).toSet == Set("id", "username", "email"))
      }

      test("api configuration schema") {
        val schema = ObjectChez(
          properties = Map(
            "apiKey" -> StringChez(pattern = Some("^[A-Za-z0-9]{32}$")),
            "endpoints" -> ObjectChez(
              patternProperties = Map(".*" -> StringChez(format = Some("uri")))
            ),
            "retryCount" -> IntegerChez(minimum = Some(0), maximum = Some(10)),
            "timeout" -> IntegerChez(minimum = Some(1000))
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
        val schema = ObjectChez(
          properties = Map("optional" -> StringChez()),
          required = Set.empty
        )
        val json = schema.toJsonSchema

        assert(!json.obj.contains("required"))
      }

      test("required field not in properties") {
        val schema = ObjectChez(
          properties = Map("name" -> StringChez()),
          required = Set("name", "missing")
        )
        val json = schema.toJsonSchema

        // Should still include the required field even if not in properties
        assert(json("required").arr.map(_.str).toSet == Set("name", "missing"))
      }

      test("zero property constraints") {
        val schema = ObjectChez(
          minProperties = Some(0),
          maxProperties = Some(0)
        )
        val json = schema.toJsonSchema

        assert(json("minProperties").num == 0)
        assert(json("maxProperties").num == 0)
      }

      test("conflicting property constraints") {
        val schema = ObjectChez(
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
        val schema = ObjectChez(
          properties = Map("name" -> StringChez(), "age" -> IntegerChez())
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
        val schema = ObjectChez(
          properties = Map("id" -> StringChez(), "active" -> BooleanChez())
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
        val schema1 = ObjectChez().withExamples(ujson.Obj("test" -> "value"))
        val schema2 = ObjectChez(
          properties = Map("name" -> StringChez())
        ).withExamples(
          ujson.Obj("name" -> "test1"),
          ujson.Obj("name" -> "test2")
        )
        
        // If we get here without exceptions, the test passes
        assert(schema1.toJsonSchema("examples").arr.length == 1)
        assert(schema2.toJsonSchema("examples").arr.length == 2)
      }
    }
  }
}