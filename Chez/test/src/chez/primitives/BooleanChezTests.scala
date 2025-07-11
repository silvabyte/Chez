package chez.primitives

import utest.*
import chez.primitives.*

object BooleanChezTests extends TestSuite {

  val tests = Tests {
    test("basic boolean schema") {
      val schema = BooleanChez()
      assert(schema.validate(true) == Nil)
      assert(schema.validate(false) == Nil)
    }

    test("const true validation") {
      val schema = BooleanChez(const = Some(true))
      assert(schema.validate(true) == Nil)
      assert(schema.validate(false).nonEmpty)
    }

    test("const false validation") {
      val schema = BooleanChez(const = Some(false))
      assert(schema.validate(false) == Nil)
      assert(schema.validate(true).nonEmpty)
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
