# Annotations Guide

Chez provides rich annotation support for JSON Schema validation. Annotations define constraints and metadata directly in your types, enabling precise validation at boundaries.

## Basic Usage

Apply annotations to case class fields using `@Schema.*`:

```scala
import chez.derivation.Schema

@Schema.title("User Profile")
@Schema.description("System user")
case class User(
  @Schema.description("Unique ID")
  @Schema.format("uuid")
  id: String,

  @Schema.description("Display name")
  @Schema.minLength(2)
  @Schema.maxLength(100)
  name: String,

  @Schema.description("User age")
  @Schema.minimum(0)
  @Schema.maximum(120)
  age: Int
) derives Schema
```

## String Annotations

### Length Constraints

```scala
case class Message(
  @Schema.minLength(1)      // At least 1 character
  @Schema.maxLength(280)    // At most 280 characters
  content: String
) derives Schema
```

### Format Validation

```scala
case class Contact(
  @Schema.format("email")       // RFC 5322 email format
  email: String,

  @Schema.format("uuid")        // UUID format
  id: String,

  @Schema.format("date")        // ISO 8601 date
  birthDate: String,

  @Schema.format("uri")         // Valid URI
  website: String
) derives Schema
```

### Pattern Matching

```scala
case class Product(
  @Schema.pattern("^prod-[0-9]+$")     // Product ID format
  id: String,

  @Schema.pattern("^[a-z0-9-]+$")      // Slug format
  slug: String,

  @Schema.pattern("^\\+?[1-9]\\d{1,14}$")  // International phone
  phone: String
) derives Schema
```

### Constant Values

```scala
case class ApiResponse(
  @Schema.const("success")      // Must be exactly "success"
  status: String,

  @Schema.const("1.0")         // API version constant
  version: String
) derives Schema
```

## Number Annotations

### Range Constraints

```scala
case class Product(
  @Schema.minimum(0.01)         // At least $0.01
  @Schema.maximum(999999.99)    // At most $999,999.99
  price: Double,

  @Schema.exclusiveMinimum(0.0) // Greater than 0 (not equal)
  @Schema.exclusiveMaximum(100.0) // Less than 100 (not equal)
  percentage: Double,

  @Schema.multipleOf(0.01)      // Must be multiple of 0.01
  precisePrice: Double
) derives Schema
```

### Integer Constraints

```scala
case class User(
  @Schema.minimum(0)            // Non‑negative
  @Schema.maximum(150)          // Reasonable age limit
  age: Int,

  @Schema.multipleOf(5)         // Must be multiple of 5
  rating: Int
) derives Schema
```

## Array Annotations

### Size Constraints

```scala
case class User(
  @Schema.minItems(1)           // At least 1 tag
  @Schema.maxItems(10)          // At most 10 tags
  @Schema.uniqueItems(true)     // No duplicates
  tags: List[String],

  @Schema.minItems(0)           // Can be empty
  @Schema.maxItems(5)           // Max 5 items
  categories: List[String]
) derives Schema
```

## Enumeration Annotations

### String Enums

```scala
case class Order(
  @Schema.enumValues("pending", "processing", "shipped", "delivered")
  status: String
) derives Schema
```

### Mixed Type Enums

```scala
case class Setting(
  @Schema.enumValues("auto", 1, true, 3.14, null)
  value: String | Int | Boolean | Double | Null
) derives Schema
```

## Metadata Annotations

### Documentation

```scala
@Schema.title("Product Catalog Item")
@Schema.description("Represents a product in our e-commerce catalog")
@Schema.examples("""{"id": "prod-123", "name": "Widget", "price": 9.99}""")
case class Product(
  @Schema.description("Unique product identifier")
  @Schema.examples("prod-123", "prod-456")
  id: String,

  @Schema.description("Product display name")
  name: String,

  @Schema.description("Price in USD")
  price: Double
) derives Schema
```

### Field Visibility

```scala
case class User(
  @Schema.readOnly(true)        // Only in responses, not requests
  id: String,

  @Schema.writeOnly(true)       // Only in requests, not responses
  password: String,

  @Schema.deprecated(true)      // Mark as deprecated
  @Schema.description("Use 'fullName' instead")
  name: String,

  fullName: String
) derives Schema
```

## Default Values

```scala
case class Settings(
  @Schema.default("en")         // Default language
  language: String,

  @Schema.default(true)         // Default enabled
  notifications: Boolean,

  @Schema.default(10)           // Default page size
  pageSize: Int,

  @Schema.default(1.0)          // Default version
  version: Double
) derives Schema
```

## Complex Examples

### User Registration

```scala
@Schema.title("User Registration")
@Schema.description("New user registration data")
case class UserRegistration(
  @Schema.description("Username (3-20 alphanumeric + underscore)")
  @Schema.minLength(3)
  @Schema.maxLength(20)
  @Schema.pattern("^[a-zA-Z0-9_]+$")
  username: String,

  @Schema.description("Valid email address")
  @Schema.format("email")
  email: String,

  @Schema.description("Strong password (8+ chars, uppercase, digit)")
  @Schema.minLength(8)
  @Schema.pattern("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)")
  password: String,

  @Schema.description("User age (must be 13+)")
  @Schema.minimum(13)
  @Schema.maximum(120)
  age: Int,

  @Schema.description("Agreed to terms of service")
  @Schema.const(true)           // Must be true
  agreeToTerms: Boolean
) derives Schema
```

