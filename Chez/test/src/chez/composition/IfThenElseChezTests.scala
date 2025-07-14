package chez.composition

import utest.*
import chez.*
import chez.primitives.*
import chez.complex.*
import chez.composition.*
import chez.validation.{ValidationContext, ValidationResult}
import upickle.default.*

object IfThenElseChezTests extends TestSuite {

  val tests = Tests {
    test("json schema generation") {
      test("basic if-then-else schema") {
        val schema = IfThenElseChez(
          condition = ObjectChez(properties = Map("type" -> StringChez(const = Some("user")))),
          thenSchema = Some(ObjectChez(properties = Map("role" -> StringChez()), required = Set("role"))),
          elseSchema = Some(ObjectChez(properties = Map("permissions" -> ArrayChez(StringChez()))))
        )
        val json = schema.toJsonSchema

        assert(json.obj.contains("if"))
        assert(json.obj.contains("then"))
        assert(json.obj.contains("else"))
        
        val ifSchema = json("if")
        assert(ifSchema("properties").obj.contains("type"))
        
        val thenSchema = json("then")
        assert(thenSchema("properties").obj.contains("role"))
        
        val elseSchema = json("else")
        assert(elseSchema("properties").obj.contains("permissions"))
      }

      test("if-then schema without else") {
        val schema = IfThenElseChez(
          condition = StringChez(pattern = Some("^admin")),
          thenSchema = Some(StringChez(minLength = Some(10)))
        )
        val json = schema.toJsonSchema

        assert(json.obj.contains("if"))
        assert(json.obj.contains("then"))
        assert(!json.obj.contains("else"))
      }

      test("if-else schema without then") {
        val schema = IfThenElseChez(
          condition = NumberChez(minimum = Some(18)),
          elseSchema = Some(NumberChez(maximum = Some(17)))
        )
        val json = schema.toJsonSchema

        assert(json.obj.contains("if"))
        assert(!json.obj.contains("then"))
        assert(json.obj.contains("else"))
      }

      test("if-then-else schema with metadata") {
        val schema = IfThenElseChez(
          condition = BooleanChez(),
          thenSchema = Some(StringChez()),
          elseSchema = Some(IntegerChez())
        ).withTitle("Conditional Type").withDescription("Type depends on boolean condition")
        val json = schema.toJsonSchema

        assert(json("title").str == "Conditional Type")
        assert(json("description").str == "Type depends on boolean condition")
        assert(json.obj.contains("if"))
      }
    }

    test("IfThenElse validation behavior") {
      test("applies then schema when condition matches") {
        val schema = IfThenElseChez(
          condition = ObjectChez(properties = Map("type" -> StringChez(const = Some("user")))),
          thenSchema = Some(ObjectChez(properties = Map("name" -> StringChez()), required = Set("name"))),
          elseSchema = Some(ObjectChez(properties = Map("id" -> IntegerChez())))
        )
        
        val value = ujson.Obj("type" -> ujson.Str("user"), "name" -> ujson.Str("John"))
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid)
      }

      test("applies else schema when condition doesn't match") {
        val schema = IfThenElseChez(
          condition = ObjectChez(properties = Map("type" -> StringChez(const = Some("admin")))),
          thenSchema = Some(ObjectChez(properties = Map("permissions" -> ArrayChez(StringChez())), required = Set("permissions"))),
          elseSchema = Some(ObjectChez(properties = Map("role" -> StringChez()), required = Set("role")))
        )
        
        val value = ujson.Obj("type" -> ujson.Str("user"), "role" -> ujson.Str("viewer"))
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid)
      }

      test("fails when then schema doesn't validate") {
        val schema = IfThenElseChez(
          condition = ObjectChez(properties = Map("type" -> StringChez(const = Some("user")))),
          thenSchema = Some(ObjectChez(properties = Map("name" -> StringChez(minLength = Some(10))), required = Set("name")))
        )
        
        val value = ujson.Obj("type" -> ujson.Str("user"), "name" -> ujson.Str("Jo")) // Name too short
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(!result.isValid)
      }

      test("fails when else schema doesn't validate") {
        val schema = IfThenElseChez(
          condition = ObjectChez(properties = Map("type" -> StringChez(const = Some("admin")))),
          thenSchema = Some(ObjectChez(properties = Map("permissions" -> ArrayChez(StringChez())))),
          elseSchema = Some(ObjectChez(properties = Map("age" -> IntegerChez(minimum = Some(18))), required = Set("age")))
        )
        
        val value = ujson.Obj("type" -> ujson.Str("user"), "age" -> ujson.Num(16)) // Age too low
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(!result.isValid)
      }

