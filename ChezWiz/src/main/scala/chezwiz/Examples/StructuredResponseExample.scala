package chezwiz.agent.examples

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.*
import scala.util.{Try, Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global
import chezwiz.agent.*
import chezwiz.agent.providers.OpenAIProvider
import ujson.{Obj, Arr}

object StructuredResponseExample extends App:
  println("=== Structured Response Example ===")

  // Define a schema for a person object
  val personSchema = JsonSchema(
    schema = Obj(
      "type" -> "object",
      "properties" -> Obj(
        "name" -> Obj("type" -> "string"),
        "age" -> Obj("type" -> "number"),
        "profession" -> Obj("type" -> "string"),
        "skills" -> Obj(
          "type" -> "array",
          "items" -> Obj("type" -> "string")
        ),
        "experience_years" -> Obj("type" -> "number")
      ),
      "required" -> Arr("name", "age", "profession")
    ),
    description = Some("A person with professional details")
  )

  sys.env.get("OPENAI_API_KEY") match
    case Some(key) =>
      val agent = Agent(
        name = "Data Extractor",
        instructions = "Extract structured data from user input",
        provider = new OpenAIProvider(key),
        model = "gpt-4o"
      )

      Try {
        Await.result(
          agent.generateObject("Create a profile for a software engineer named Alex who has 5 years of experience", personSchema),
          15.seconds
        )
      } match {
        case Success(response) =>
          println(s"Generated object: ${response.`object`}")
          response.usage.foreach { usage =>
            println(s"Tokens used: ${usage.totalTokens}")
          }
        case Failure(ex: LLMError) =>
          println(s"LLM Error: ${ex.message}")
        case Failure(ex) =>
          println(s"Error: ${ex.getMessage}")
      }

    case None =>
      println("OPENAI_API_KEY not found for structured response example")
