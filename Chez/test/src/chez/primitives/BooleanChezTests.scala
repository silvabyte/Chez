package chez.primitives

import utest.*
import chez.primitives.*
import chez.validation.ValidationContext
import chez.*

object BooleanChezTests extends TestSuite {

  val tests = Tests {
    test("basic boolean schema") {
      val schema = BooleanChez()
      val result1 = schema.validate(ujson.Bool(true), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Bool(false), ValidationContext())
      assert(result2.isValid)
    }

    test("const true validation") {
      val schema = BooleanChez(const = Some(true))
      val result1 = schema.validate(ujson.Bool(true), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Bool(false), ValidationContext())
      assert(!result2.isValid)
    }

    test("const false validation") {
      val schema = BooleanChez(const = Some(false))
      val result1 = schema.validate(ujson.Bool(false), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Bool(true), ValidationContext())
      assert(!result2.isValid)
    }

    test("json schema generation") {
      val schema = BooleanChez()
      val json = schema.toJsonSchema
      assert(json("type").str == "boolean")
    }

    test("default value support") {
      val schema = BooleanChez().withDefault(ujson.Bool(true))
      val json = schema.toJsonSchema
      assert(json("default").bool == true)

      val schemaFalse = BooleanChez().withDefault(ujson.Bool(false))
      val jsonFalse = schemaFalse.toJsonSchema
      assert(jsonFalse("default").bool == false)
    }
  }
}
