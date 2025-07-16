package chezwiz.agent.examples

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.*
import scala.util.{Try, Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global
import chezwiz.agent.*
import chezwiz.agent.providers.AnthropicProvider
import ujson.{Obj, Arr}

object EntityExtractionExample extends App:
  println("=== Entity Extraction Example ===")

  val extractionSchema = JsonSchema(
    schema = Obj(
      "type" -> "object",
      "properties" -> Obj(
        "entities" -> Obj(
          "type" -> "array",
          "items" -> Obj(
            "type" -> "object",
            "properties" -> Obj(
              "name" -> Obj("type" -> "string"),
              "type" -> Obj("type" -> "string", "enum" -> Arr("PERSON", "ORGANIZATION", "LOCATION", "DATE")),
              "confidence" -> Obj("type" -> "number", "minimum" -> 0, "maximum" -> 1)
            ),
            "required" -> Arr("name", "type", "confidence")
          )
        ),
        "summary" -> Obj("type" -> "string")
      ),
      "required" -> Arr("entities", "summary")
    ),
    description = Some("Extract entities from text and provide analysis")
  )

  sys.env.get("ANTHROPIC_API_KEY") match
    case Some(key) =>
      val agent = Agent(
        name = "Entity Extractor",
        instructions = "Extract entities from text and provide detailed analysis",
        provider = new AnthropicProvider(key),
        model = "claude-3-5-sonnet-20241022"
      )

      val text =
        "Apple Inc. was founded by Steve Jobs in 1976. The company is headquartered in Cupertino, California and Tim Cook is the current CEO."

      Try {
        Await.result(
          agent.generateObject(s"Extract entities from this text: $text", extractionSchema),
          15.seconds
        )
      } match {
        case Success(response) =>
          println(s"Extracted entities: ${response.`object`}")
        case Failure(ex: LLMError) =>
          println(s"LLM Error: ${ex.message}")
        case Failure(ex) =>
          println(s"Error: ${ex.getMessage}")
      }

    case None =>
      println("ANTHROPIC_API_KEY not found for entity extraction example")
