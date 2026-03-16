package org.springaicommunity.tool.fallback.callback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springaicommunity.tool.fallback.strategy.FallbackToolStrategy;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FallbackToolCallbackTest {

    @Mock ToolCallback delegate;
    @Mock ToolDefinition toolDefinition;

    @BeforeEach
    void setUp() {
        when(delegate.getToolDefinition()).thenReturn(toolDefinition);
    }

    @Test
    void call_returnsDelegateResult_whenDelegateSucceeds() {
        when(delegate.call("input")).thenReturn("ok");

        FallbackToolCallback cb = FallbackToolCallback.wrap(delegate)
            .fallback(FallbackToolStrategy.withMessage("fallback"))
            .build();

        assertThat(cb.call("input")).isEqualTo("ok");
        verify(delegate).call("input");
    }

    @Test
    void call_returnsFallback_whenDelegateThrows() {
        when(delegate.call("input")).thenThrow(new RuntimeException("boom"));

        FallbackToolCallback cb = FallbackToolCallback.wrap(delegate)
            .fallback(FallbackToolStrategy.withMessage("service unavailable"))
            .build();

        assertThat(cb.call("input")).isEqualTo("service unavailable");
    }

    @Test
    void call_passesCauseToStrategy_whenDelegateThrows() {
        RuntimeException cause = new RuntimeException("root cause");
        when(delegate.call("input")).thenThrow(cause);

        FallbackToolStrategy capturingStrategy = (inp, t) -> "caught: " + t.getMessage();

        FallbackToolCallback cb = FallbackToolCallback.wrap(delegate)
            .fallback(capturingStrategy)
            .build();

        assertThat(cb.call("input")).isEqualTo("caught: root cause");
    }

    @Test
    void call_withToolContext_returnsDelegateResult_whenDelegateSucceeds() {
        ToolContext ctx = mock(ToolContext.class);
        when(delegate.call("input", ctx)).thenReturn("ok");

        FallbackToolCallback cb = FallbackToolCallback.wrap(delegate)
            .fallback(FallbackToolStrategy.withMessage("fallback"))
            .build();

        assertThat(cb.call("input", ctx)).isEqualTo("ok");
        verify(delegate).call("input", ctx);
    }

    @Test
    void call_withToolContext_returnsFallback_whenDelegateThrows() {
        ToolContext ctx = mock(ToolContext.class);
        when(delegate.call("input", ctx)).thenThrow(new RuntimeException("boom"));

        FallbackToolCallback cb = FallbackToolCallback.wrap(delegate)
            .fallback(FallbackToolStrategy.withMessage("fallback"))
            .build();

        assertThat(cb.call("input", ctx)).isEqualTo("fallback");
    }

    @Test
    void build_throwsNullPointerException_whenNoStrategySet() {
        assertThatThrownBy(() -> FallbackToolCallback.wrap(delegate).build())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getToolDefinition_delegatesToDelegate() {
        FallbackToolCallback cb = FallbackToolCallback.wrap(delegate)
            .fallback(FallbackToolStrategy.withMessage("fallback"))
            .build();

        assertThat(cb.getToolDefinition()).isSameAs(toolDefinition);
    }
}