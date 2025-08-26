# Chez: Type-Safe JSON Schema Ecosystem for Scala 3

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/silvabyte/scalaschemaz)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

A powerful ecosystem of libraries for JSON Schema generation, HTTP validation, and LLM integration in Scala 3. Each module can be used independently or together for a complete solution.

## The Ecosystem

- **[Chez](./docs/chez.md)**: Core JSON Schema library with TypeBox-like ergonomics
- **[CaskChez](./docs/caskchez.md)**: Automatic HTTP validation for Cask web framework
- **[ChezWiz](./docs/chezwiz.md)**: Type-safe LLM agents with structured generation

## Example Time -> From Schema to Production

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

### 3️⃣ Add ChezWiz - AI-Powered Product Enrichment

```scala
import chezwiz.agent._
import chezwiz.agent.providers.OpenAIProvider

// Define enriched metadata that AI will generate
case class ProductMetadata(
  @Schema.description("Primary category")
  category: String,
  
  @Schema.description("Sub-category for detailed classification")
  subCategory: String,
  
  @Schema.description("Relevant tags for search and filtering")
  @Schema.minItems(3)
  @Schema.maxItems(10)
  tags: List[String],
  
  @Schema.description("SEO-optimized description")
  @Schema.minLength(50)
  @Schema.maxLength(500)
  seoDescription: String,
  
  @Schema.description("Key features and benefits")
  @Schema.minItems(3)
  @Schema.maxItems(6)
  features: List[String],
  
  @Schema.description("Target audience")
  targetAudience: String,
  
  @Schema.description("Search keywords")
  searchKeywords: List[String]
) derives Schema

// AI enrichment service
object ProductEnricher {
  
  val provider = new OpenAIProvider(sys.env("OPENAI_API_KEY"))
  
  // ✨ Create specialized agent for product enrichment
  val agent = Agent(
    name = "ProductEnricher",
    instructions = """You are a product categorization and SEO expert. 
                     Analyze products and generate rich metadata to improve 
                     searchability and user experience. Focus on accuracy 
                     and relevant categorization.""",
    provider = provider,
    model = "gpt-4o",
    temperature = Some(0.3)  // Lower temperature for consistency
  )

  // ✨ Enrich a basic product with AI-generated metadata
  def enrichProduct(basicProduct: Product): Either[ChezError, ProductMetadata] = {
    val metadata = RequestMetadata(
      tenantId = Some("store-1"),
      userId = Some("system")
    )
    
    // AI analyzes the product and generates structured metadata
    agent.generateObject[ProductMetadata](
      s"""Analyze this product and generate comprehensive metadata:
         |Name: ${basicProduct.name}
         |Price: $$${basicProduct.price}
         |Current tags: ${basicProduct.tags.mkString(", ")}
         |
         |Generate:
         |- Accurate category and subcategory
         |- SEO-optimized description that highlights key benefits
         |- Relevant tags for discovery (include original tags if appropriate)
         |- Key features that customers care about
         |- Target audience identification
         |- Search keywords customers might use
         """.stripMargin,
      metadata
    ).map(_.data)
  }

  // ✨ Batch enrichment with context awareness
  def enrichSimilarProducts(products: List[Product]): List[(Product, ProductMetadata)] = {
    val metadata = RequestMetadata(
      tenantId = Some("store-1"),
      conversationId = Some("batch-enrichment")  // Maintains context across products
    )
    
    // Set context for consistent categorization
    agent.generateText(
      "You'll be enriching multiple related products. Ensure consistent categorization.",
      metadata
    )
    
    products.flatMap { product =>
      enrichProduct(product) match {
        case Right(enriched) => Some((product, enriched))
        case Left(error) => 
          println(s"Failed to enrich ${product.name}: $error")
          None
      }
    }
  }
}
```

### 🎯 Putting It All Together

```scala
// Complete system: User creates product → AI enriches it → Save enhanced product
object ProductSystem extends Main {

  case class EnrichedProduct(
    product: Product,
    metadata: ProductMetadata
  ) derives Schema

  // API endpoint that creates AND enriches products with AI
  @CaskChez.post("/products", RouteSchema(
    body = Some(Schema[Product]),
    responses = Map(
      200 -> ResponseDef("Enriched product", Some(Schema[EnrichedProduct])),
      400 -> ResponseDef("Invalid product"),
      500 -> ResponseDef("Enrichment failed")
    )
  ))
  def createProduct(validatedRequest: ValidatedRequest) = {
    validatedRequest.getBody[Product] match {
      case Right(product) =>
        // Step 1: User's product is already validated by CaskChez
        println(s"✅ Received valid product: ${product.name}")
        
        // Step 2: Use AI to enrich with metadata
        ProductEnricher.enrichProduct(product) match {
          case Right(metadata) =>
            // Step 3: Combine and save enriched product
            val enriched = EnrichedProduct(product, metadata)
            saveToDatabase(enriched)
            
            // Return enriched product to user
            cask.Response(
              upickle.default.write(enriched),
              200,
              headers = Seq("Content-Type" -> "application/json")
            )
            
          case Left(error) =>
            // AI enrichment failed, but we can still save the basic product
            saveToDatabase(product)
            cask.Response(
              s"""{"warning": "Product saved without enrichment: $error"}""",
              200
            )
        }
        
      case Left(errors) =>
        cask.Response(s"""{"errors": ${errors.toJson}}""", 400)
    }
  }

  // Batch enrichment endpoint
  @CaskChez.post("/products/enrich-batch", RouteSchema(
    body = Some(Schema[List[Product]])
  ))
  def enrichBatch(validatedRequest: ValidatedRequest) = {
    validatedRequest.getBody[List[Product]] match {
      case Right(products) =>
        val enriched = ProductEnricher.enrichSimilarProducts(products)
        cask.Response(upickle.default.write(enriched), 200)
      case Left(errors) =>
        cask.Response(s"""{"errors": ${errors.toJson}}""", 400)
    }
  }

  initialize()
}

// Example usage showing the complete flow:
/*
POST /products
{
  "id": "LAP-4521",
  "name": "UltraBook Pro 15",
  "price": 1299.99,
  "stock": 10,
  "tags": ["laptop"]  // User provides minimal tags
}

Response:
{
  "product": {
    "id": "LAP-4521",
    "name": "UltraBook Pro 15",
    "price": 1299.99,
    "stock": 10,
    "tags": ["laptop"]
  },
  "metadata": {
    "category": "Electronics",
    "subCategory": "Computers & Laptops",
    "tags": ["laptop", "ultrabook", "portable", "professional", "high-performance"],
    "seoDescription": "The UltraBook Pro 15 delivers exceptional performance in an ultra-slim design. Perfect for professionals and power users who need reliable computing on the go. Features cutting-edge processors and all-day battery life.",
    "features": [
      "Ultra-slim lightweight design",
      "High-performance processor",
      "All-day battery life",
      "Premium build quality",
      "Professional-grade display"
    ],
    "targetAudience": "Business professionals, content creators, and power users",
    "searchKeywords": ["ultrabook", "laptop 15 inch", "professional laptop", "thin laptop", "portable computer"]
  }
}
*/
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
