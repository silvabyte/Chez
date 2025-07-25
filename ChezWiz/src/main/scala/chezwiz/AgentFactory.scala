package chezwiz.agent

import chezwiz.agent.providers.{OpenAIProvider, AnthropicProvider}

object AgentFactory:

  def createOpenAIAgent(
      name: String,
      instructions: String,
      apiKey: String,
      model: String,
      temperature: Option[Double] = None,
      maxTokens: Option[Int] = None,
      hooks: HookRegistry = HookRegistry.empty
  ): Either[ChezError, Agent] = {
    val provider = new OpenAIProvider(apiKey)
    provider.validateModel(model) match {
      case Right(_) =>
        Right(Agent(name, instructions, provider, model, temperature, maxTokens, hooks))
      case Left(error) =>
        Left(error)
    }
  }

  def createAnthropicAgent(
      name: String,
      instructions: String,
      apiKey: String,
      model: String,
      temperature: Option[Double] = None,
      maxTokens: Option[Int] = None,
      hooks: HookRegistry = HookRegistry.empty
  ): Either[ChezError, Agent] = {
    val provider = new AnthropicProvider(apiKey)
    provider.validateModel(model) match {
      case Right(_) =>
        Right(Agent(name, instructions, provider, model, temperature, maxTokens, hooks))
      case Left(error) =>
        Left(error)
    }
  }
