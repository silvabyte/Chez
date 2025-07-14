package chez.complex

import utest.*
import chez.*
import chez.primitives.*
import chez.complex.*
import chez.validation.{ValidationContext, ValidationResult}
import upickle.default.*

object ArrayChezTests extends TestSuite {

  val tests = Tests {
    test("json schema generation") {
      test("basic array schema") {
        val schema = ArrayChez(StringChez())
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(json("items")("type").str == "string")
      }

      test("array with item constraints") {
        val schema = ArrayChez(
          items = IntegerChez(minimum = Some(0)),
          minItems = Some(1),
          maxItems = Some(10),
          uniqueItems = Some(true)
        )
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(json("items")("type").str == "integer")
        assert(json("items")("minimum").num == 0)
        assert(json("minItems").num == 1)
        assert(json("maxItems").num == 10)
        assert(json("uniqueItems").bool == true)
      }

      test("array with complex item types") {
        val objectSchema = ObjectChez(
          properties = Map("name" -> StringChez(), "age" -> IntegerChez())
        )
        val schema = ArrayChez(objectSchema)
        val json = schema.toJsonSchema

        assert(json("type").str == "array")
        assert(json("items")("type").str == "object")
        assert(json("items")("properties")("name")("type").str == "string")
        assert(json("items")("properties")("age")("type").str == "integer")
      }

      test("array with metadata") {
        val schema = ArrayChez(StringChez())
          .withTitle("String Array")
          .withDescription("An array of strings")
          .withDefault(ujson.Arr("default1", "default2"))
        val json = schema.toJsonSchema

        assert(json("title").str == "String Array")
        assert(json("description").str == "An array of strings")
        assert(json("default").arr.map(_.str).toList == List("default1", "default2"))
      }
    }

    test("json schema 2020-12 features") {
      test("prefix items (tuple validation)") {
        val schema = ArrayChez(
          items = StringChez(),
          prefixItems = Some(List(IntegerChez(), StringChez(), BooleanChez()))
        )
        val json = schema.toJsonSchema

        assert(json("prefixItems").arr.length == 3)
        assert(json("prefixItems")(0)("type").str == "integer")
        assert(json("prefixItems")(1)("type").str == "string")
        assert(json("prefixItems")(2)("type").str == "boolean")
      }

      test("contains validation") {
        val schema = ArrayChez(
          items = StringChez(),
          contains = Some(StringChez(pattern = Some("^test.*"))),
          minContains = Some(1),
          maxContains = Some(3)
        )
        val json = schema.toJsonSchema

        assert(json("contains")("type").str == "string")
        assert(json("contains")("pattern").str == "^test.*")
        assert(json("minContains").num == 1)
        assert(json("maxContains").num == 3)
      }

      test("unevaluated items") {
        val schema = ArrayChez(
          items = StringChez(),
          unevaluatedItems = Some(false)
        )
        val json = schema.toJsonSchema

        assert(json("unevaluatedItems").bool == false)
      }
    }

    test("validation behavior") {
      test("valid array validation") {
        val schema = ArrayChez(StringChez(), minItems = Some(1), maxItems = Some(3))
        val result = schema.validate(ujson.Arr(ujson.Str("test1"), ujson.Str("test2")), ValidationContext())
        assert(result.isValid)
      }

      test("min items validation") {
        val schema = ArrayChez(StringChez(), minItems = Some(2))
        val result = schema.validate(ujson.Arr(ujson.Str("test1")), ValidationContext())
        assert(result.errors.length == 1)
        assert(result.errors.head.isInstanceOf[chez.ValidationError.MinItemsViolation])
      }

      test("max items validation") {
        val schema = ArrayChez(StringChez(), maxItems = Some(2))
        val result =
          schema.validate(ujson.Arr(ujson.Str("test1"), ujson.Str("test2"), ujson.Str("test3")), ValidationContext())
        assert(result.errors.length == 1)
        assert(result.errors.head.isInstanceOf[chez.ValidationError.MaxItemsViolation])
      }

      test("unique items validation") {
        val schema = ArrayChez(StringChez(), uniqueItems = Some(true))
        val result = schema.validate(ujson.Arr(ujson.Str("test1"), ujson.Str("test1")), ValidationContext())
        assert(result.errors.length == 1)
        assert(result.errors.head.isInstanceOf[chez.ValidationError.UniqueViolation])
      }

      test("multiple validation errors") {
        val schema =
          ArrayChez(StringChez(), minItems = Some(3), maxItems = Some(2), uniqueItems = Some(true))
        val result = schema.validate(ujson.Arr(ujson.Str("test1"), ujson.Str("test1")), ValidationContext())
        assert(result.errors.length == 2) // min items + unique items violations
      }
    }

    test("examples serialization") {
      test("array with examples using withExamples") {
        val schema = ArrayChez(StringChez())
          .withExamples(
            ujson.Arr("example1", "example2"),
            ujson.Arr("example3", "example4")
          )
        val json = schema.toJsonSchema

        assert(json.obj.contains("examples"))
        val examples = json("examples").arr
        assert(examples.length == 2)
        assert(examples(0).arr.map(_.str).toList == List("example1", "example2"))
        assert(examples(1).arr.map(_.str).toList == List("example3", "example4"))
      }

      test("array examples serialize as valid JSON array") {
        val schema = ArrayChez(IntegerChez())
          .withExamples(
            ujson.Arr(1, 2, 3),
            ujson.Arr(4, 5, 6)
          )
        val json = schema.toJsonSchema

        // Verify examples is a proper JSON array
        assert(json("examples").isInstanceOf[ujson.Arr])
        val examples = json("examples").arr
        assert(examples.length == 2)

        // Verify each example is itself an array
        assert(examples(0).isInstanceOf[ujson.Arr])
        assert(examples(1).isInstanceOf[ujson.Arr])

        // Verify content
        assert(examples(0).arr.map(_.num.toInt).toList == List(1, 2, 3))
        assert(examples(1).arr.map(_.num.toInt).toList == List(4, 5, 6))
      }

      test("withExamples calls work without errors") {
        // This test ensures withExamples() doesn't throw runtime errors
        val schema1 = ArrayChez(StringChez()).withExamples(ujson.Arr("test"))
        val schema2 = ArrayChez(IntegerChez()).withExamples(ujson.Arr(1, 2), ujson.Arr(3, 4))
        val schema3 = ArrayChez(BooleanChez()).withExamples(ujson.Arr(true, false))

        // If we get here without exceptions, the test passes
        assert(schema1.toJsonSchema("examples").arr.length == 1)
        assert(schema2.toJsonSchema("examples").arr.length == 2)
        assert(schema3.toJsonSchema("examples").arr.length == 1)
      }
    }

    test("Tuple validation with prefixItems") {
      test("valid tuple validation") {
        val schema = ArrayChez(
          items = StringChez(), // fallback for items beyond prefix
          prefixItems = Some(List(
            IntegerChez(minimum = Some(0)),
            StringChez(minLength = Some(1)),
            BooleanChez()
          ))
        )
        
        // Valid tuple: [42, "hello", true, "extra"]
        val validArray = ujson.Arr(ujson.Num(42), ujson.Str("hello"), ujson.Bool(true), ujson.Str("extra"))
        val result = schema.validate(validArray, ValidationContext())
        assert(result.isValid)
      }

      test("tuple validation with prefix constraint violations") {
        val schema = ArrayChez(
          items = StringChez(),
          prefixItems = Some(List(
            IntegerChez(minimum = Some(10)), // First item must be >= 10
            StringChez(minLength = Some(5)), // Second item must have length >= 5
            BooleanChez()
          ))
        )
        
        // Invalid tuple: [5, "hi", true] - first two items violate constraints
        val invalidArray = ujson.Arr(ujson.Num(5), ujson.Str("hi"), ujson.Bool(true))
        val result = schema.validate(invalidArray, ValidationContext())
        assert(!result.isValid)
        assert(result.errors.length == 2) // Two constraint violations
        
        // Check that errors have correct paths
        val hasIndexZeroError = result.errors.exists(error => 
          error.asInstanceOf[chez.ValidationError].toString.contains("/0")
        )
        val hasIndexOneError = result.errors.exists(error => 
          error.asInstanceOf[chez.ValidationError].toString.contains("/1")
        )
        assert(hasIndexZeroError)
        assert(hasIndexOneError)
      }

      test("tuple validation with items beyond prefix") {
        val schema = ArrayChez(
          items = StringChez(minLength = Some(3)), // Items beyond prefix must be strings >= 3 chars
          prefixItems = Some(List(IntegerChez(), BooleanChez()))
        )
        
        // Array: [42, true, "validstring", "x"] - last item too short
        val invalidArray = ujson.Arr(ujson.Num(42), ujson.Bool(true), ujson.Str("validstring"), ujson.Str("x"))
        val result = schema.validate(invalidArray, ValidationContext())
        assert(!result.isValid)
        assert(result.errors.length == 1)
        
        // Error should be at index 3
        val error = result.errors.head.asInstanceOf[chez.ValidationError.MinLengthViolation]
        assert(error.path == "/3")
      }
    }

    test("Contains validation") {
      test("valid contains validation") {
        val schema = ArrayChez(
          items = StringChez(),
          contains = Some(StringChez(pattern = Some("^test.*"))), // Must contain strings starting with "test"
          minContains = Some(1),
          maxContains = Some(2)
        )
        
        // Array with exactly 2 items matching pattern: ["test1", "other", "test2", "another"]
        val validArray = ujson.Arr(ujson.Str("test1"), ujson.Str("other"), ujson.Str("test2"), ujson.Str("another"))
        val result = schema.validate(validArray, ValidationContext())
        assert(result.isValid)
      }

      test("contains validation - too few matches") {
        val schema = ArrayChez(
          items = StringChez(),
          contains = Some(StringChez(pattern = Some("^test.*"))),
          minContains = Some(2),
          maxContains = Some(5)
        )
        
        // Array with only 1 matching item: ["test1", "other", "another"]
        val invalidArray = ujson.Arr(ujson.Str("test1"), ujson.Str("other"), ujson.Str("another"))
        val result = schema.validate(invalidArray, ValidationContext())
        assert(!result.isValid)
        assert(result.errors.length == 1)
        assert(result.errors.head.isInstanceOf[chez.ValidationError.ContainsViolation])
        
        val containsError = result.errors.head.asInstanceOf[chez.ValidationError.ContainsViolation]
        assert(containsError.minContains == Some(2))
        assert(containsError.actualContains == 1)
      }

      test("contains validation - too many matches") {
        val schema = ArrayChez(
          items = StringChez(),
          contains = Some(StringChez(pattern = Some("^test.*"))),
          minContains = Some(1),
          maxContains = Some(2)
        )
        
        // Array with 3 matching items: ["test1", "test2", "test3"]
        val invalidArray = ujson.Arr(ujson.Str("test1"), ujson.Str("test2"), ujson.Str("test3"))
        val result = schema.validate(invalidArray, ValidationContext())
        assert(!result.isValid)
        assert(result.errors.length == 1)
        assert(result.errors.head.isInstanceOf[chez.ValidationError.ContainsViolation])
        
        val containsError = result.errors.head.asInstanceOf[chez.ValidationError.ContainsViolation]
        assert(containsError.maxContains == Some(2))
        assert(containsError.actualContains == 3)
      }

      test("contains validation - no constraints means any count is valid") {
        val schema = ArrayChez(
          items = StringChez(),
          contains = Some(StringChez(pattern = Some("^test.*")))
          // No minContains or maxContains specified
        )
        
        // Array with 0 matches should be valid
        val zeroMatchArray = ujson.Arr(ujson.Str("other"), ujson.Str("another"))
        val result1 = schema.validate(zeroMatchArray, ValidationContext())
        assert(result1.isValid)
        
        // Array with many matches should be valid
        val manyMatchArray = ujson.Arr(ujson.Str("test1"), ujson.Str("test2"), ujson.Str("test3"), ujson.Str("test4"))
        val result2 = schema.validate(manyMatchArray, ValidationContext())
        assert(result2.isValid)
      }
    }

    test("Complex array validation scenarios") {
      test("array with both prefixItems and contains validation") {
        val schema = ArrayChez(
          items = StringChez(), // Fallback for items beyond prefix
          prefixItems = Some(List(
            IntegerChez(minimum = Some(0)),
            StringChez(minLength = Some(1))
          )),
          contains = Some(StringChez(pattern = Some("^valid.*"))),
          minContains = Some(1),
          minItems = Some(3),
          maxItems = Some(6)
        )
        
        // Valid array: [42, "hello", "validstring", "other"]
        val validArray = ujson.Arr(ujson.Num(42), ujson.Str("hello"), ujson.Str("validstring"), ujson.Str("other"))
        val result = schema.validate(validArray, ValidationContext())
        assert(result.isValid)
      }

      test("array with multiple constraint violations") {
        val schema = ArrayChez(
          items = StringChez(minLength = Some(5)),
          prefixItems = Some(List(IntegerChez(minimum = Some(100)))),
          contains = Some(StringChez(pattern = Some("^required.*"))),
          minContains = Some(1),
          minItems = Some(5),
          uniqueItems = Some(true)
        )
        
        // Invalid array with multiple violations: [5, "hi", "hi"] 
        // - prefix violation (5 < 100)
        // - items violation ("hi" < 5 chars) 
        // - contains violation (no "required" pattern)
        // - minItems violation (3 < 5)
        // - uniqueItems violation (duplicate "hi")
        val invalidArray = ujson.Arr(ujson.Num(5), ujson.Str("hi"), ujson.Str("hi"))
        val result = schema.validate(invalidArray, ValidationContext())
        assert(!result.isValid)
        assert(result.errors.length >= 4) // At least 4 different types of violations
      }
    }

    test("ValidationResult framework integration for T5 features") {
      test("ValidationResult.valid() for successful tuple and contains validation") {
        val schema = ArrayChez(
          items = StringChez(),
          prefixItems = Some(List(IntegerChez(), StringChez())),
          contains = Some(StringChez(pattern = Some("^test.*"))),
          minContains = Some(1)
        )
        
        val validArray = ujson.Arr(ujson.Num(42), ujson.Str("hello"), ujson.Str("test123"))
        val result = schema.validate(validArray, ValidationContext())
        
        assert(result.isValid)
        assert(result.errors.isEmpty)
        assert(result.isInstanceOf[ValidationResult.Valid.type])
      }

      test("ValidationResult.invalid() for failed tuple and contains validation") {
        val schema = ArrayChez(
          items = StringChez(),
          prefixItems = Some(List(IntegerChez(minimum = Some(100)))),
          contains = Some(StringChez(pattern = Some("^required.*"))),
          minContains = Some(1)
        )
        
        val invalidArray = ujson.Arr(ujson.Num(5), ujson.Str("hello"))
        val result = schema.validate(invalidArray, ValidationContext())
        
        assert(!result.isValid)
        assert(result.errors.nonEmpty)
        assert(result.isInstanceOf[ValidationResult.Invalid])
        assert(result.errors.length == 2) // prefix violation + contains violation
      }
    }
  }
}
