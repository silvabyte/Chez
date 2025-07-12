package chez.complex

import utest.*
import chez.*
import chez.primitives.*
import chez.complex.*
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
          contains = Some(StringChez(pattern = Some("^test"))),
          minContains = Some(1),
          maxContains = Some(3)
        )
        val json = schema.toJsonSchema

        assert(json("contains")("type").str == "string")
        assert(json("contains")("pattern").str == "^test")
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
        val errors = schema.validate(List(ujson.Str("test1"), ujson.Str("test2")))
        assert(errors.isEmpty)
      }

      test("min items validation") {
        val schema = ArrayChez(StringChez(), minItems = Some(2))
        val errors = schema.validate(List(ujson.Str("test1")))
        assert(errors.length == 1)
        assert(errors.head.isInstanceOf[chez.ValidationError.MinItemsViolation])
      }

      test("max items validation") {
        val schema = ArrayChez(StringChez(), maxItems = Some(2))
        val errors = schema.validate(List(ujson.Str("test1"), ujson.Str("test2"), ujson.Str("test3")))
        assert(errors.length == 1)
        assert(errors.head.isInstanceOf[chez.ValidationError.MaxItemsViolation])
      }

      test("unique items validation") {
        val schema = ArrayChez(StringChez(), uniqueItems = Some(true))
        val errors = schema.validate(List(ujson.Str("test1"), ujson.Str("test1")))
        assert(errors.length == 1)
        assert(errors.head.isInstanceOf[chez.ValidationError.UniqueViolation])
      }

      test("multiple validation errors") {
        val schema = ArrayChez(StringChez(), minItems = Some(3), maxItems = Some(2), uniqueItems = Some(true))
        val errors = schema.validate(List(ujson.Str("test1"), ujson.Str("test1")))
        assert(errors.length == 2) // min items + unique items violations
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
  }
}