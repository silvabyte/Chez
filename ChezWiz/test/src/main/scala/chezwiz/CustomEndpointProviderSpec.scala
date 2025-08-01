package chezwiz.agent

import chezwiz.agent.providers.{CustomEndpointProvider, HttpVersion}
import upickle.default.*
import utest._

object CustomEndpointProviderSpec extends TestSuite:
  val tests = Tests {
    test("CustomEndpointProvider - create with basic settings") {
      val provider = CustomEndpointProvider(
        baseUrl = "https://api.example.com/v1",
        apiKey = "test-key"
      )

      assert(provider.name.contains("CustomEndpoint"))
      assert(provider.supportedModels.isEmpty) // Default allows any model
      assert(provider.validateModel("any-model") == Right(()))
      assert(provider.httpVersion == HttpVersion.Http2) // Default
    }

    test("CustomEndpointProvider - create with custom settings") {
      val customUrl = "http://localhost:8080/v1"
      val provider = CustomEndpointProvider(
        baseUrl = customUrl,
        apiKey = "custom-key",
        supportedModels = List("model1", "model2"),
        httpVersion = HttpVersion.Http11
      )

      assert(provider.name.contains(customUrl))
      assert(provider.supportedModels.contains("model1"))
      assert(provider.supportedModels.contains("model2"))
      assert(provider.httpVersion == HttpVersion.Http11)
    }

    test("CustomEndpointProvider - create with supported models") {
      val provider = CustomEndpointProvider(
        baseUrl = "https://api.example.com/v1",
        apiKey = "test-key",
        supportedModels = List("model1", "model2")
      )

      assert(provider.supportedModels.contains("model1"))
      assert(provider.supportedModels.contains("model2"))
      assert(provider.validateModel("model1") == Right(()))
      assert(provider.validateModel("model3").isLeft)
    }

    test("CustomEndpointProvider - create provider with custom headers") {
      val provider = new CustomEndpointProvider(
        apiKey = "test-api-key",
        baseUrl = "https://api.test.com/v1",
        customHeaders = Map("X-Custom" -> "value")
      )

      assert(provider.customHeaders.contains("X-Custom"))
      assert(provider.customHeaders("X-Custom") == "value")
    }

    test("CustomEndpointProvider - empty API key") {
      val provider = new CustomEndpointProvider(
        apiKey = "",
        baseUrl = "http://localhost:1234/v1"
      )

      // Empty API key should be allowed
      assert(provider.name.contains("CustomEndpoint"))
    }


    test("CustomEndpointProvider - validate models correctly when supportedModels is empty") {
      val provider = new CustomEndpointProvider(
        apiKey = "test-key",
        baseUrl = "http://localhost:1234/v1",
        supportedModels = List.empty
      )

      // When supportedModels is empty, any model should be valid
      assert(provider.validateModel("any-model") == Right(()))
      assert(provider.validateModel("another-model") == Right(()))
    }

    test("CustomEndpointProvider - validate models correctly when supportedModels is specified") {
      val provider = new CustomEndpointProvider(
        apiKey = "test-key",
        baseUrl = "http://localhost:1234/v1",
        supportedModels = List("model1", "model2")
      )

      assert(provider.validateModel("model1") == Right(()))
      assert(provider.validateModel("model2") == Right(()))
      provider.validateModel("model3") match {
        case Left(ChezError.ModelNotSupported("model3", _, _)) => // Expected
        case _ => assert(false)
      }
    }


    test("CustomEndpointProvider - allow configuring HTTP version") {
      val http2Provider = CustomEndpointProvider(
        baseUrl = "https://api.example.com/v1",
        apiKey = "key",
        httpVersion = HttpVersion.Http2
      )
      assert(http2Provider.httpVersion == HttpVersion.Http2)

      val http11Provider = CustomEndpointProvider(
        baseUrl = "http://localhost:8080/v1",
        apiKey = "key",
        httpVersion = HttpVersion.Http11
      )
      assert(http11Provider.httpVersion == HttpVersion.Http11)
    }

  }
