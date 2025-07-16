package chezwiz.agent.examples

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.*
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global
import chezwiz.agent.*
import chezwiz.agent.providers.{AnthropicProvider, OpenAIProvider}

object BasicExample extends App:
  println("=== Basic Example ===")

  // Get API keys from environment variables
  val openaiKey = sys.env.get("OPENAI_API_KEY")
  val anthropicKey = sys.env.get("ANTHROPIC_API_KEY")

  openaiKey match
    case Some(key) =>
      AgentFactory.createOpenAIAgent("OpenAI Assistant", "You are a helpful assistant", key) match
        case Success(agent) =>
          val response = Await.result(agent.generateText("What is 2+2?"), 10.seconds)
          println(s"OpenAI Response: ${response.content}")
        case Failure(ex) =>
          println(s"Failed to create OpenAI agent: ${ex.getMessage}")
    case None =>
      println("OPENAI_API_KEY not found, skipping openai example")

  anthropicKey match
    case Some(key) =>
      AgentFactory.createAnthropicAgent(
        "Anthropic Assistant",
        "You are a helpful assistant",
        key
      ) match
        case Success(agent) =>
          val response = Await.result(
            agent.generateText("Explain quantum computing in one sentence"),
            10.seconds
          )
          println(s"Anthropic Response: ${response.content}")
        case Failure(ex) =>
          println(s"Failed to create Anthropic agent: ${ex.getMessage}")
    case None =>
      println("ANTHROPIC_API_KEY not found, skipping anthropic example")
