# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Spring AI community library providing decorator-based utilities for Spring AI tool callbacks:
- **Guardrails**: Input/output validation and sanitization for AI tool calls
- **Tool Confirmation**: User approval workflows before sensitive tool operations execute

Multi-module Maven project: `spring-ai-tools-utils` (core library) + `examples/` (reference implementations).

## Build & Test Commands

```bash
# Compile
mvn clean compile

# Run all tests
mvn test

# Run a single test class
mvn test -pl spring-ai-tools-utils -Dtest=ClassName

# Build JAR
mvn clean package

# Build with Javadoc
mvn clean package -Pjavadoc

# Deploy to Maven Central (requires GPG key)
mvn deploy -Prelease
```

## Architecture

The library implements a **decorator pattern** stacking two layers around Spring AI `ToolCallback` instances:

```
ToolCallback (delegate)
    ↑ wrapped by
GuardedToolCallback       ← validates input/output via @InputGuardrail / @OutputGuardrail annotations
    ↑ wrapped by
ConfirmableToolCallback   ← requires user approval via @ConfirmableTool annotation
```

### Entry Point

**`ToolCallbacksFactory.from(Object toolsBean)`** — discovers all `@Tool`-annotated methods on a bean, then applies decorators based on annotations found on each method. Returns a `ToolCallback[]` ready for use with Spring AI's `ChatClient`.

### Guardrails Layer (`org.springaicommunity.tool.guardrails`)

- Annotate `@Tool` methods with `@InputGuardrail(guardrailClass)` and/or `@OutputGuardrail(guardrailClass)`
- Input guardrail: implement `ToolInputGuardrail` → returns `InputGuardrailResult` (PASS/BLOCKED + optional sanitized input)
- Output guardrail: implement `ToolOutputGuardrail` → returns `OutputGuardrailResult` (PASS/BLOCKED + optional sanitized output)
- Violation throws `GuardrailViolationException`

### Confirmation Layer (`org.springaicommunity.tool.confirmation`)

- Annotate `@Tool` methods with `@ConfirmableTool(handler = SomeHandler.class)`
- Implement `ConfirmationHandler` (functional interface) or extend `AbstractConfirmationHandler`
- Handler receives `ConfirmationRequest` (tool definition + input) and returns `ConfirmationResult` (APPROVED/REJECTED)
- Rejection throws `ToolRejectionException`
- `InMemoryConfirmationStore` holds pending confirmations (backed by Caffeine cache)
- `AutoApproveConfirmationHandler` is the default no-op handler

### Auto-Configuration

`ToolsAutoConfiguration` registers `ToolCallbacksFactory`, `GuardedToolCallbacks`, `ConfirmableToolCallbacks`, and `InMemoryConfirmationStore` as Spring beans automatically when the library is on the classpath.

## Key Dependencies

- **Spring AI** `1.1.2` — `spring-ai-client-chat`
- **Spring Framework** `7.0.1`
- **Spring Boot** `3.4.1` (examples only)
- **Caffeine** — in-memory confirmation store
- **Java 21** (core module), Java 17 (parent POM minimum)

## Module Layout

```
pom.xml                          # Parent POM, module aggregator
spring-ai-tools-utils/           # Core library (published artifact)
  src/main/java/org/springaicommunity/tool/
    callback/                    # ToolCallbacksFactory, AbstractToolCallbacks
    guardrails/                  # annotation/, callback/, input/, output/, exception/
    confirmation/                # annotation/, callback/, store/, properties/, exception/
    configuration/               # ToolsAutoConfiguration
examples/
  tool-confirmation/             # ConsoleConfirmationHandler demo
  tool-guardrails/               # PathTraversal + SensitiveData guardrail demo
```