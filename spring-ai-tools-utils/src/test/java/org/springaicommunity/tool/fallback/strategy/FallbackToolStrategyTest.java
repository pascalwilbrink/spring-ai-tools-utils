package org.springaicommunity.tool.fallback.strategy;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FallbackToolStrategyTest {

    static class FallbackMethods {
        String noParams()                              { return "no-params"; }
        String withCauseOnly(Throwable cause)          { return "cause:" + cause.getMessage(); }
        String withInputOnly(String input)             { return "input:" + input; }
        String withInputAndCause(String input, Throwable cause) { return "input:" + input + ",cause:" + cause.getMessage(); }
        String returnsNull(String input)               { return null; }
        String throwsException(String input)           { throw new RuntimeException("fallback blew up"); }
    }

    private final FallbackMethods target = new FallbackMethods();
    private final RuntimeException cause = new RuntimeException("original");

    @Test
    void withMessage_alwaysReturnsStaticMessage() {
        FallbackToolStrategy strategy = FallbackToolStrategy.withMessage("service down");

        assertThat(strategy.fallback("any-input", cause)).isEqualTo("service down");
        assertThat(strategy.fallback("other-input", new IllegalStateException())).isEqualTo("service down");
    }

    // ── withMethod — no-params signature ────────────────────────────────────

    @Test
    void withMethod_invokesNoParamFallback() throws Exception {
        Method m = FallbackMethods.class.getDeclaredMethod("noParams");
        FallbackToolStrategy strategy = FallbackToolStrategy.withMethod(target, m);

        assertThat(strategy.fallback("input", cause)).isEqualTo("no-params");
    }

    // ── withMethod — cause-only signature ───────────────────────────────────

    @Test
    void withMethod_invokesCauseOnlyFallback() throws Exception {
        Method m = FallbackMethods.class.getDeclaredMethod("withCauseOnly", Throwable.class);
        FallbackToolStrategy strategy = FallbackToolStrategy.withMethod(target, m);

        assertThat(strategy.fallback("input", cause)).isEqualTo("cause:original");
    }

    // ── withMethod — input-only signature ───────────────────────────────────

    @Test
    void withMethod_invokesInputOnlyFallback() throws Exception {
        Method m = FallbackMethods.class.getDeclaredMethod("withInputOnly", String.class);
        FallbackToolStrategy strategy = FallbackToolStrategy.withMethod(target, m);

        assertThat(strategy.fallback("hello", cause)).isEqualTo("input:hello");
    }

    // ── withMethod — input + cause signature ────────────────────────────────

    @Test
    void withMethod_invokesInputAndCauseFallback() throws Exception {
        Method m = FallbackMethods.class.getDeclaredMethod("withInputAndCause", String.class, Throwable.class);
        FallbackToolStrategy strategy = FallbackToolStrategy.withMethod(target, m);

        assertThat(strategy.fallback("hello", cause)).isEqualTo("input:hello,cause:original");
    }

    // ── edge cases ───────────────────────────────────────────────────────────

    @Test
    void withMethod_returnsEmptyString_whenFallbackMethodReturnsNull() throws Exception {
        Method m = FallbackMethods.class.getDeclaredMethod("returnsNull", String.class);
        FallbackToolStrategy strategy = FallbackToolStrategy.withMethod(target, m);

        assertThat(strategy.fallback("input", cause)).isEqualTo("");
    }

    @Test
    void withMethod_wrapsException_whenFallbackMethodThrows() throws Exception {
        Method m = FallbackMethods.class.getDeclaredMethod("throwsException", String.class);
        FallbackToolStrategy strategy = FallbackToolStrategy.withMethod(target, m);

        assertThatThrownBy(() -> strategy.fallback("input", cause))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("throwsException");
    }
}