### E‑commerce Product

```scala
@Schema.title("Product")
@Schema.description("E-commerce product with full validation")
case class Product(
  @Schema.description("Product SKU")
  @Schema.pattern("^[A-Z]{3}-\\d{6}$")
  @Schema.examples("ELE-123456", "CLO-789012")
  sku: String,

  @Schema.description("Product name")
  @Schema.minLength(1)
  @Schema.maxLength(200)
  name: String,

  @Schema.description("Product description")
  @Schema.maxLength(2000)
  description: Option[String],

  @Schema.description("Price in USD (minimum $0.01)")
  @Schema.minimum(0.01)
  @Schema.maximum(999999.99)
  @Schema.multipleOf(0.01)
  price: Double,

  @Schema.description("Product category")
  @Schema.enumValues("electronics", "clothing", "books", "food", "other")
  category: String,

  @Schema.description("Product tags")
  @Schema.minItems(0)
  @Schema.maxItems(10)
  @Schema.uniqueItems(true)
  tags: List[String],

  @Schema.description("Currently in stock")
  @Schema.default(true)
  inStock: Boolean,

  @Schema.description("Product rating (1-5 stars)")
  @Schema.minimum(1.0)
  @Schema.maximum(5.0)
  @Schema.multipleOf(0.5)       // Half-star ratings
  rating: Option[Double]
) derives Schema
```

### API Configuration

```scala
@Schema.title("API Configuration")
@Schema.description("Service configuration with validation")
case class ApiConfig(
  @Schema.description("Service name")
  @Schema.minLength(1)
  @Schema.maxLength(50)
  @Schema.pattern("^[a-zA-Z][a-zA-Z0-9-]*$")
  serviceName: String,

  @Schema.description("Service version")
  @Schema.pattern("^\\d+\\.\\d+\\.\\d+$")
  @Schema.examples("1.0.0", "2.1.3")
  version: String,

  @Schema.description("Environment")
  @Schema.enumValues("development", "staging", "production")
  @Schema.default("development")
  environment: String,

  @Schema.description("Server port")
  @Schema.minimum(1024)
  @Schema.maximum(65535)
  @Schema.default(8080)
  port: Int,

  @Schema.description("Enable debug logging")
  @Schema.default(false)
  debug: Boolean,

  @Schema.description("Database connection string")
  @Schema.format("uri")
  @Schema.pattern("^(postgresql|mysql)://.*")
  databaseUrl: String,

  @Schema.description("Allowed origins for CORS")
  @Schema.minItems(0)
  @Schema.maxItems(20)
  allowedOrigins: List[String]
) derives Schema
```

## Annotation Combinations

Multiple annotations work together:

```scala
case class StrictField(
  @Schema.description("Highly constrained field")
  @Schema.minLength(5)          // At least 5 chars
  @Schema.maxLength(50)         // At most 50 chars
  @Schema.pattern("^[A-Z][a-zA-Z0-9]*$")  // Start with uppercase
  @Schema.format("custom")      // Custom format
  value: String
) derives Schema
```

## Validation Results

Annotations generate detailed validation errors:

```scala
val schema = Schema[User]
val json = ujson.Obj("name" -> "", "age" -> -5)

schema.validate(json) match {
  case Right(()) => println("Valid")
  case Left(errors) =>
    errors.foreach(println)
    // MinLengthViolation: name must be at least 2 characters
    // OutOfRange: age must be >= 0
}
```

## Best Practices

### 1. Document Everything

```scala
@Schema.title("Clear Title")
@Schema.description("Detailed description of purpose and usage")
case class WellDocumented(
  @Schema.description("Explain what this field represents")
  field: String
) derives Schema
```

### 2. Use Realistic Constraints

```scala
case class RealisticUser(
  @Schema.minLength(2)          // Reasonable minimum
  @Schema.maxLength(100)        // Reasonable maximum
  name: String,

  @Schema.minimum(13)           // Legal minimum
  @Schema.maximum(150)          // Biological maximum
  age: Int
) derives Schema
```

### 3. Provide Examples

```scala
@Schema.examples("""{"username": "john_doe", "email": "john@example.com"}""")
case class ExampleRich(
  @Schema.examples("john_doe", "jane_smith")
  username: String,

  @Schema.examples("john@example.com", "jane@company.org")
  email: String
) derives Schema
```

### 4. Use Enums for Fixed Sets

```scala
case class BetterThanStrings(
  @Schema.enumValues("small", "medium", "large", "xl")
  size: String,  // Better than unconstrained String

  @Schema.enumValues(1, 2, 3, 4, 5)
  priority: Int  // Better than unconstrained Int
) derives Schema
```

Annotations provide precise, self‑documenting validation that ensures data quality at your API boundaries.

