package chezwiz.agent.examples

import chezwiz.agent.*
import chezwiz.agent.providers.{OpenAIProvider, AnthropicProvider}
import chez.derivation.Schema
import upickle.default.*

/**
 * Consolidated examples demonstrating ChezWiz core functionality
 */
object Examples extends App:
  
  // Example 1: Simple text generation
  def textGeneration(): Unit =
    println("=== Text Generation ===")
    
    sys.env.get("OPENAI_API_KEY") match
      case Some(key) =>
        val agent = Agent(
          name = "Assistant",
          instructions = "You are a helpful assistant that gives concise answers",
          provider = new OpenAIProvider(key),
          model = "gpt-4o-mini"
        )
        
        val response = agent.generateText("What is 2 + 2?")
        println(s"Q: What is 2 + 2?")
        println(s"A: ${response.content}")
        
      case None =>
        println("OPENAI_API_KEY not found")

  // Example 2: Structured data extraction
  def structuredData(): Unit =
    println("\n=== Structured Data Extraction ===")
    
    case class Person(
      @Schema.description("Person's full name")
      name: String,
      @Schema.description("Person's age") 
      @Schema.minimum(0)
      @Schema.maximum(120)
      age: Int,
      @Schema.description("Person's job title")
      profession: String,
      @Schema.description("List of skills")
      skills: List[String]
    ) derives Schema, ReadWriter
    
    sys.env.get("OPENAI_API_KEY") match
      case Some(key) =>
        val agent = Agent(
          name = "Data Extractor",
          instructions = "Extract structured information from text",
          provider = new OpenAIProvider(key),
          model = "gpt-4o"
        )
        
        val response = agent.generateObject[Person](
          "Create a profile for Sarah, a 28-year-old software engineer who knows Python, React, and Docker"
        )
        
        println(s"Name: ${response.`object`.name}")
        println(s"Age: ${response.`object`.age}")
        println(s"Profession: ${response.`object`.profession}")
        println(s"Skills: ${response.`object`.skills.mkString(", ")}")
        
      case None =>
        println("OPENAI_API_KEY not found")

  // Example 3: Conversation with memory
  def conversation(): Unit =
    println("\n=== Conversation with Memory ===")
    
    sys.env.get("OPENAI_API_KEY") match
      case Some(key) =>
        val agent = Agent(
          name = "Chat Bot",
          instructions = "You are a friendly assistant that remembers context",
          provider = new OpenAIProvider(key),
          model = "gpt-4o-mini"
        )
        
        // First exchange
        val response1 = agent.generateText("Hi, I'm Alex and I'm learning Scala")
        println(s"Alex: Hi, I'm Alex and I'm learning Scala")
        println(s"Bot: ${response1.content}")
        
        // Second exchange - should remember Alex's name and context
        val response2 = agent.generateText("What's a good resource for learning functional programming?")
        println(s"Alex: What's a good resource for learning functional programming?")
        println(s"Bot: ${response2.content}")
        
      case None =>
        println("OPENAI_API_KEY not found")

  // Example 4: Using with different providers
  def multiProvider(): Unit =
    println("\n=== Multiple Providers ===")
    
    val question = "Explain recursion in one sentence"
    
    // OpenAI
    sys.env.get("OPENAI_API_KEY") match
      case Some(key) =>
        val openai = Agent(
          name = "OpenAI",
          instructions = "Give brief, technical explanations",
          provider = new OpenAIProvider(key),
          model = "gpt-4o-mini"
        )
        val response = openai.generateText(question)
        println(s"OpenAI: ${response.content}")
      case None =>
        println("OPENAI_API_KEY not found")
    
    // Anthropic
    sys.env.get("ANTHROPIC_API_KEY") match
      case Some(key) =>
        val anthropic = Agent(
          name = "Anthropic",
          instructions = "Give brief, technical explanations",
          provider = new AnthropicProvider(key),
          model = "claude-3-5-haiku-20241022"
        )
        val response = anthropic.generateText(question)
        println(s"Anthropic: ${response.content}")
      case None =>
        println("ANTHROPIC_API_KEY not found")

  // Run all examples
  textGeneration()
  structuredData()
  conversation()
  multiProvider()
  
  println("\n=== All Examples Complete ===")