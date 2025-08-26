# Chez: Type-Safe JSON Schema Ecosystem for Scala 3

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/silvabyte/scalaschemaz)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A powerful ecosystem of libraries for JSON Schema generation, HTTP validation, and LLM integration in Scala 3. Each module can be used independently or together for a complete solution.

## The Ecosystem

- **[Chez](./docs/chez.md)**: Core JSON Schema library with TypeBox-like ergonomics
- **[CaskChez](./docs/caskchez.md)**: Automatic HTTP validation for Cask web framework  
- **[ChezWiz](./docs/chezwiz.md)**: Type-safe LLM agents with structured generation

## Progressive Example: From Schema to Production

### 1️⃣ Start with Chez - Define Your Data Model

```scala
import chez.derivation.Schema
import chez.Chez
import ujson._

// Define your domain model with validation rules
case class Product(
  @Schema.description("Product identifier")
  @Schema.pattern("^[A-Z]{3}-[0-9]{4}$")
  id: String,

  @Schema.description("Product name")
  @Schema.minLength(1)
  @Schema.maxLength(100)
  name: String,

  @Schema.description("Price in USD")
  @Schema.minimum(0.01)
  @Schema.maximum(999999.99)
  price: Double,

  @Schema.description("Available stock")
  @Schema.minimum(0)
  @Schema.default(0)
  stock: Int = 0,

  @Schema.description("Product tags for categorization")
  @Schema.minItems(1)
  @Schema.uniqueItems(true)
  tags: List[String]
) derives Schema

// ✨ Automatic schema generation
val productSchema = Schema[Product]
println(productSchema.toJsonSchema.spaces2)
// Output: Full JSON Schema with all validation rules!

// ✨ Runtime validation
val validProduct = Obj(
  "id" -> "ABC-1234",
  "name" -> "Laptop",
  "price" -> 999.99,
  "stock" -> 5,
  "tags" -> Arr("electronics", "computers")
)

productSchema.validate(validProduct) match {
  case Right(()) => println("✅ Valid product!")
  case Left(errors) => println(s"❌ Validation failed: $errors")
}
```

### 2️⃣ Add CaskChez - Create a Validated REST API

```scala
import caskchez._
import cask.main.Main

// Build a REST API with automatic validation
object ProductAPI extends Main {
  
  // ✨ Automatic request validation using the Product schema
  @CaskChez.post("/products", RouteSchema(
    body = Some(Schema[Product]),
    responses = Map(
      200 -> ResponseDef("Product created", Some(Schema[Product])),
      400 -> ResponseDef("Invalid product data")
    )
  ))
  def createProduct(validatedRequest: ValidatedRequest) = {
    validatedRequest.getBody[Product] match {
      case Right(product) =>
        // Product is guaranteed to be valid here!
        val saved = saveToDatabase(product)
        cask.Response(upickle.default.write(saved), 200)
      
      case Left(errors) =>
        cask.Response(s"""{"errors": ${errors.toJson}}""", 400)
    }
  }

  // ✨ Query parameter validation
  @CaskChez.get("/products", RouteSchema(
    queryParams = Map(
      "minPrice" -> ParamDef(Schema[Double], required = false),
      "tags" -> ParamDef(Schema[List[String]], required = false)
    )
  ))
  def listProducts(validatedRequest: ValidatedRequest) = {
    val minPrice = validatedRequest.getQueryParam[Double]("minPrice").toOption
    val tags = validatedRequest.getQueryParam[List[String]]("tags").toOption
    
    val products = findProducts(minPrice, tags)
    upickle.default.write(products)
  }

  // ✨ Automatic OpenAPI documentation
  @cask.get("/openapi.json")
  def openapi() = {
    OpenAPIGenerator.generate(this).toJson
  }

  initialize()
}
```

### 3️⃣ Add ChezWiz - Intelligent Product Generation with LLMs

