package org.springaicommunity.tool.callback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springaicommunity.tool.confirmation.callback.ConfirmableToolCallbacks;
import org.springaicommunity.tool.fallback.callback.FallbackToolCallbacks;
import org.springaicommunity.tool.guardrails.callback.GuardedToolCallbacks;
import org.springaicommunity.tool.retry.callback.RetryableToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ToolCallbacksFactoryTest {

    @Mock GuardedToolCallbacks guardedToolCallbacks;
    @Mock ConfirmableToolCallbacks confirmableToolCallbacks;
    @Mock FallbackToolCallbacks fallbackToolCallbacks;
    @Mock
    RetryableToolCallbacks retryableToolCallbacks;
    
    ToolCallbacksFactory factory;

    // ── tool fixtures ─────────────────────────────────────────────────────────

    static class SingleToolObject {
        @Tool(name = "myTool")
        public String myTool(String input) { return input; }
    }

    static class TwoToolObject {
        @Tool(name = "toolA")
        public String toolA(String input) { return input; }

        @Tool(name = "toolB")
        public String toolB(String input) { return input; }
    }

    // @Tool without explicit name — tool name defaults to method name
    static class ImplicitNameToolObject {
        @Tool
        public String implicitName(String input) { return input; }
    }


    // ── setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        factory = new ToolCallbacksFactory(
            guardedToolCallbacks, confirmableToolCallbacks, fallbackToolCallbacks, retryableToolCallbacks
        );
    }

    // ── decorator chaining ────────────────────────────────────────────────────

    @Test
    void from_appliesDecoratorsInOrder_guardedThenConfirmableThenRetryableThenFallback() {
        ToolCallback guardedCb     = mock(ToolCallback.class);
        ToolCallback confirmableCb = mock(ToolCallback.class);
        ToolCallback retryableCb   = mock(ToolCallback.class);
        ToolCallback fallbackCb    = mock(ToolCallback.class);

        // Each wrapper receives the output of the previous one
        when(guardedToolCallbacks.wrap(any(ToolCallback.class), any(Method.class)))
            .thenReturn(guardedCb);
        when(confirmableToolCallbacks.wrap(eq(guardedCb), any(Method.class)))
            .thenReturn(confirmableCb);
        when(retryableToolCallbacks.wrap(eq(confirmableCb), any(Method.class), any()))
            .thenReturn(retryableCb);
        when(fallbackToolCallbacks.wrap(eq(retryableCb), any(Method.class), any()))
            .thenReturn(fallbackCb);

        ToolCallback[] result = factory.from(new SingleToolObject());

        assertThat(result).hasSize(1);
        assertThat(result[0]).isSameAs(fallbackCb);
    }

    @Test
    void from_producesOneCallbackPerToolMethod() {
        passThrough();

        ToolCallback[] result = factory.from(new TwoToolObject());

        assertThat(result).hasSize(2);
    }

    // ── method lookup ─────────────────────────────────────────────────────────

    @Test
    void from_findsMethodByExplicitToolName() {
        passThrough();

        factory.from(new SingleToolObject());

        // Verify guarded wrapper was called — means method was found
        verify(guardedToolCallbacks).wrap(any(ToolCallback.class), any(Method.class));
    }

    @Test
    void from_findsMethodByImplicitName_whenToolNameBlank() {
        passThrough();

        factory.from(new ImplicitNameToolObject());

        verify(guardedToolCallbacks).wrap(any(ToolCallback.class), any(Method.class));
    }

    @Test
    void from_decoratesEachToolMethod_inMultiToolObject() {
        passThrough();

        ToolCallback[] result = factory.from(new TwoToolObject());

        // Both methods must pass through all four decoration layers
        assertThat(result).hasSize(2);
        verify(guardedToolCallbacks, org.mockito.Mockito.times(2))
            .wrap(any(ToolCallback.class), any(Method.class));
        verify(confirmableToolCallbacks, org.mockito.Mockito.times(2))
            .wrap(any(ToolCallback.class), any(Method.class));
        verify(retryableToolCallbacks, org.mockito.Mockito.times(2))
            .wrap(any(ToolCallback.class), any(Method.class), any());
        verify(fallbackToolCallbacks, org.mockito.Mockito.times(2))
            .wrap(any(ToolCallback.class), any(Method.class), any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Configure all four wrappers to return whatever they receive (pass-through). */
    private void passThrough() {
        when(guardedToolCallbacks.wrap(any(ToolCallback.class), any(Method.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(confirmableToolCallbacks.wrap(any(ToolCallback.class), any(Method.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(retryableToolCallbacks.wrap(any(ToolCallback.class), any(Method.class), any()))
            .thenAnswer(inv -> inv.getArgument(0));
        when(fallbackToolCallbacks.wrap(any(ToolCallback.class), any(Method.class), any()))
            .thenAnswer(inv -> inv.getArgument(0));
    }
}