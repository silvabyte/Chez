package boogieloops.schema.primitives

import utest.*
import boogieloops.schema.primitives.*
import boogieloops.schema.validation.ValidationContext
import boogieloops.schema.*

object IntegerSchemaTests extends TestSuite {

  val tests = Tests {
    test("basic integer schema") {
      val schema = IntegerSchema()
      val result1 = schema.validate(ujson.Num(42), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(-10), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(0), ValidationContext())
      assert(result3.isValid)
    }

    test("minimum validation") {
      val schema = IntegerSchema(minimum = Some(10))
      val result1 = schema.validate(ujson.Num(10), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(15), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(5), ValidationContext())
      assert(!result3.isValid)
    }

    test("maximum validation") {
      val schema = IntegerSchema(maximum = Some(100))
      val result1 = schema.validate(ujson.Num(100), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(50), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Num(150), ValidationContext())
      assert(!result3.isValid)
    }

    test("exclusive minimum validation") {
      val schema = IntegerSchema(exclusiveMinimum = Some(0))
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
      val schema = IntegerSchema(exclusiveMaximum = Some(10))
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
      val schema = IntegerSchema(multipleOf = Some(5))
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
      val schema = IntegerSchema(const = Some(42))
      val result1 = schema.validate(ujson.Num(42), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Num(43), ValidationContext())
      assert(!result2.isValid)
      val result3 = schema.validate(ujson.Num(0), ValidationContext())
      assert(!result3.isValid)
    }

    test("enum validation - moved to EnumSchema") {
      // Note: enum validation is now handled by EnumSchema, not IntegerSchema
      // This test exists for backward compatibility documentation
      val enumSchema = boogieloops.schema.primitives.EnumSchema.fromInts(1, 5, 10)
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
      val schema = IntegerSchema(
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
      val schema = IntegerSchema(minimum = Some(1), maximum = Some(10))
      val json = schema.toJsonSchema
      assert(json("type").str == "integer")
      assert(json("minimum").num == 1)
      assert(json("maximum").num == 10)
    }

    test("default value support") {
      val schema = IntegerSchema().withDefault(ujson.Num(42))
      val json = schema.toJsonSchema
      assert(json("default").num == 42)

      val schemaZero = IntegerSchema(minimum = Some(0)).withDefault(ujson.Num(0))
      val jsonZero = schemaZero.toJsonSchema
      assert(jsonZero("default").num == 0)
      assert(jsonZero("minimum").num == 0)
    }
  }
}
