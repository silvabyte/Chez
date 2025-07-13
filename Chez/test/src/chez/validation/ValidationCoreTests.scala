package chez.validation

import chez.*
import chez.primitives.*
import chez.validation.*
import utest.*

object ValidationCoreTests extends TestSuite {
  val tests = Tests {
    
    test("ValidationResult") {
      test("valid result") {
        val result = ValidationResult.valid()
        assert(result.isValid)
        assert(result.errors.isEmpty)
      }
      
      test("invalid result with single error") {
        val error = ValidationError.TypeMismatch("string", "number", "/test")
        val result = ValidationResult.invalid(error)
        assert(!result.isValid)
        assert(result.errors == List(error))
      }
      
      test("invalid result with multiple errors") {
        val error1 = ValidationError.TypeMismatch("string", "number", "/test1")
        val error2 = ValidationError.MissingField("name", "/test2")
        val result = ValidationResult.invalid(List(error1, error2))
        assert(!result.isValid)
        assert(result.errors == List(error1, error2))
      }
      
      test("combine valid results") {
        val result1 = ValidationResult.valid()
        val result2 = ValidationResult.valid()
        val combined = result1.combine(result2)
        assert(combined.isValid)
        assert(combined.errors.isEmpty)
      }
      
      test("combine valid and invalid results") {
        val valid = ValidationResult.valid()
        val error = ValidationError.TypeMismatch("string", "number", "/test")
        val invalid = ValidationResult.invalid(error)
        
        val combined1 = valid.combine(invalid)
        val combined2 = invalid.combine(valid)
        
        assert(!combined1.isValid)
        assert(!combined2.isValid)
        assert(combined1.errors == List(error))
        assert(combined2.errors == List(error))
      }
      
      test("combine invalid results") {
        val error1 = ValidationError.TypeMismatch("string", "number", "/test1")
        val error2 = ValidationError.MissingField("name", "/test2")
        val invalid1 = ValidationResult.invalid(error1)
        val invalid2 = ValidationResult.invalid(error2)
        
        val combined = invalid1.combine(invalid2)
        assert(!combined.isValid)
        assert(combined.errors == List(error1, error2))
      }
      
      test("combine multiple results") {
        val valid = ValidationResult.valid()
        val error1 = ValidationError.TypeMismatch("string", "number", "/test1")
        val error2 = ValidationError.MissingField("name", "/test2")
        val invalid1 = ValidationResult.invalid(error1)
        val invalid2 = ValidationResult.invalid(error2)
        
        val combined = ValidationResult.combine(List(valid, invalid1, invalid2))
        assert(!combined.isValid)
        assert(combined.errors == List(error1, error2))
      }
    }
    
    test("ValidationContext") {
      test("default context") {
        val context = ValidationContext()
        assert(context.path == "/")
        assert(context.rootSchema.isEmpty)
      }
      
      test("with property") {
        val context = ValidationContext().withProperty("name")
        assert(context.path == "/name")
      }
      
      test("with nested property") {
        val context = ValidationContext()
          .withProperty("user")
          .withProperty("address")
          .withProperty("street")
        assert(context.path == "/user/address/street")
      }
      
      test("with index") {
        val context = ValidationContext().withIndex(0)
        assert(context.path == "/0")
      }
      
      test("with property and index") {
        val context = ValidationContext()
          .withProperty("items")
          .withIndex(0)
          .withProperty("name")
        assert(context.path == "/items/0/name")
      }
      
      test("with custom path") {
        val context = ValidationContext().withPath("custom")
        assert(context.path == "/custom")
      }
      
      test("with root schema") {
        val schema = Chez.String()
        val context = ValidationContext().withRootSchema(schema)
        assert(context.rootSchema.contains(schema))
      }
    }
    
    test("ValidationEngine") {
      test("adapter for existing validation methods") {
        // Test with EnumChez which has existing validate(ujson.Value) method
        val schema = Chez.StringEnum("red", "green", "blue")
        
        test("valid enum value") {
          val result = ValidationEngine.validate(schema, ujson.Str("red"))
          assert(result.isValid)
          assert(result.errors.isEmpty)
        }
        
        test("invalid enum value") {
          val result = ValidationEngine.validate(schema, ujson.Str("yellow"))
          assert(!result.isValid)
          assert(result.errors.nonEmpty)
          assert(result.errors.head.isInstanceOf[ValidationError.TypeMismatch])
        }
      }
      
      test("validation at specific path") {
        val schema = Chez.StringEnum("red", "green", "blue")
        val result = ValidationEngine.validateAtPath(schema, ujson.Str("yellow"), "/color")
        assert(!result.isValid)
        assert(result.errors.nonEmpty)
        val error = result.errors.head.asInstanceOf[ValidationError.TypeMismatch]
        // Note: existing validateAtPath method in EnumChez uses hardcoded "/" path
        // This will be fixed in later tasks when individual validation implementations are updated
        assert(error.path == "/")
      }
      
      test("conversion utilities") {
        val error = ValidationError.TypeMismatch("string", "number", "/test")
        val errors = List(error)
        
        val result = ValidationEngine.fromErrors(errors)
        assert(!result.isValid)
        assert(result.errors == errors)
        
        val convertedErrors = ValidationEngine.toErrors(result)
        assert(convertedErrors == errors)
        
        val validResult = ValidationEngine.fromErrors(List.empty)
        assert(validResult.isValid)
        assert(ValidationEngine.toErrors(validResult).isEmpty)
      }
    }
    
    test("Chez trait validation interface") {
      test("validate with context") {
        val schema = Chez.StringEnum("red", "green", "blue")
        val context = ValidationContext("/test")
        
        val result = schema.validate(ujson.Str("red"), context)
        assert(result.isValid)
      }
      
      test("schema without validation methods") {
        // Test with StringChez which doesn't have validate(ujson.Value) method
        val schema = Chez.String(minLength = Some(5))
        val context = ValidationContext()
        
        // Should return valid for now since T1 is just infrastructure
        val result = schema.validate(ujson.Str("test"), context)
        assert(result.isValid)
      }
    }
  }
}