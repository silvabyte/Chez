package chez.primitives

import utest.*
import chez.primitives.*
import chez.validation.ValidationContext
import chez.*

object NumberChezTests extends TestSuite {

  val tests = Tests {
    test("basic number schema") {
      val schema = NumberChez()
      val result1 = schema.validate(ujson.Num(42.5), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(-10.25), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(0.0), ValidationContext())
      assert(result3.isValid)
      val result4 = schema.validate(ujson.Num(3.14159), ValidationContext())
      assert(result4.isValid)
    }

    test("minimum validation") {
      val schema = NumberChez(minimum = Some(0.0))
      val result1 = schema.validate(ujson.Num(0.0), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(1.5), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(-0.1), ValidationContext())
      assert(!result3.isValid)
    }

    test("maximum validation") {
      val schema = NumberChez(maximum = Some(100.0))
      val result1 = schema.validate(ujson.Num(100.0), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(50.5), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(100.1), ValidationContext())
      assert(!result3.isValid)
    }

    test("exclusive minimum validation") {
      val schema = NumberChez(exclusiveMinimum = Some(0.0))
      val result1 = schema.validate(ujson.Num(0.1), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(1.0), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(0.0), ValidationContext())
      assert(!result3.isValid)
      val result4 = schema.validate(ujson.Num(-0.1), ValidationContext())
      assert(!result4.isValid)
    }

    test("exclusive maximum validation") {
      val schema = NumberChez(exclusiveMaximum = Some(10.0))
      val result1 = schema.validate(ujson.Num(9.9), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(5.0), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(10.0), ValidationContext())
      assert(!result3.isValid)
      val result4 = schema.validate(ujson.Num(10.1), ValidationContext())
      assert(!result4.isValid)
    }

    test("multiple of validation") {
      val schema = NumberChez(multipleOf = Some(0.5))
      val result1 = schema.validate(ujson.Num(1.0), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(1.5), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(0.0), ValidationContext())
      assert(result3.isValid)
      val result4 = schema.validate(ujson.Num(0.75), ValidationContext())
      assert(!result4.isValid)
      val result5 = schema.validate(ujson.Num(1.3), ValidationContext())
      assert(!result5.isValid)
    }

    test("const validation") {
      val schema = NumberChez(const = Some(3.14))
      val result1 = schema.validate(ujson.Num(3.14), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(3.15), ValidationContext())
      assert(!result2.isValid)
      val result3 = schema.validate(ujson.Num(0.0), ValidationContext())
      assert(!result3.isValid)
    }

    test("enum validation - moved to EnumChez") {
      // Note: enum validation is now handled by EnumChez, not NumberChez
      // This test exists for backward compatibility documentation
      val enumSchema = chez.primitives.EnumChez.fromNumbers(1.0, 2.5, 5.0)
      val result1 = enumSchema.validate(ujson.Num(1.0), ValidationContext())
      assert(result1.isValid)
      val result2 = enumSchema.validate(ujson.Num(2.5), ValidationContext())
      assert(result2.isValid)
      val result3 = enumSchema.validate(ujson.Num(5.0), ValidationContext())
      assert(result3.isValid)
      val result4 = enumSchema.validate(ujson.Num(3.0), ValidationContext())
      assert(!result4.isValid)
      val result5 = enumSchema.validate(ujson.Num(0.0), ValidationContext())
      assert(!result5.isValid)
    }

    test("precision edge cases") {
      val schema = NumberChez()
      val result1 = schema.validate(ujson.Num(Double.MaxValue), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(Double.MinValue), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(Double.PositiveInfinity), ValidationContext())
      assert(result3.isValid)
      val result4 = schema.validate(ujson.Num(Double.NegativeInfinity), ValidationContext())
      assert(result4.isValid)
    }

    test("combined validations") {
      val schema = NumberChez(
        minimum = Some(0.0),
        maximum = Some(100.0),
        multipleOf = Some(1.0)
      )
      val result1 = schema.validate(ujson.Num(50.0), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(0.0), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(99.0), ValidationContext())
      assert(result3.isValid)
      val result4 = schema.validate(ujson.Num(-0.1), ValidationContext())
      assert(!result4.isValid) // below minimum
      val result5 = schema.validate(ujson.Num(100.1), ValidationContext())
      assert(!result5.isValid) // above maximum
      val result6 = schema.validate(ujson.Num(50.5), ValidationContext())
      assert(!result6.isValid) // not multiple of 1.0
    }

    test("json schema generation") {
      val schema = NumberChez(minimum = Some(0.0), maximum = Some(1.0))
      val json = schema.toJsonSchema
      assert(json("type").str == "number")
      assert(json("minimum").num == 0.0)
      assert(json("maximum").num == 1.0)
    }

    test("default value support") {
      val schema = NumberChez().withDefault(ujson.Num(3.14))
      val json = schema.toJsonSchema
      assert(json("default").num == 3.14)

      val schemaZero = NumberChez(minimum = Some(0.0)).withDefault(ujson.Num(0.0))
      val jsonZero = schemaZero.toJsonSchema
      assert(jsonZero("default").num == 0.0)
      assert(jsonZero("minimum").num == 0.0)

      val schemaNegative = NumberChez().withDefault(ujson.Num(-1.5))
      val jsonNegative = schemaNegative.toJsonSchema
      assert(jsonNegative("default").num == -1.5)
    }
  }
}
