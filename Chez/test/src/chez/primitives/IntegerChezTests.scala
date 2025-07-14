package chez.primitives

import utest.*
import chez.primitives.*

object IntegerChezTests extends TestSuite {

  val tests = Tests {
    test("basic integer schema") {
      val schema = IntegerChez()
      assert(schema.validate(42) == Nil)
      assert(schema.validate(-10) == Nil)
      assert(schema.validate(0) == Nil)
    }

    test("minimum validation") {
      val schema = IntegerChez(minimum = Some(10))
      assert(schema.validate(10) == Nil)
      assert(schema.validate(15) == Nil)
      assert(schema.validate(5).nonEmpty)
    }

    test("maximum validation") {
      val schema = IntegerChez(maximum = Some(100))
      assert(schema.validate(100) == Nil)
      assert(schema.validate(50) == Nil)
      assert(schema.validate(150).nonEmpty)
    }

    test("exclusive minimum validation") {
      val schema = IntegerChez(exclusiveMinimum = Some(0))
      assert(schema.validate(1) == Nil)
      assert(schema.validate(5) == Nil)
      assert(schema.validate(0).nonEmpty)
      assert(schema.validate(-1).nonEmpty)
    }

    test("exclusive maximum validation") {
      val schema = IntegerChez(exclusiveMaximum = Some(10))
      assert(schema.validate(9) == Nil)
      assert(schema.validate(5) == Nil)
      assert(schema.validate(10).nonEmpty)
      assert(schema.validate(15).nonEmpty)
    }

    test("multiple of validation") {
      val schema = IntegerChez(multipleOf = Some(5))
      assert(schema.validate(10) == Nil)
      assert(schema.validate(15) == Nil)
      assert(schema.validate(0) == Nil)
      assert(schema.validate(7).nonEmpty)
      assert(schema.validate(13).nonEmpty)
    }

    test("const validation") {
      val schema = IntegerChez(const = Some(42))
      assert(schema.validate(42) == Nil)
      assert(schema.validate(43).nonEmpty)
      assert(schema.validate(0).nonEmpty)
    }

    test("enum validation - moved to EnumChez") {
      // Note: enum validation is now handled by EnumChez, not IntegerChez
      // This test exists for backward compatibility documentation
      val enumSchema = chez.primitives.EnumChez.fromInts(1, 5, 10)
      assert(enumSchema.validateInt(1) == Nil)
      assert(enumSchema.validateInt(5) == Nil)
      assert(enumSchema.validateInt(10) == Nil)
      assert(enumSchema.validateInt(3).nonEmpty)
      assert(enumSchema.validateInt(0).nonEmpty)
    }

    test("combined validations") {
      val schema = IntegerChez(
        minimum = Some(0),
        maximum = Some(100),
        multipleOf = Some(2)
      )
      assert(schema.validate(50) == Nil)
      assert(schema.validate(0) == Nil)
      assert(schema.validate(100) == Nil)
      assert(schema.validate(-1).nonEmpty) // below minimum
      assert(schema.validate(101).nonEmpty) // above maximum
      assert(schema.validate(3).nonEmpty) // not multiple of 2
    }

    test("json schema generation") {
      val schema = IntegerChez(minimum = Some(1), maximum = Some(10))
      val json = schema.toJsonSchema
      assert(json("type").str == "integer")
      assert(json("minimum").num == 1)
      assert(json("maximum").num == 10)
    }

    test("default value support") {
      val schema = IntegerChez().withDefault(ujson.Num(42))
      val json = schema.toJsonSchema
      assert(json("default").num == 42)

      val schemaZero = IntegerChez(minimum = Some(0)).withDefault(ujson.Num(0))
      val jsonZero = schemaZero.toJsonSchema
      assert(jsonZero("default").num == 0)
      assert(jsonZero("minimum").num == 0)
    }
  }
}
