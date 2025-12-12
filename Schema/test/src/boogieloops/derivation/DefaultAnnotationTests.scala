package boogieloops.schema.derivation

import utest.*
import boogieloops.schema.*
import boogieloops.schema.derivation.Schematic

object DefaultAnnotationSchemaTests extends TestSuite {

  val tests = Tests {
    test("all default annotation types compile") {
      case class ComprehensiveDefaults(
          @Schematic.default(42) count: Int,
          @Schematic.default(true) enabled: Boolean,
          @Schematic.default(3.14) pi: Double,
          @Schematic.default("hello") greeting: String
      ) derives Schematic

      val schema = Schematic[ComprehensiveDefaults]
      val json = schema.toJsonSchema
      val props = json("properties").obj

      assert(json("type").str == "object")

      // Verify all default values are correctly included in the JSON schema
      assert(props("count")("default").num == 42)
      assert(props("enabled")("default").bool == true)
      assert(props("pi")("default").num == 3.14)
      assert(props("greeting")("default").str == "hello")
    }

    test("optional fields with defaults") {
      case class OptionalDefaults(
          @Schematic.default(8080) port: Option[Int],
          @Schematic.default(false) ssl: Option[Boolean],
          @Schematic.default(30.0) timeout: Option[Double],
          @Schematic.default("localhost") host: Option[String]
      ) derives Schematic

      val schema = Schematic[OptionalDefaults]
      val json = schema.toJsonSchema
      val props = json("properties").obj

      assert(json("type").str == "object")

      // Verify default values work with Optional types
      assert(props("port")("default").num == 8080)
      assert(props("ssl")("default").bool == false)
      assert(props("timeout")("default").num == 30.0)
      assert(props("host")("default").str == "localhost")
    }

    test("defaults mixed with other annotations") {
      case class AnnotatedConfig(
          @Schematic.default(3000)
          @Schematic.minimum(1000)
          @Schematic.maximum(65535) port: Int,
          @Schematic.default(true)
          @Schematic.description("Enable SSL encryption") ssl: Boolean,
          @Schematic.default("production")
          @Schematic.enumValues("development", "staging", "production") environment: String,
          @Schematic.default(2.5)
          @Schematic.minimum(0.0)
          @Schematic.maximum(10.0) multiplier: Double
      ) derives Schematic

      val schema = Schematic[AnnotatedConfig]
      val json = schema.toJsonSchema
      val props = json("properties").obj

      // Verify other annotations work (proves annotation processing is functional)
      assert(props("ssl")("description").str == "Enable SSL encryption")
      assert(props("port")("minimum").num == 1000.0)
      assert(props("port")("maximum").num == 65535.0)
      assert(props("multiplier")("minimum").num == 0.0)
      assert(props("multiplier")("maximum").num == 10.0)

      // Verify default values work alongside other annotations
      assert(props("port")("default").num == 3000)
      assert(props("ssl")("default").bool == true)
      assert(props("environment")("default").str == "production")
      assert(props("multiplier")("default").num == 2.5)
    }

    test("negative and zero default values") {
      case class EdgeCaseDefaults(
          @Schematic.default(-1) negative: Int,
          @Schematic.default(0) zero: Int,
          @Schematic.default(-3.14) negativeDouble: Double,
          @Schematic.default(0.0) zeroDouble: Double,
          @Schematic.default(false) disabled: Boolean,
          @Schematic.default("") empty: String
      ) derives Schematic

      val schema = Schematic[EdgeCaseDefaults]
      val json = schema.toJsonSchema
      val props = json("properties").obj

      assert(json("type").str == "object")

      // Verify edge case default values are handled correctly
      assert(props("negative")("default").num == -1)
      assert(props("zero")("default").num == 0)
      assert(props("negativeDouble")("default").num == -3.14)
      assert(props("zeroDouble")("default").num == 0.0)
      assert(props("disabled")("default").bool == false)
      assert(props("empty")("default").str == "")
    }

    test("nested case classes with defaults") {
      case class DatabaseConfig(
          @Schematic.default("localhost") host: String,
          @Schematic.default(5432) port: Int,
          @Schematic.default(true) ssl: Boolean
      ) derives Schematic

      case class AppConfig(
          @Schematic.default("MyApp") name: String,
          @Schematic.default(1.0) version: Double,
          database: DatabaseConfig
      ) derives Schematic

      val appSchema = Schematic[AppConfig]
      val dbSchema = Schematic[DatabaseConfig]

      val appJson = appSchema.toJsonSchema
      val dbJson = dbSchema.toJsonSchema

      assert(appJson("type").str == "object")
      assert(dbJson("type").str == "object")

      // Verify defaults work in nested structures
      val appProps = appJson("properties").obj
      val dbProps = dbJson("properties").obj

      assert(appProps("name")("default").str == "MyApp")
      assert(appProps("version")("default").num == 1.0)

      assert(dbProps("host")("default").str == "localhost")
      assert(dbProps("port")("default").num == 5432)
      assert(dbProps("ssl")("default").bool == true)
    }
  }
}
