package chez.primitives

import utest.*
import chez.primitives.*
import chez.*
import chez.validation.*

object StringChezTests extends TestSuite {

  val tests = Tests {
    test("basic string schema") {
      val schema = StringChez()
      val result1 = schema.validate(ujson.Str("hello"), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Str(""), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Str("multiple words"), ValidationContext())
      assert(result3.isValid)
    }

    test("min length validation") {
      val schema = StringChez(minLength = Some(5))
      val result1 = schema.validate(ujson.Str("hello"), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Str("longer string"), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Str("hi"), ValidationContext())
      assert(!result3.isValid)
      val result4 = schema.validate(ujson.Str(""), ValidationContext())
      assert(!result4.isValid)
    }

    test("max length validation") {
      val schema = StringChez(maxLength = Some(10))
      val result1 = schema.validate(ujson.Str("short"), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Str("exactly10!"), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Str("this is way too long"), ValidationContext())
      assert(!result3.isValid)
    }

    test("pattern validation") {
      val schema = StringChez(pattern = Some("^[a-zA-Z]+$"))
      val result1 = schema.validate(ujson.Str("hello"), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Str("OnlyLetters"), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Str("hello123"), ValidationContext())
      assert(!result3.isValid)
      val result4 = schema.validate(ujson.Str("hello world"), ValidationContext())
      assert(!result4.isValid)
      val result5 = schema.validate(ujson.Str("123"), ValidationContext())
      assert(!result5.isValid)
    }

    test("const validation") {
      val schema = StringChez(const = Some("exactly this"))
      val result1 = schema.validate(ujson.Str("exactly this"), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Str("not this"), ValidationContext())
      assert(!result2.isValid)
      val result3 = schema.validate(ujson.Str("EXACTLY THIS"), ValidationContext())
      assert(!result3.isValid)
    }

    test("enum validation - moved to EnumChez") {
      // Note: enum validation is now handled by EnumChez, not StringChez
      // This test exists for backward compatibility documentation
      val enumSchema = chez.primitives.EnumChez.fromStrings("red", "green", "blue")
      val result1 = enumSchema.validate(ujson.Str("red"), ValidationContext())
      assert(result1.isValid)
      val result2 = enumSchema.validate(ujson.Str("green"), ValidationContext())
      assert(result2.isValid)
      val result3 = enumSchema.validate(ujson.Str("blue"), ValidationContext())
      assert(result3.isValid)
      val result4 = enumSchema.validate(ujson.Str("yellow"), ValidationContext())
      assert(!result4.isValid)
      val result5 = enumSchema.validate(ujson.Str("Red"), ValidationContext())
      assert(!result5.isValid) // case sensitive
    }

    test("email format validation") {
      val schema = StringChez(format = Some("email"))
      val result1 = schema.validate(ujson.Str("user@example.com"), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Str("test.email+tag@domain.co.uk"), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Str("not-an-email"), ValidationContext())
      assert(!result3.isValid)
      val result4 = schema.validate(ujson.Str("@domain.com"), ValidationContext())
      assert(!result4.isValid)
      val result5 = schema.validate(ujson.Str("user@"), ValidationContext())
      assert(!result5.isValid)
    }

    test("uri format validation") {
      val schema = StringChez(format = Some("uri"))
      val result1 = schema.validate(ujson.Str("https://example.com"), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Str("http://localhost:8080/path"), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Str("ftp://files.example.org"), ValidationContext())
      assert(result3.isValid)
      val result4 = schema.validate(ujson.Str("not a uri"), ValidationContext())
      assert(!result4.isValid)
      val result5 = schema.validate(ujson.Str("://invalid"), ValidationContext())
      assert(!result5.isValid)
    }

    test("uuid format validation") {
      val schema = StringChez(format = Some("uuid"))
      val result1 =
        schema.validate(ujson.Str("550e8400-e29b-41d4-a716-446655440000"), ValidationContext())
      assert(result1.isValid)
      val result2 =
        schema.validate(ujson.Str("6ba7b810-9dad-11d1-80b4-00c04fd430c8"), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Str("not-a-uuid"), ValidationContext())
      assert(!result3.isValid)
      val result4 = schema.validate(ujson.Str("550e8400-e29b-41d4-a716"), ValidationContext())
      assert(!result4.isValid) // too short
      val result5 = schema.validate(
        ujson.Str("550e8400-e29b-41d4-a716-446655440000-extra"),
        ValidationContext()
      )
      assert(!result5.isValid)
    }

    test("date format validation") {
      val schema = StringChez(format = Some("date"))
      val result1 = schema.validate(ujson.Str("2023-12-25"), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Str("1999-01-01"), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Str("not-a-date"), ValidationContext())
      assert(!result3.isValid)
      val result4 = schema.validate(ujson.Str("12-25-2023"), ValidationContext())
      assert(!result4.isValid) // wrong format
      val result5 = schema.validate(ujson.Str("2023/12/25"), ValidationContext())
      assert(!result5.isValid) // wrong separator
    }

    test("time format validation") {
      val schema = StringChez(format = Some("time"))
      val result1 = schema.validate(ujson.Str("14:30:00"), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Str("09:05:15"), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Str("not-a-time"), ValidationContext())
      assert(!result3.isValid)
      val result4 = schema.validate(ujson.Str("2:30:00"), ValidationContext())
      assert(!result4.isValid) // missing leading zero
      val result5 = schema.validate(ujson.Str("14:30"), ValidationContext())
      assert(!result5.isValid) // missing seconds
    }

    test("date-time format validation") {
      val schema = StringChez(format = Some("date-time"))
      val result1 = schema.validate(ujson.Str("2023-12-25T14:30:00"), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Str("1999-01-01T00:00:00"), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Str("not-datetime"), ValidationContext())
      assert(!result3.isValid)
      val result4 = schema.validate(ujson.Str("2023-12-25 14:30:00"), ValidationContext())
      assert(!result4.isValid) // space instead of T
    }

    test("unknown format validation") {
      val schema = StringChez(format = Some("unknown-format"))
      // Unknown formats should not cause validation errors
      val result1 = schema.validate(ujson.Str("any string"), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Str("12345"), ValidationContext())
      assert(result2.isValid)
    }

    test("combined validations") {
      val schema = StringChez(
        minLength = Some(8),
        maxLength = Some(20),
        pattern = Some("^[a-zA-Z0-9]+$")
      )
      val result1 = schema.validate(ujson.Str("Password123"), ValidationContext())
      assert(result1.isValid)
      val result2 = schema.validate(ujson.Str("abcd1234"), ValidationContext())
      assert(result2.isValid)
      val result3 = schema.validate(ujson.Str("abc123"), ValidationContext())
      assert(!result3.isValid) // too short
      val result4 = schema.validate(ujson.Str("this_password_is_way_too_long"), ValidationContext())
      assert(!result4.isValid) // too long
      val result5 = schema.validate(ujson.Str("Password 123"), ValidationContext())
      assert(!result5.isValid) // contains space
    }

    test("json schema generation") {
      val schema = StringChez(
        minLength = Some(1),
        maxLength = Some(100),
        pattern = Some("^[a-zA-Z]+$")
      )
      val json = schema.toJsonSchema
      assert(json("type").str == "string")
      assert(json("minLength").num == 1)
      assert(json("maxLength").num == 100)
      assert(json("pattern").str == "^[a-zA-Z]+$")
    }

    test("default value support") {
      val schema = StringChez().withDefault(ujson.Str("hello"))
      val json = schema.toJsonSchema
      assert(json("default").str == "hello")

      val schemaEmpty = StringChez(minLength = Some(0)).withDefault(ujson.Str(""))
      val jsonEmpty = schemaEmpty.toJsonSchema
      assert(jsonEmpty("default").str == "")
      assert(jsonEmpty("minLength").num == 0)

      val schemaEmail =
        StringChez(format = Some("email")).withDefault(ujson.Str("user@example.com"))
      val jsonEmail = schemaEmail.toJsonSchema
      assert(jsonEmail("default").str == "user@example.com")
      assert(jsonEmail("format").str == "email")
    }

    test("StringChez validates ujson.Str values correctly") {
      val schema = Chez.String(minLength = Some(3), maxLength = Some(10))

      test("valid string") {
        val result = schema.validate(ujson.Str("hello"))
        assert(result.isValid)
        assert(result.errors.isEmpty)
      }

      test("string too short") {
        val result = schema.validate(ujson.Str("hi"))
        assert(!result.isValid)
        assert(result.errors.length == 1)
        assert(result.errors.head.isInstanceOf[ValidationError.MinLengthViolation])
      }

      test("string too long") {
        val result = schema.validate(ujson.Str("this is way too long"))
        assert(!result.isValid)
        assert(result.errors.length == 1)
        assert(result.errors.head.isInstanceOf[ValidationError.MaxLengthViolation])
      }
    }

    test("StringChez rejects non-string ujson.Value types with TypeMismatch error") {
      val schema = Chez.String()

      test("number value") {
        val result = schema.validate(ujson.Num(42))
        assert(!result.isValid)
        assert(result.errors.length == 1)
        val error = result.errors.head.asInstanceOf[ValidationError.TypeMismatch]
        assert(error.expected == "string")
        assert(error.actual == "number")
      }

      test("boolean value") {
        val result = schema.validate(ujson.Bool(true))
        assert(!result.isValid)
        assert(result.errors.length == 1)
        val error = result.errors.head.asInstanceOf[ValidationError.TypeMismatch]
        assert(error.expected == "string")
        assert(error.actual == "boolean")
      }

      test("null value") {
        val result = schema.validate(ujson.Null)
        assert(!result.isValid)
        assert(result.errors.length == 1)
        val error = result.errors.head.asInstanceOf[ValidationError.TypeMismatch]
        assert(error.expected == "string")
        assert(error.actual == "null")
      }

      test("array value") {
        val result = schema.validate(ujson.Arr(ujson.Str("test")))
        assert(!result.isValid)
        assert(result.errors.length == 1)
        val error = result.errors.head.asInstanceOf[ValidationError.TypeMismatch]
        assert(error.expected == "string")
        assert(error.actual == "array")
      }

      test("object value") {
        val result = schema.validate(ujson.Obj("key" -> ujson.Str("value")))
        assert(!result.isValid)
        assert(result.errors.length == 1)
        val error = result.errors.head.asInstanceOf[ValidationError.TypeMismatch]
        assert(error.expected == "string")
        assert(error.actual == "object")
      }
    }

    test("All existing validation logic works with ujson.Value") {
      test("pattern validation") {
        val schema = Chez.String(pattern = Some("^[A-Z]+$"))

        val validResult = schema.validate(ujson.Str("HELLO"))
        assert(validResult.isValid)

        val invalidResult = schema.validate(ujson.Str("hello"))
        assert(!invalidResult.isValid)
        assert(invalidResult.errors.head.isInstanceOf[ValidationError.PatternMismatch])
      }

      test("const validation") {
        val schema = Chez.String(const = Some("expected"))

        val validResult = schema.validate(ujson.Str("expected"))
        assert(validResult.isValid)

        val invalidResult = schema.validate(ujson.Str("unexpected"))
        assert(!invalidResult.isValid)
        assert(invalidResult.errors.head.isInstanceOf[ValidationError.TypeMismatch])
      }

      test("format validation") {
        val emailSchema = Chez.String(format = Some("email"))

        val validResult = emailSchema.validate(ujson.Str("test@example.com"))
        assert(validResult.isValid)

        val invalidResult = emailSchema.validate(ujson.Str("not-an-email"))
        assert(!invalidResult.isValid)
        assert(invalidResult.errors.head.isInstanceOf[ValidationError.InvalidFormat])
      }

      test("multiple constraint validation") {
        val schema = Chez.String(
          minLength = Some(5),
          maxLength = Some(15),
          pattern = Some("^[a-z]+$")
        )

        val validResult = schema.validate(ujson.Str("hello"))
        assert(validResult.isValid)

        val invalidResult = schema.validate(ujson.Str("Hi"))
        assert(!invalidResult.isValid)
        // Should have both minLength and pattern violations
        assert(invalidResult.errors.length == 2)
      }
    }

    test("Error paths are correctly set using ValidationContext") {
      val schema = Chez.String(minLength = Some(5))

      test("default context") {
        val result = schema.validate(ujson.Str("hi"))
        assert(!result.isValid)
        val error = result.errors.head.asInstanceOf[ValidationError.MinLengthViolation]
        assert(error.path == "/")
      }

      test("custom context path") {
        val context = ValidationContext("/user/name")
        val result = schema.validate(ujson.Str("hi"), context)
        assert(!result.isValid)
        val error = result.errors.head.asInstanceOf[ValidationError.MinLengthViolation]
        assert(error.path == "/user/name")
      }

      test("nested context path") {
        val context = ValidationContext()
          .withProperty("user")
          .withProperty("profile")
          .withProperty("name")
        val result = schema.validate(ujson.Str("hi"), context)
        assert(!result.isValid)
        val error = result.errors.head.asInstanceOf[ValidationError.MinLengthViolation]
        assert(error.path == "/user/profile/name")
      }

      test("type mismatch error path") {
        val context = ValidationContext("/data/field")
        val result = schema.validate(ujson.Num(42), context)
        assert(!result.isValid)
        val error = result.errors.head.asInstanceOf[ValidationError.TypeMismatch]
        assert(error.path == "/data/field")
      }
    }

    test("ValidationResult is returned with proper valid/invalid state") {
      val schema = Chez.String(minLength = Some(3))

      test("valid state") {
        val result = schema.validate(ujson.Str("hello"))
        assert(result.isValid)
        assert(result.errors.isEmpty)
        assert(result.isInstanceOf[ValidationResult.Valid.type])
      }

      test("invalid state") {
        val result = schema.validate(ujson.Str("hi"))
        assert(!result.isValid)
        assert(result.errors.nonEmpty)
        assert(result.isInstanceOf[ValidationResult.Invalid])
      }
    }

    test("String validation using new ValidationResult framework") {
      val schema = Chez.String(minLength = Some(3), pattern = Some("^[a-z]+$"))

      test("valid string") {
        val result = schema.validate(ujson.Str("hello"), ValidationContext())
        assert(result.isValid)
      }

      test("invalid string") {
        val result = schema.validate(ujson.Str("Hi"), ValidationContext())
        assert(!result.isValid)
        assert(result.errors.length == 2) // Both minLength and pattern violations
        assert(result.errors.exists(_.isInstanceOf[ValidationError.MinLengthViolation]))
        assert(result.errors.exists(_.isInstanceOf[ValidationError.PatternMismatch]))
      }

      test("string too short") {
        val result = schema.validate(ujson.Str("hi"), ValidationContext())
        assert(!result.isValid)
        assert(result.errors.length == 1)
        assert(result.errors.head.isInstanceOf[ValidationError.MinLengthViolation])
      }

      test("errors use default path") {
        val result = schema.validate(ujson.Str("X"), ValidationContext())
        assert(!result.isValid)
        // ValidationContext() should use "/" path
        result.errors.foreach { error =>
          val path = error match {
            case ValidationError.MinLengthViolation(_, _, path) => path
            case ValidationError.PatternMismatch(_, _, path) => path
            case _ => "/"
          }
          assert(path == "/")
        }
      }
    }

    test("Direct schema validation") {
      val schema = Chez.String(const = Some("test"))

      test("direct validate works") {
        val result = schema.validate(ujson.Str("test"), ValidationContext())
        assert(result.isValid)

        val invalidResult = schema.validate(ujson.Str("wrong"), ValidationContext())
        assert(!invalidResult.isValid)
      }

      test("validate with custom path works") {
        val context = ValidationContext("/custom/path")
        val result = schema.validate(ujson.Str("wrong"), context)
        assert(!result.isValid)
        assert(result.errors.head.asInstanceOf[ValidationError.TypeMismatch].path == "/custom/path")
      }
    }
  }
}
