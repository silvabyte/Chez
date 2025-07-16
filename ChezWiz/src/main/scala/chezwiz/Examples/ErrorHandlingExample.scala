package chezwiz.agent.examples

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.*
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global
import chezwiz.agent.*
import chezwiz.agent.providers.OpenAIProvider

object ErrorHandlingExample extends App:
  println("=== Error Handling Example ===")

  // Try with invalid API key
  val invalidAgent = Agent(
    name = "Invalid Agent",
    instructions = "Test agent",
    provider = new OpenAIProvider("invalid-key"),
    model = "gpt-4o-mini"
  )

  import scala.util.{Try, Success, Failure}

  Try {
    Await.result(invalidAgent.generateText("Hello"), 10.seconds)
  } match {
    case Success(response) =>
      println(s"Unexpected success: ${response.content}")
    case Failure(ex: LLMError) =>
      println(s"Expected error: ${ex.message}")
    case Failure(ex) =>
      println(s"Unexpected error: ${ex.getMessage}")
  }
