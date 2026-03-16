# Spring AI Tools Utils

A Spring AI community library that adds **guardrails**, **human confirmation**, **automatic retry**, and **fallback strategies** to Spring AI tool callbacks through a clean annotation-driven decorator pattern.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.2-green.svg)](https://spring.io/projects/spring-ai)

## Features

| Feature | Description |
|---|---|
| **Input Guardrails** | Validate and sanitize tool input before execution |
| **Output Guardrails** | Validate and sanitize tool output before it reaches the model |
| **Tool Confirmation** | Require human approval before a tool runs |
| **Automatic Retry** | Retry a failing tool transparently using Spring Retry |
| **Fallback Strategies** | Return a safe response when a tool throws an exception |
| **Built-in Guardrails** | SQL injection, path traversal, sensitive data redaction, size limits, keyword blocking |
| **Auto-configuration** | Zero-config Spring Boot integration |

## Quick Start

### Dependency

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-tools-utils</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Create tool callbacks

Inject `ToolCallbacksFactory` and call `from(toolBean)` instead of `ToolCallbacks.from(toolBean)`:

```java
@Configuration
class AiConfig {

    @Bean
    ChatClient chatClient(ChatModel model, ToolCallbacksFactory factory, MyTools tools) {
        return ChatClient.builder(model)
            .defaultTools(factory.from(tools))
            .build();
    }
}
```

That's it. Annotations on your `@Tool` methods are picked up automatically.

---

## Guardrails

Guardrails run before (input) and after (output) your tool executes. A blocked result throws `GuardrailViolationException`.

### Annotate your tool

```java
@Tool(description = "Runs a database query")
@InputGuardrail(SqlInjectionInputGuardrail.class)
@OutputGuardrail(SensitiveDataOutputGuardrail.class)
public String query(String sql) {
    return db.execute(sql);
}
```

Multiple guardrails are applied in declaration order:

```java
@InputGuardrail(PathTraversalInputGuardrail.class)
@InputGuardrail(MaxInputSizeInputGuardrail.class)
public String readFile(String path) { ... }
```

### Built-in guardrails

Use the `Guardrails` factory class to compose common guardrails programmatically, or reference the built-in classes directly in annotations.

| Class | Type | What it does |
|---|---|---|
| `PathTraversalInputGuardrail` | Input | Blocks inputs containing `..` |
| `SqlInjectionInputGuardrail` | Input | Blocks SQL keywords like `DROP`, `DELETE`, `--` |
| `MaxInputSizeInputGuardrail` | Input | Rejects inputs exceeding 4 096 bytes (configurable) |
| `SensitiveDataOutputGuardrail` | Output | Redacts passwords, tokens, API keys from output |
| `MaxOutputSizeOutputGuardrail` | Output | Rejects outputs exceeding 8 192 bytes (configurable) |

Programmatic usage via `Guardrails`:

```java
ToolInputGuardrail g = Guardrails.blockKeywords(List.of("confidential", "internal"));
ToolOutputGuardrail r = Guardrails.redactPattern("\\d{16}", "****-****-****-****");
ToolInputGuardrail allowed = Guardrails.allowedTools(Set.of("search", "summarize"));
```

### Custom guardrails

Implement `ToolInputGuardrail` or `ToolOutputGuardrail`:

```java
public class ProfanityInputGuardrail implements ToolInputGuardrail {

    @Override
    public InputGuardrailResult evaluate(ToolDefinition def, String toolInput) {
        if (containsProfanity(toolInput)) {
            return InputGuardrailResult.blocked("Input contains disallowed content");
        }
        return InputGuardrailResult.pass(toolInput);       // optionally sanitize
    }
}
```

Register it as a `@Bean` for injection, or rely on the no-arg constructor fallback.

---

## Tool Confirmation

Require explicit human approval before a tool runs. Rejection throws `ToolRejectionException`.

### Annotate your tool

```java
@Tool(description = "Deletes a customer record")
@ConfirmableTool(handler = MyConfirmationHandler.class, reason = "This permanently deletes data.")
public String deleteCustomer(String customerId) { ... }
```

### Implement a handler

For interactive use cases, extend `AbstractConfirmationHandler`:

```java
@Component
public class WebSocketConfirmationHandler extends AbstractConfirmationHandler {

    public WebSocketConfirmationHandler(ConfirmationStore store, ConfirmationProperties props) {
        super(store, props);
    }

    @Override
    protected void onPendingConfirmation(String confirmationId, ConfirmationRequest request) {
        // Push to UI via WebSocket / SSE
        ws.send(new ConfirmationEvent(confirmationId, request.reason(), request.toolInput()));
    }
}
```

When the user responds, call:

```java
handler.respond(confirmationId, approved, reason);
```

`AbstractConfirmationHandler` blocks the calling thread (using `CompletableFuture`) until a response arrives or the configured timeout elapses.

### Auto-approve (testing / dev)

```java
@ConfirmableTool(handler = AutoApproveConfirmationHandler.class)
```

### Configuration

```properties
tools.confirmation.max-pending=1000
tools.confirmation.timeout=60s
```

---

## Automatic Retry

Transparently retry a tool call when it throws an exception, using Spring Retry's `RetryTemplate` under the hood. The AI model never sees the intermediate failures.

### Annotate your tool

```java
@Tool(description = "Fetch data from a flaky external API")
@RetryableTool(maxRetries = 3)
public String fetchData(String query) {
    return externalApi.call(query); // retried up to 3 times on exception
}
```

`maxRetries` is the number of additional attempts after the initial call fails. With `maxRetries = 3` the tool is called at most 4 times (1 initial + 3 retries) before the last exception is rethrown. A fixed 1-second back-off is applied between attempts.

---

## Fallback Strategies

Return a safe response when your tool throws instead of propagating the exception to the model.

### Static message

```java
@Tool(description = "Fetches live weather data")
@FallbackTool(message = "Weather service is currently unavailable.")
public String getWeather(String city) { ... }
```

### Fallback method

The fallback method can accept any combination of the original parameters plus an optional `Throwable`:

```java
@Tool(description = "Fetches stock price")
@FallbackTool(method = "stockPriceFailed")
public String getStockPrice(String ticker) { ... }

// Any of these signatures work:
public String stockPriceFailed() { return "Price unavailable"; }
public String stockPriceFailed(Throwable cause) { return "Error: " + cause.getMessage(); }
public String stockPriceFailed(String ticker) { return ticker + " price unavailable"; }
public String stockPriceFailed(String ticker, Throwable cause) { ... }
```

---

## Decorator Stack

Decorators are applied in this order when you call `ToolCallbacksFactory.from(toolBean)`:

```
ToolCallback (raw Spring AI callback)
    └── GuardedToolCallback         ← @InputGuardrail / @OutputGuardrail
            └── ConfirmableToolCallback     ← @ConfirmableTool
                    └── RetryableToolCallback      ← @RetryableTool
                            └── FallbackToolCallback      ← @FallbackTool
```

The outermost layer (fallback) catches exceptions from any inner layer, including retries that have been exhausted.

---

## Architecture

The library is structured as a multi-module Maven project:

```
spring-ai-tools-utils/          # Published library artifact
  callback/                     # ToolCallbacksFactory, AbstractToolCallbacks
  guardrails/
    annotation/                 # @InputGuardrail, @OutputGuardrail
    builtin/                    # Ready-made guardrail implementations + Guardrails factory
    callback/                   # GuardedToolCallback, GuardedToolCallbacks
    input/, output/, exception/
  confirmation/
    annotation/                 # @ConfirmableTool
    callback/                   # ConfirmableToolCallback, ConfirmableToolCallbacks
    store/                      # ConfirmationStore, InMemoryConfirmationStore
    properties/, exception/
  fallback/
    annotation/                 # @FallbackTool
    callback/                   # FallbackToolCallback, FallbackToolCallbacks
    strategy/                   # FallbackToolStrategy
  retry/
    annotation/                 # @RetryableTool
    callback/                   # RetryableToolCallback, RetryableToolCallbacks
  configuration/                # ToolsAutoConfiguration (Spring Boot auto-config)
examples/
  tool-guardrails/              # Guardrail usage demo
  tool-confirmation/            # Console confirmation demo
  tool-fallbacks/               # Fallback strategy demo
  tool-sse-confirmation/        # SSE + REST confirmation demo (browser UI)
  tool-retry/                   # Retry demo with a simulated flaky tool
```

## Requirements

- Java 21
- Spring AI 1.1.2
- Spring Boot 3.5.x
- Spring Retry 2.0.x

## License

Apache License 2.0 — see [LICENSE](https://www.apache.org/licenses/LICENSE-2.0).
