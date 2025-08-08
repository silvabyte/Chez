package chezwiz.agent.examples

import chezwiz.agent.*
import chezwiz.agent.EmbeddingInput
import chezwiz.agent.providers.{LMStudioProvider, HttpVersion}
import scribe.Logging

/**
 * Example demonstrating vector embeddings with LM Studio
 * using the text-embedding-qwen3-embedding-8b model
 *
 * Prerequisites:
 * 1. Install LM Studio from https://lmstudio.ai
 * 2. Download and load the text-embedding-qwen3-embedding-8b model
 * 3. Start the LM Studio server (default: http://localhost:1234)
 */
object LMStudioEmbeddingExample extends App with Logging:

  // Initialize config from environment directory
  Config.initialize(sys.env.getOrElse("ENV_DIR", os.pwd.toString))

  logger.info(
    s"Initialized config from directory: ${sys.env.getOrElse("ENV_DIR", os.pwd.toString)}"
  )

  // Configure LM Studio provider with embedding model
  val baseUrl = Config.get("LM_STUDIO_URL", "http://localhost:1234/v1")
  val embeddingModel = Config.get("LM_STUDIO_EMBEDDING_MODEL", "text-embedding-qwen3-embedding-8b")

  logger.info(s"Connecting to LM Studio at: $baseUrl")
  logger.info(s"Using embedding model: $embeddingModel")

  // Create LM Studio provider with HTTP/1.1 for better local compatibility
  val provider = new LMStudioProvider(
    baseUrl = baseUrl,
    modelId = embeddingModel,
    httpVersionParam = HttpVersion.Http11
  )

  // Create agent with embedding support
  val agent = Agent(
    name = "EmbeddingAgent",
    instructions = "Generate high-quality text embeddings for semantic search and similarity",
    provider = provider,
    model = embeddingModel
  )

  logger.info("=" * 60)
  logger.info("LM Studio Embedding Example with Qwen3")
  logger.info("=" * 60)

  // Example 1: Single text embedding
  logger.info("\n1. Single Text Embedding")
  logger.info("-" * 40)

  val singleText =
    "ChezWiz is a type-safe LLM agent library for Scala 3 that provides comprehensive support for multiple providers."

  agent.generateEmbedding(singleText) match {
    case Right(response) =>
      logger.info(s"✓ Generated embedding with ${response.dimensions} dimensions")
      logger.info(s"  Model: ${response.model}")
      response.usage.foreach { usage =>
        logger.info(s"  Tokens used: ${usage.totalTokens}")
      }
      // Show first 10 values of the embedding vector
      val preview = response.embeddings.head.values.take(10)
      logger.info(s"  Embedding preview (first 10 values): [${preview.mkString(", ")}...]")

    case Left(error) =>
      logger.error(s"✗ Failed to generate embedding: $error")
  }

  // Example 2: Batch embeddings
  logger.info("\n2. Batch Embeddings")
  logger.info("-" * 40)

  val texts = List(
    "Scala is a powerful functional programming language",
    "Type safety helps prevent runtime errors in production",
    "LLM agents can generate structured data with JSON schemas",
    "Vector embeddings enable semantic search capabilities",
    "ChezWiz supports OpenAI, Anthropic, and LM Studio providers"
  )

  logger.info(s"Generating embeddings for ${texts.size} texts...")

  agent.generateEmbeddings(texts) match {
    case Right(response) =>
      logger.info(s"✓ Generated ${response.embeddings.size} embeddings")
      logger.info(s"  Dimensions per embedding: ${response.dimensions}")
      response.usage.foreach { usage =>
        logger.info(s"  Total tokens used: ${usage.totalTokens}")
      }

      // Example 3: Semantic similarity search
      logger.info("\n3. Semantic Similarity Search")
      logger.info("-" * 40)

      val queryText = "functional programming with strong types"
      logger.info(s"Query: \"$queryText\"")

      // Generate embedding for query
      agent.generateEmbedding(queryText) match {
        case Right(queryResponse) =>
          val queryEmbedding = queryResponse.embeddings.head.values

          // Calculate similarities with all texts
          val similarities = response.embeddings.zip(texts).map { case (embedding, text) =>
            val similarity = agent.cosineSimilarity(queryEmbedding, embedding.values)
            (text, similarity)
          }

          // Sort by similarity and display results
          logger.info("\nSimilarity scores (higher = more similar):")
          similarities.sortBy(-_._2).foreach { case (text, score) =>
            val barLength = (score * 20).toInt
            val bar = "█" * barLength + "░" * (20 - barLength)
            logger.info(f"  $bar $score%.3f - ${text.take(50)}...")
          }

          // Find most similar text
          val (mostSimilar, highestScore) = similarities.maxBy(_._2)
          logger.info(
            s"\n✓ Most similar to query: \"${mostSimilar.take(50)}...\" (score: ${f"$highestScore%.3f"})"
          )

        case Left(error) =>
          logger.error(s"✗ Failed to generate query embedding: $error")
      }

    case Left(error) =>
      logger.error(s"✗ Failed to generate batch embeddings: $error")
  }

  // Example 4: Document clustering simulation
  logger.info("\n4. Document Clustering (Similarity Matrix)")
  logger.info("-" * 40)

  val documents = List(
    "Machine learning models require training data",
    "Neural networks learn patterns from examples",
    "Scala combines object-oriented and functional paradigms",
    "Type inference reduces boilerplate code in Scala"
  )

  logger.info("Generating embeddings for clustering...")

  agent.generateEmbeddings(documents) match {
    case Right(response) =>
      logger.info(s"✓ Generated embeddings for ${documents.size} documents")

      // Create similarity matrix
      logger.info("\nSimilarity Matrix:")
      logger.info("     " + (1 to documents.size).map(i => f"Doc$i%5s").mkString(" "))

      response.embeddings.zipWithIndex.foreach { case (embedding1, i) =>
        val similarities = response.embeddings.map { embedding2 =>
          agent.cosineSimilarity(embedding1.values, embedding2.values)
        }
        val row = similarities.map(s => f"$s%5.2f").mkString(" ")
        logger.info(f"Doc${i + 1}%4s $row")
      }

      // Find document pairs with high similarity
      logger.info("\nDocument Pairs with High Similarity (> 0.7):")
      for {
        i <- documents.indices
        j <- (i + 1) until documents.size
        similarity = agent.cosineSimilarity(
          response.embeddings(i).values,
          response.embeddings(j).values
        )
        if similarity > 0.7
      } {
        logger.info(f"  Doc${i + 1} ↔ Doc${j + 1}: $similarity%.3f")
        logger.info(s"    - \"${documents(i).take(40)}...\"")
        logger.info(s"    - \"${documents(j).take(40)}...\"")
      }

    case Left(error) =>
      logger.error(s"✗ Failed to generate document embeddings: $error")
  }

  // Example 5: Embedding with metadata (for tracking)
  logger.info("\n5. Embeddings with Request Metadata")
  logger.info("-" * 40)

  val metadata = RequestMetadata(
    conversationId = Some("embed-session-001"),
    userId = Some("demo-user"),
    tenantId = Some("lmstudio-examples")
  )

  val textWithMetadata = "Track this embedding request with metadata for observability"

  agent.generateEmbedding(textWithMetadata, metadata = metadata) match {
    case Right(response) =>
      logger.info(s"✓ Generated tracked embedding")
      logger.info(s"  Conversation: ${metadata.conversationId.getOrElse("N/A")}")
      logger.info(s"  User: ${metadata.userId.getOrElse("N/A")}")
      logger.info(s"  Tenant: ${metadata.tenantId.getOrElse("N/A")}")
      logger.info(s"  Dimensions: ${response.dimensions}")

    case Left(error) =>
      logger.error(s"✗ Failed to generate tracked embedding: $error")
  }

  logger.info("\n" + "=" * 60)
  logger.info("Example completed successfully!")
  logger.info("=" * 60)

  // Helper function to display embeddings
  def displayEmbedding(embedding: Vector[Float], maxValues: Int = 5): String = {
    val preview = embedding.take(maxValues).map(f => f"$f%.4f")
    s"[${preview.mkString(", ")}... (${embedding.size} total)]"
  }
