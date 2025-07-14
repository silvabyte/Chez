package chez.primitives

import utest.*
import chez.primitives.*
import chez.validation.ValidationContext
import chez.*

object IntegerChezTests extends TestSuite {

  val tests = Tests {
    test("basic integer schema") {
      val schema = IntegerChez()
      val result1 = schema.validate(ujson.Num(42), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(-10), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(0), ValidationContext())
      assert(result3.isValid)
    }

    test("minimum validation") {
      val schema = IntegerChez(minimum = Some(10))
      val result1 = schema.validate(ujson.Num(10), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(15), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(5), ValidationContext())
      assert(!result3.isValid)
    }

    test("maximum validation") {
      val schema = IntegerChez(maximum = Some(100))
      val result1 = schema.validate(ujson.Num(100), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(50), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(150), ValidationContext())
      assert(!result3.isValid)
    }

    test("exclusive minimum validation") {
      val schema = IntegerChez(exclusiveMinimum = Some(0))
      val result1 = schema.validate(ujson.Num(1), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(5), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(0), ValidationContext())
      assert(!result3.isValid)
      val result4 = schema.validate(ujson.Num(-1), ValidationContext())
      assert(!result4.isValid)
    }

    test("exclusive maximum validation") {
      val schema = IntegerChez(exclusiveMaximum = Some(10))
      val result1 = schema.validate(ujson.Num(9), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(5), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(10), ValidationContext())
      assert(!result3.isValid)
      val result4 = schema.validate(ujson.Num(15), ValidationContext())
      assert(!result4.isValid)
    }

    test("multiple of validation") {
      val schema = IntegerChez(multipleOf = Some(5))
      val result1 = schema.validate(ujson.Num(10), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(15), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(0), ValidationContext())
      assert(result3.isValid)
      val result4 = schema.validate(ujson.Num(7), ValidationContext())
      assert(!result4.isValid)
      val result5 = schema.validate(ujson.Num(13), ValidationContext())
      assert(!result5.isValid)
    }

    test("const validation") {
      val schema = IntegerChez(const = Some(42))
      val result1 = schema.validate(ujson.Num(42), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(43), ValidationContext())
      assert(!result2.isValid)
      val result3 = schema.validate(ujson.Num(0), ValidationContext())
      assert(!result3.isValid)
    }

    test("enum validation - moved to EnumChez") {
      // Note: enum validation is now handled by EnumChez, not IntegerChez
      // This test exists for backward compatibility documentation
      val enumSchema = chez.primitives.EnumChez.fromInts(1, 5, 10)
      val result1 = enumSchema.validate(ujson.Num(1), ValidationContext())
      assert(result1.isValid)
      val result2 = enumSchema.validate(ujson.Num(5), ValidationContext())
      assert(result2.isValid)
      val result3 = enumSchema.validate(ujson.Num(10), ValidationContext())
      assert(result3.isValid)
      val result4 = enumSchema.validate(ujson.Num(3), ValidationContext())
      assert(!result4.isValid)
      val result5 = enumSchema.validate(ujson.Num(0), ValidationContext())
      assert(!result5.isValid)
    }

    test("combined validations") {
      val schema = IntegerChez(
        minimum = Some(0),
        maximum = Some(100),
        multipleOf = Some(2)
      )
      val result1 = schema.validate(ujson.Num(50), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(0), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(100), ValidationContext())
      assert(result3.isValid)
      val result4 = schema.validate(ujson.Num(-1), ValidationContext())
      assert(!result4.isValid) // below minimum
      val result5 = schema.validate(ujson.Num(101), ValidationContext())
      assert(!result5.isValid) // above maximum
      val result6 = schema.validate(ujson.Num(3), ValidationContext())
      assert(!result6.isValid) // not multiple of 2
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
