# Metadata & Scoping

BoogieLoops AI uses `RequestMetadata` to scope conversations and enable stateful interactions across requests.

## RequestMetadata

```scala
case class RequestMetadata(
  tenantId: Option[String] = None,
  userId: Option[String] = None,
  conversationId: Option[String] = None
)
```

- **tenantId**: Multi‑tenant isolation
- **userId**: Per‑user conversation history
- **conversationId**: Specific conversation thread

## Scoped History

Agents automatically maintain conversation history based on metadata:

```scala
val metadata = RequestMetadata(
  userId = Some("alice"),
  conversationId = Some("support-session-123")
)

// First message - starts new conversation
agent.generateText("Hello, I need help with my account.", metadata)

// Follow-up - continues same conversation
agent.generateText("What's my current balance?", metadata)
```

## Stateless Requests

Use `generateTextWithoutHistory` for one‑off requests:

```scala
// No history maintained
val response = agent.generateTextWithoutHistory("Translate: Hello world")
```

## Conversation Isolation

Different metadata creates separate conversation contexts:

```scala
val alice = RequestMetadata(userId = Some("alice"))
val bob = RequestMetadata(userId = Some("bob"))

// These maintain separate histories
agent.generateText("My favorite color is blue", alice)
agent.generateText("My favorite color is red", bob)
```

## Multi‑Tenant Usage

Combine `tenantId` and `userId` for SaaS applications:

```scala
val metadata = RequestMetadata(
  tenantId = Some("company-a"),
  userId = Some("alice"),
  conversationId = Some("onboarding")
)
```

This ensures conversation history is isolated both by tenant and user.