      test("validates when no then schema and condition matches") {
        val schema = IfThenElseChez(
          condition = StringChez(minLength = Some(5)),
          elseSchema = Some(StringChez(maxLength = Some(3)))
        )
        
        val value = ujson.Str("hello world") // Condition matches, no then schema to fail
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid)
      }

      test("validates when no else schema and condition doesn't match") {
        val schema = IfThenElseChez(
          condition = StringChez(minLength = Some(10)),
          thenSchema = Some(StringChez(pattern = Some("^[A-Z].*")))
        )
        
        val value = ujson.Str("short") // Condition doesn't match, no else schema to fail
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid)
      }
    }

    test("Complex IfThenElse scenarios") {
      test("nested conditional validation") {
        val innerIfThenElse = IfThenElseChez(
          condition = StringChez(pattern = Some("^admin.*")),
          thenSchema = Some(StringChez(minLength = Some(10))),
          elseSchema = Some(StringChez(maxLength = Some(8)))
        )
        
        val outerIfThenElse = IfThenElseChez(
          condition = StringChez(),
          thenSchema = Some(innerIfThenElse)
        )
        
        val value = ujson.Str("admin12345") // Matches inner condition and then constraint
        val context = ValidationContext()
        val result = outerIfThenElse.validate(value, context)

        assert(result.isValid)
      }

      test("object type discrimination") {
        val schema = IfThenElseChez(
          condition = ObjectChez(properties = Map("type" -> StringChez(const = Some("circle")))),
          thenSchema = Some(ObjectChez(properties = Map(
            "radius" -> NumberChez(minimum = Some(0))
          ), required = Set("radius"))),
          elseSchema = Some(ObjectChez(properties = Map(
            "width" -> NumberChez(minimum = Some(0)),
            "height" -> NumberChez(minimum = Some(0))
          ), required = Set("width", "height")))
        )
        
        // Circle case
        val circleValue = ujson.Obj("type" -> ujson.Str("circle"), "radius" -> ujson.Num(5))
        val context = ValidationContext()
        val circleResult = schema.validate(circleValue, context)
        assert(circleResult.isValid)

        // Rectangle case
        val rectValue = ujson.Obj("type" -> ujson.Str("rectangle"), "width" -> ujson.Num(10), "height" -> ujson.Num(5))
        val rectResult = schema.validate(rectValue, context)
        assert(rectResult.isValid)
      }

      test("array length conditional validation") {
        val schema = IfThenElseChez(
          condition = ArrayChez(StringChez(), minItems = Some(3)),
          thenSchema = Some(ArrayChez(StringChez(minLength = Some(5)))),
          elseSchema = Some(ArrayChez(StringChez()))
        )
        
        // Long array with long strings
        val longArray = ujson.Arr(ujson.Str("hello"), ujson.Str("world"), ujson.Str("testing"))
        val context = ValidationContext()
        val longResult = schema.validate(longArray, context)
        assert(longResult.isValid)

        // Short array with any strings
        val shortArray = ujson.Arr(ujson.Str("hi"))
        val shortResult = schema.validate(shortArray, context)
        assert(shortResult.isValid)
      }
    }

    test("Error reporting and paths") {
      test("error paths are preserved during then validation") {
        val schema = IfThenElseChez(
          condition = StringChez(),
          thenSchema = Some(StringChez(minLength = Some(10)))
        )
        
        val value = ujson.Str("short")
        val context = ValidationContext("/test/path")
        val result = schema.validate(value, context)

        assert(!result.isValid)
        assert(result.errors.exists(_.toString.contains("/test/path")))
      }

      test("error paths are preserved during else validation") {
        val schema = IfThenElseChez(
          condition = IntegerChez(),
          elseSchema = Some(StringChez(minLength = Some(10)))
        )
        
        val value = ujson.Str("short")
        val context = ValidationContext("/test/path")
        val result = schema.validate(value, context)

        assert(!result.isValid)
        assert(result.errors.exists(_.toString.contains("/test/path")))
      }

      test("condition validation doesn't affect error reporting") {
        val schema = IfThenElseChez(
          condition = ObjectChez(properties = Map("flag" -> BooleanChez())),
          thenSchema = Some(StringChez(minLength = Some(10)))
        )
        
        // String value doesn't match condition (object), so else branch (none) validates
        val value = ujson.Str("test")
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid) // No else schema means validation passes
      }
    }

    test("Real-world conditional patterns") {
      test("user role-based validation") {
        val schema = IfThenElseChez(
          condition = ObjectChez(properties = Map("role" -> StringChez(const = Some("admin")))),
          thenSchema = Some(ObjectChez(properties = Map(
            "permissions" -> ArrayChez(StringChez(), minItems = Some(1)),
            "department" -> StringChez()
          ), required = Set("permissions", "department"))),
          elseSchema = Some(ObjectChez(properties = Map(
            "supervisor" -> StringChez()
          ), required = Set("supervisor")))
        )
        
        // Admin user
        val adminUser = ujson.Obj(
          "role" -> ujson.Str("admin"),
          "permissions" -> ujson.Arr(ujson.Str("read"), ujson.Str("write")),
          "department" -> ujson.Str("IT")
        )
        val context = ValidationContext()
        val adminResult = schema.validate(adminUser, context)
        assert(adminResult.isValid)

        // Regular user
        val regularUser = ujson.Obj(
          "role" -> ujson.Str("user"),
          "supervisor" -> ujson.Str("John Smith")
        )
        val userResult = schema.validate(regularUser, context)
        assert(userResult.isValid)
      }

      test("API versioning conditional validation") {
        val schema = IfThenElseChez(
          condition = ObjectChez(properties = Map("version" -> StringChez(const = Some("v2")))),
          thenSchema = Some(ObjectChez(properties = Map(
            "data" -> ObjectChez(properties = Map("format" -> StringChez(const = Some("json"))))
          ), required = Set("data"))),
          elseSchema = Some(ObjectChez(properties = Map(
            "payload" -> StringChez()
          ), required = Set("payload")))
        )
        
        // v2 API format
        val v2Request = ujson.Obj(
          "version" -> ujson.Str("v2"),
          "data" -> ujson.Obj("format" -> ujson.Str("json"), "content" -> ujson.Str("test"))
        )
        val context = ValidationContext()
        val v2Result = schema.validate(v2Request, context)
        assert(v2Result.isValid)

        // v1 API format
        val v1Request = ujson.Obj(
          "version" -> ujson.Str("v1"),
          "payload" -> ujson.Str("legacy format")
        )
        val v1Result = schema.validate(v1Request, context)
        assert(v1Result.isValid)
      }

      test("feature flag conditional validation") {
        val schema = IfThenElseChez(
          condition = ObjectChez(properties = Map("experimentalFeatures" -> BooleanChez(const = Some(true)))),
          thenSchema = Some(ObjectChez(properties = Map(
            "betaConfig" -> ObjectChez(properties = Map("enabled" -> BooleanChez()))
          ))),
          elseSchema = Some(ObjectChez(properties = Map(
            "stableConfig" -> ObjectChez(properties = Map("version" -> StringChez()))
          )))
        )
        
        // Experimental features enabled
        val betaConfig = ujson.Obj(
          "experimentalFeatures" -> ujson.Bool(true),
          "betaConfig" -> ujson.Obj("enabled" -> ujson.Bool(true))
        )
        val context = ValidationContext()
        val betaResult = schema.validate(betaConfig, context)
        assert(betaResult.isValid)

        // Stable configuration
        val stableConfig = ujson.Obj(
          "experimentalFeatures" -> ujson.Bool(false),
          "stableConfig" -> ujson.Obj("version" -> ujson.Str("1.0.0"))
        )
        val stableResult = schema.validate(stableConfig, context)
        assert(stableResult.isValid)
      }
    }

    test("ValidationResult framework integration") {
      test("ValidationResult.valid() for successful conditional validation") {
        val schema = IfThenElseChez(
          condition = StringChez(),
          thenSchema = Some(StringChez(minLength = Some(1)))
        )
        val value = ujson.Str("test")
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid)
        assert(result.errors.isEmpty)
        assert(result.isInstanceOf[ValidationResult])
      }

      test("ValidationResult.invalid() for failed conditional validation") {
        val schema = IfThenElseChez(
          condition = StringChez(),
          thenSchema = Some(StringChez(minLength = Some(10)))
        )
        val value = ujson.Str("short")
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(!result.isValid)
        assert(result.errors.nonEmpty)
        assert(result.isInstanceOf[ValidationResult])
      }
    }

    test("Edge cases and boundary conditions") {
      test("condition only (no then or else)") {
        val schema = IfThenElseChez(condition = StringChez())
        val value = ujson.Str("test")
        val context = ValidationContext()
        val result = schema.validate(value, context)

        assert(result.isValid) // No then/else schemas to fail
      }

      test("complex condition evaluation") {
        val complexCondition = AllOfChez(List(
          StringChez(minLength = Some(5)),
          StringChez(pattern = Some("^[A-Z].*"))
        ))
        val schema = IfThenElseChez(
          condition = complexCondition,
          thenSchema = Some(StringChez(maxLength = Some(10))),
          elseSchema = Some(StringChez())
        )
        
        // Meets complex condition
        val value = ujson.Str("Hello")
        val context = ValidationContext()
        val result = schema.validate(value, context)
        assert(result.isValid)

        // Doesn't meet complex condition
        val shortValue = ujson.Str("hi")
        val shortResult = schema.validate(shortValue, context)
        assert(shortResult.isValid) // Falls to else branch
      }

      test("condition with type mismatch") {
        val schema = IfThenElseChez(
          condition = ObjectChez(),
          thenSchema = Some(ObjectChez(properties = Map("key" -> StringChez()))),
          elseSchema = Some(StringChez())
        )
        
        // String doesn't match object condition, should use else
        val value = ujson.Str("not an object")
        val context = ValidationContext()
        val result = schema.validate(value, context)
        assert(result.isValid)
      }
    }
  }
}