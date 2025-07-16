package chezwiz.agent.examples

import scala.concurrent.{Await}
import scala.concurrent.duration.*
import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global
import chezwiz.agent.{Agent}
import chezwiz.agent.providers.AnthropicProvider

object ConversationExample extends App:
  println("=== Conversation Example ===")

  sys.env.get("ANTHROPIC_API_KEY") match
    case Some(key) =>
      val agent = Agent(
        name = "Conversational AI",
        instructions = "You are a friendly conversational AI that remembers context",
        provider = new AnthropicProvider(key),
        model = "claude-3-5-haiku-20241022"
      )

      // First message
      val response1 = Await.result(
        agent.generateText("Hi, my name is Alice and I love programming"),
        10.seconds
      )
      println(s"Agent: ${response1.content}")

      // Second message - agent should remember the name
      val response2 = Await.result(
        agent.generateText("What programming language would you recommend for me?"),
        10.seconds
      )
      println(s"Agent: ${response2.content}")

      // Show conversation history
      println(s"\nConversation history has ${agent.getConversationHistory.size} messages")

    case None =>
      println("ANTHROPIC_API_KEY not found for conversation example")