```scala
import chezwiz.agent._
import chezwiz.agent.providers.OpenAIProvider

// Create an AI agent that understands your Product schema
object ProductAI {
  
  // ✨ Create agent with built-in metrics
  val (agent, metrics) = MetricsFactory.createOpenAIAgentWithMetrics(
    name = "ProductGenerator",
    instructions = """You are a product catalog expert. Generate realistic 
                     product data following all validation rules.""",
    apiKey = sys.env("OPENAI_API_KEY"),
    model = "gpt-4o"
  ) match {
    case Right(result) => result
    case Left(error) => throw new RuntimeException(s"Failed: $error")
  }

  // ✨ Generate valid products using AI
  def generateProduct(description: String): Either[ChezError, Product] = {
    val metadata = RequestMetadata(
      tenantId = Some("store-1"),
      userId = Some("admin"),
      conversationId = Some("product-gen-session")
    )
    
    // The agent automatically ensures the output matches the Product schema!
    agent.generateObject[Product](
      s"""Create a product based on this description: $description
          Ensure the ID follows the pattern XXX-9999 format.
          Choose appropriate tags from: electronics, clothing, books, home, sports.""",
      metadata
    ).map(_.data)
  }

  // ✨ Bulk generation with conversation context
  def generateCatalog(category: String, count: Int): List[Product] = {
    val metadata = RequestMetadata(
      tenantId = Some("store-1"),
      conversationId = Some(s"catalog-$category")
    )
    
    // First message sets context
    agent.generateText(
      s"We're building a $category catalog. Generate diverse, realistic products.",
      metadata
    )
    
    // Generate products with maintained context
    (1 to count).flatMap { i =>
      generateProduct(s"Product $i for $category category").toOption
    }.toList
  }

  // ✨ Monitor AI performance
  def printMetrics(): Unit = {
    metrics.getSnapshot("ProductGenerator").foreach { snapshot =>
      println(s"""
        |📊 AI Product Generator Metrics:
        |  Total requests: ${snapshot.totalRequests}
        |  Success rate: ${(snapshot.successRate * 100).round}%
        |  Avg response time: ${snapshot.averageDuration}ms
        |  Products generated: ${snapshot.objectGenerations.count}
        |  Tokens used: ${snapshot.totalTokens}
      """.stripMargin)
    }
  }
}
```

### 🎯 Putting It All Together

```scala
// Complete system: Schema validation + REST API + AI generation
object ProductSystem extends Main {
  
  // Reuse the same Product schema everywhere
  val productSchema = Schema[Product]
  
  // API endpoint that uses AI to generate products
  @CaskChez.post("/products/generate", RouteSchema(
    body = Some(Schema[ProductRequest]),
    responses = Map(200 -> ResponseDef("Generated product", Some(productSchema)))
  ))
  def generateProduct(validatedRequest: ValidatedRequest) = {
    validatedRequest.getBody[ProductRequest] match {
      case Right(request) =>
        // AI generates a product that's guaranteed to be schema-valid
        ProductAI.generateProduct(request.description) match {
          case Right(product) =>
            // Save to database and return
            val saved = saveToDatabase(product)
            cask.Response(upickle.default.write(saved), 200)
          
          case Left(error) =>
            cask.Response(s"""{"error": "$error"}""", 500)
        }
      
      case Left(errors) =>
        cask.Response(s"""{"errors": ${errors.toJson}}""", 400)
    }
  }

  // Metrics endpoint for monitoring
  @cask.get("/metrics")
  def metrics() = {
    ProductAI.metrics.exportPrometheus  // Prometheus-ready metrics
  }
  
  initialize()
}

case class ProductRequest(description: String) derives Schema
```

## Installation

Each module can be used independently:

```scala
// build.mill - Choose what you need

// Just schema validation
ivy"com.silvabyte::chez:0.2.0"

// Add HTTP validation
ivy"com.silvabyte::chez:0.2.0"
ivy"com.silvabyte::caskchez:0.2.0"

// Full ecosystem with LLM support
ivy"com.silvabyte::chez:0.2.0"
ivy"com.silvabyte::caskchez:0.2.0"
ivy"com.silvabyte::chezwiz:0.2.0"
```

## Key Features by Module

### 🎯 Chez Core
- ✅ Full JSON Schema 2020-12 compliance
- ✅ Annotation-based schema derivation  
- ✅ TypeBox-like ergonomics with type safety
- ✅ Compile-time and runtime validation
- ✅ Scala 3 union types and match types

### 🌐 CaskChez  
- ✅ Zero-boilerplate HTTP validation
- ✅ Automatic OpenAPI generation
- ✅ Request/response schema enforcement
- ✅ Query, path, header validation
- ✅ Content negotiation

### 🤖 ChezWiz
- ✅ Multi-provider LLM support (OpenAI, Anthropic, local)
- ✅ Structured generation with schema validation
- ✅ Scoped conversation management 
- ✅ Built-in metrics and monitoring
- ✅ Comprehensive hook system
- ✅ Vector embeddings support

## Documentation

- **[Chez Reference](./docs/chez.md)** - Complete schema API
- **[CaskChez Guide](./docs/caskchez.md)** - HTTP validation patterns  
- **[ChezWiz Manual](./docs/chezwiz.md)** - LLM agent development
- **[Custom Providers](./docs/custom-providers.md)** - Local LLM integration

## Quick Start

```bash
# Clone and explore examples
git clone https://github.com/silvabyte/Chez.git
cd Chez

# Run all tests
./mill __.test

# Run specific module tests
./mill Chez.test
./mill CaskChez.test  
./mill ChezWiz.test

# Run examples
./mill Chez.run
./mill CaskChez.run
./mill ChezWiz.run
```

## License

MIT License. See [LICENSE](LICENSE) for details.
