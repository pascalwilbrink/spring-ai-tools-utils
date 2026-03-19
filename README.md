# Spring AI Tools Utils

A Spring AI community library that adds **guardrails**, **human confirmation**, **rate limiting**, **automatic retry**, **fallback strategies**, **structured logging**, and **Micrometer metrics** to Spring AI tool callbacks through a clean annotation-driven decorator pattern.

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.2-green.svg)](https://spring.io/projects/spring-ai)

## Features

| Feature | Description |
|---|---|
| **Input Guardrails** | Validate and sanitize tool input before execution |
| **Output Guardrails** | Validate and sanitize tool output before it reaches the model |
| **Tool Confirmation** | Require human approval before a tool runs |
| **Rate Limiting** | Cap tool invocations per second, minute, or hour |
| **Automatic Retry** | Retry a failing tool transparently using Spring Retry |
| **Fallback Strategies** | Return a safe response when a tool throws an exception |
| **Structured Logging** | Automatic DEBUG/WARN/ERROR logging with duration for every tool call |
| **Micrometer Metrics** | Call count and duration metrics tagged by tool name and outcome |
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

## Rate Limiting

Cap how often a tool can be called within a time window. Calls that exceed the limit throw `RateLimitExceededException` immediately.

### Annotate your tool

```java
@Tool(description = "Get the current exchange rate")
@RateLimitedTool(requests = 60, per = RateLimitedTool.RateLimit.MINUTE)
public String getExchangeRate(String currency) {
    return ratesApi.fetch(currency);
}
```

| Attribute | Default | Description |
|---|---|---|
| `requests` | `10` | Maximum number of calls allowed within the window |
| `per` | `MINUTE` | Time window: `SECOND`, `MINUTE`, or `HOUR` |

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

`maxRetries` is the number of additional attempts after the initial call fails. With `maxRetries = 3` the tool is called at most 4 times (1 initial + 3 retries) before the last exception is rethrown.

### All attributes

| Attribute | Default | Description |
|---|---|---|
| `maxRetries` | `3` | Number of retry attempts after the initial failure (must be ≥ 1) |
| `delay` | `1000` | Delay between attempts in milliseconds |
| `multiplier` | `1.0` | Backoff multiplier; set > 1.0 for exponential backoff (e.g. `2.0`) |
| `retryOn` | `RuntimeException.class` | Exception types that trigger a retry |
| `noRetryOn` | _(empty)_ | Exception types that are never retried, even if they match `retryOn` |

### Exponential backoff

```java
@RetryableTool(maxRetries = 5, delay = 500, multiplier = 2.0)
public String callExternalApi(String input) { ... }
// delays: 500ms, 1000ms, 2000ms, 4000ms, 8000ms
```

### Selective retry

```java
@RetryableTool(maxRetries = 3, retryOn = { IOException.class },
               noRetryOn = { IllegalArgumentException.class })
public String callService(String input) { ... }
```

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

## Logging

Every tool call is automatically logged at `DEBUG` level with the tool name and elapsed time. Specific failure types (guardrail violations, rejections, rate limit, unexpected errors) are logged at `WARN` or `ERROR`. No annotation is required — logging is applied to all tools automatically.

```
DEBUG Tool call started  | tool=getWeather input={"city":"Amsterdam"}
DEBUG Tool call success  | tool=getWeather duration=42ms output={"temp":18}
WARN  Tool call blocked  | tool=query duration=1ms reason=SQL Injection detected...
ERROR Tool call failed   | tool=fetchData duration=3210ms error=Connection timed out
```

The logger name is `org.springaicommunity.tool.logging.callback.LoggingToolCallback`.

---

## Metrics

When Micrometer is on the classpath (e.g. via `spring-boot-starter-actuator`), each tool call emits two metrics:

| Metric | Type | Tags |
|---|---|---|
| `tools.call.duration` | Timer | `tool`, `outcome` |
| `tools.call.count` | Counter | `tool`, `outcome` |

Possible `outcome` values: `success`, `blocked`, `rejected`, `rate_limited`, `failure`.

Metrics are applied automatically to all tools when a `MeterRegistry` bean is present — no annotation required.

---

## Decorator Stack

Decorators are applied in this order when you call `ToolCallbacksFactory.from(toolBean)`:

```
ToolCallback (raw Spring AI callback)
    └── GuardedToolCallback         ← @InputGuardrail / @OutputGuardrail
            └── RateLimitedToolCallback     ← @RateLimitedTool
                    └── RetryableToolCallback      ← @RetryableTool
                            └── ConfirmableToolCallback   ← @ConfirmableTool
                                    └── FallbackToolCallback      ← @FallbackTool
                                            └── LoggingToolCallback       ← always applied
                                                    └── MetricsToolCallback   ← applied when Micrometer is present
```

Annotation-driven layers (guarded, rate-limited, retryable, confirmable, fallback) are only added when the corresponding annotation is present on the method. Logging and metrics are always applied to every tool.

The outermost layer (logging/metrics) records the final outcome — after all retries have been exhausted and after the fallback has been applied.

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
  ratelimit/
    annotation/                 # @RateLimitedTool
    callback/                   # RateLimitedToolCallback, RateLimitedToolCallbacks
    exception/                  # RateLimitExceededException
  retry/
    annotation/                 # @RetryableTool
    callback/                   # RetryableToolCallback, RetryableToolCallbacks
  fallback/
    annotation/                 # @FallbackTool
    callback/                   # FallbackToolCallback, FallbackToolCallbacks
    strategy/                   # FallbackToolStrategy
  logging/
    callback/                   # LoggingToolCallback (always applied)
  metrics/
    callback/                   # MetricsToolCallback (applied when Micrometer is present)
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
