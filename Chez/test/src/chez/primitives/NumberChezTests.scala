package chez.primitives

import utest.*
import chez.primitives.*

object NumberChezTests extends TestSuite {

  val tests = Tests {
    test("basic number schema") {
      val schema = NumberChez()
      assert(schema.validate(42.5) == Nil)
      assert(schema.validate(-10.25) == Nil)
      assert(schema.validate(0.0) == Nil)
      assert(schema.validate(3.14159) == Nil)
    }
    
    test("minimum validation") {
      val schema = NumberChez(minimum = Some(0.0))
      assert(schema.validate(0.0) == Nil)
      assert(schema.validate(1.5) == Nil)
      assert(schema.validate(-0.1).nonEmpty)
    }
    
    test("maximum validation") {
      val schema = NumberChez(maximum = Some(100.0))
      assert(schema.validate(100.0) == Nil)
      assert(schema.validate(50.5) == Nil)
      assert(schema.validate(100.1).nonEmpty)
    }
    
    test("exclusive minimum validation") {
      val schema = NumberChez(exclusiveMinimum = Some(0.0))
      assert(schema.validate(0.1) == Nil)
      assert(schema.validate(1.0) == Nil)
      assert(schema.validate(0.0).nonEmpty)
      assert(schema.validate(-0.1).nonEmpty)
    }
    
    test("exclusive maximum validation") {
      val schema = NumberChez(exclusiveMaximum = Some(10.0))
      assert(schema.validate(9.9) == Nil)
      assert(schema.validate(5.0) == Nil)
      assert(schema.validate(10.0).nonEmpty)
      assert(schema.validate(10.1).nonEmpty)
    }
    
    test("multiple of validation") {
      val schema = NumberChez(multipleOf = Some(0.5))
      assert(schema.validate(1.0) == Nil)
      assert(schema.validate(1.5) == Nil)
      assert(schema.validate(0.0) == Nil)
      assert(schema.validate(0.75).nonEmpty)
      assert(schema.validate(1.3).nonEmpty)
    }
    
    test("const validation") {
      val schema = NumberChez(const = Some(3.14))
      assert(schema.validate(3.14) == Nil)
      assert(schema.validate(3.15).nonEmpty)
      assert(schema.validate(0.0).nonEmpty)
    }
    
    test("enum validation - moved to EnumChez") {
      // Note: enum validation is now handled by EnumChez, not NumberChez
      // This test exists for backward compatibility documentation
      val enumSchema = chez.primitives.EnumChez.fromNumbers(1.0, 2.5, 5.0)
      assert(enumSchema.validateNumber(1.0) == Nil)
      assert(enumSchema.validateNumber(2.5) == Nil)
      assert(enumSchema.validateNumber(5.0) == Nil)
      assert(enumSchema.validateNumber(3.0).nonEmpty)
      assert(enumSchema.validateNumber(0.0).nonEmpty)
    }
    
    test("precision edge cases") {
      val schema = NumberChez()
      assert(schema.validate(Double.MaxValue) == Nil)
      assert(schema.validate(Double.MinValue) == Nil)
      assert(schema.validate(Double.PositiveInfinity) == Nil)
      assert(schema.validate(Double.NegativeInfinity) == Nil)
    }
    
    test("combined validations") {
      val schema = NumberChez(
        minimum = Some(0.0),
        maximum = Some(100.0),
        multipleOf = Some(1.0)
      )
      assert(schema.validate(50.0) == Nil)
      assert(schema.validate(0.0) == Nil)
      assert(schema.validate(99.0) == Nil)
      assert(schema.validate(-0.1).nonEmpty) // below minimum
      assert(schema.validate(100.1).nonEmpty) // above maximum
      assert(schema.validate(50.5).nonEmpty) // not multiple of 1.0
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