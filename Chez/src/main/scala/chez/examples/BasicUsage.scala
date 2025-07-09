package chez.examples

import chez.*

/**
 * Basic usage examples of the Chez library
 */
object BasicUsage {

  def main(args: Array[String]): Unit = {
    println("🎉 Chez Library - JSON Schema generation using upickle for Scala 3!")
    println("=" * 50)

    // Basic primitive schemas
    println("\n1. Primitive Schemas:")

    val stringSchema = Chez.String(
      minLength = Some(1),
      maxLength = Some(100),
      pattern = Some("^[a-zA-Z]+$")
    )
    println(s"String Schema: ${stringSchema.toJsonSchema}")

    val numberSchema = Chez.Number(
      minimum = Some(0.0),
      maximum = Some(100.0),
      multipleOf = Some(0.1)
    )
    println(s"Number Schema: ${numberSchema.toJsonSchema}")

    val integerSchema = Chez.Integer(
      minimum = Some(1),
      maximum = Some(1000)
    )
    println(s"Integer Schema: ${integerSchema.toJsonSchema}")

    // Complex schemas
    println("\n2. Complex Schemas:")

    val userSchema = Chez.Object(
      "id" -> Chez.String(),
      "name" -> Chez.String(minLength = Some(1)),
      "email" -> Chez.String(format = Some("email")),
      "age" -> Chez.Integer(minimum = Some(0)).optional
    )
    println(s"User Schema: ${userSchema.toJsonSchema}")

    val arraySchema = Chez.Array(
      Chez.String(),
      minItems = Some(1),
      maxItems = Some(10),
      uniqueItems = Some(true)
    )
    println(s"Array Schema: ${arraySchema.toJsonSchema}")

    // Null handling with modifier pattern
    println("\n3. Null Handling:")

    val nullableString = Chez.String().nullable
    println(s"Nullable String: ${nullableString.toJsonSchema}")

    val optionalString = Chez.String().optional
    println(s"Optional String: ${optionalString.toJsonSchema}")

    val optionalNullableString = Chez.String().optional.nullable
    println(s"Optional Nullable String: ${optionalNullableString.toJsonSchema}")

    // JSON Schema 2020-12 composition keywords
    println("\n4. Composition Keywords:")

    val anyOfSchema = Chez.AnyOf(
      Chez.String(),
      Chez.Number()
    )
    println(s"AnyOf Schema: ${anyOfSchema.toJsonSchema}")

    val oneOfSchema = Chez.OneOf(
      Chez.String(),
      Chez.Integer()
    )
    println(s"OneOf Schema: ${oneOfSchema.toJsonSchema}")

    val allOfSchema = Chez.AllOf(
      Chez.Object("name" -> Chez.String()),
      Chez.Object("age" -> Chez.Integer())
    )
    println(s"AllOf Schema: ${allOfSchema.toJsonSchema}")

    val notSchema = Chez.Not(Chez.String())
    println(s"Not Schema: ${notSchema.toJsonSchema}")

    // Conditional schemas (if/then/else)
    println("\n5. Conditional Schemas:")

    val conditionalSchema = Chez.If(
      condition = Chez.Object("type" -> Chez.String()),
      thenSchema = Chez.Object("name" -> Chez.String()),
      elseSchema = Chez.Object("id" -> Chez.Integer())
    )
    println(s"Conditional Schema: ${conditionalSchema.toJsonSchema}")

    // References
    println("\n6. References:")

    val refSchema = Chez.Ref("#/$defs/User")
    println(s"Reference Schema: ${refSchema.toJsonSchema}")

    val dynamicRefSchema = Chez.DynamicRef("#user")
    println(s"Dynamic Reference Schema: ${dynamicRefSchema.toJsonSchema}")

    // Complex nested example
    println("\n7. Complex Nested Example:")

    val productSchema = Chez.Object(
      "id" -> Chez.String(),
      "name" -> Chez.String(minLength = Some(1)),
      "price" -> Chez.Number(minimum = Some(0)),
      "category" -> Chez.OneOf(
        Chez.String(),
        Chez.Object("id" -> Chez.String(), "name" -> Chez.String())
      ),
      "tags" -> Chez.Array(Chez.String()).optional,
      "metadata" -> Chez.Object().optional.nullable
    )

    println(s"Product Schema: ${productSchema.toJsonSchema}")

    // Demonstrate JSON Schema 2020-12 compliance
    println("\n8. JSON Schema 2020-12 Compliance:")

    val compliantSchema = Chez
      .Object(
        "version" -> Chez.String()
      )
      .withSchema(Chez.MetaSchemaUrl)
      .withId("https://example.com/schemas/product")
      .withTitle("Product Schema")
      .withDescription("A schema for product objects")

    println(s"Compliant Schema: ${compliantSchema.toJsonSchema}")

    println("\n🎯 All examples completed successfully!")
    println("Chez provides full JSON Schema 2020-12 compliance with TypeBox-like ergonomics!")
  }
}
