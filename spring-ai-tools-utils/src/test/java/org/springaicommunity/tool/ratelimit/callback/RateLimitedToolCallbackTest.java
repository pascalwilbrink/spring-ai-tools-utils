package org.springaicommunity.tool.ratelimit.callback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springaicommunity.tool.ratelimit.annotation.RateLimitedTool;
import org.springaicommunity.tool.ratelimit.exception.RateLimitExceededException;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitedToolCallbackTest {

    @Mock
    ToolCallback delegate;
    @Mock
    ToolDefinition toolDefinition;
    @Mock
    ToolMetadata toolMetadata;

    @BeforeEach
    void setUp() {
        when(delegate.getToolDefinition()).thenReturn(toolDefinition);
        when(delegate.getToolMetadata()).thenReturn(toolMetadata);
        when(toolDefinition.name()).thenReturn("testTool");
    }

    @Test
    void call_delegatesWhenRateLimitNotExceeded() {
        when(delegate.call("input")).thenReturn("ok");

        RateLimitedToolCallback cb = RateLimitedToolCallback.wrap(delegate)
                .requests(10)
                .per(RateLimitedTool.RateLimit.MINUTE)
                .build();

        assertThat(cb.call("input")).isEqualTo("ok");
        verify(delegate).call("input");
    }

    @Test
    void call_withToolContext_delegatesWhenRateLimitNotExceeded() {
        ToolContext ctx = mock(ToolContext.class);
        when(delegate.call("input", ctx)).thenReturn("ok");

        RateLimitedToolCallback cb = RateLimitedToolCallback.wrap(delegate)
                .requests(10)
                .per(RateLimitedTool.RateLimit.MINUTE)
                .build();

        assertThat(cb.call("input", ctx)).isEqualTo("ok");
        verify(delegate, times(1)).call("input", ctx);
        verify(delegate, never()).call("input");
    }

    @Test
    void call_allowsFirstCall_withOneRequestPerMinute() {
        when(delegate.call("input")).thenReturn("ok");

        RateLimitedToolCallback cb = RateLimitedToolCallback.wrap(delegate)
                .requests(1)
                .per(RateLimitedTool.RateLimit.MINUTE)
                .build();

        assertThat(cb.call("input")).isEqualTo("ok");
    }

    @Test
    void call_allowsFirstCall_withOneRequestPerHour() {
        when(delegate.call("input")).thenReturn("ok");

        RateLimitedToolCallback cb = RateLimitedToolCallback.wrap(delegate)
                .requests(1)
                .per(RateLimitedTool.RateLimit.HOUR)
                .build();

        assertThat(cb.call("input")).isEqualTo("ok");
    }

    @Test
    void call_allowsFirstCall_withOneRequestPerSecond() {
        when(delegate.call("input")).thenReturn("ok");

        RateLimitedToolCallback cb = RateLimitedToolCallback.wrap(delegate)
                .requests(1)
                .per(RateLimitedTool.RateLimit.SECOND)
                .build();

        assertThat(cb.call("input")).isEqualTo("ok");
    }

    @Test
    void call_throwsRateLimitExceededException_whenRateLimitExceeded() {
        when(delegate.call("input")).thenReturn("ok");

        RateLimitedToolCallback cb = RateLimitedToolCallback.wrap(delegate)
                .requests(1)
                .per(RateLimitedTool.RateLimit.MINUTE)
                .build();

        cb.call("input"); // first call — within limit

        assertThatThrownBy(() -> cb.call("input")) // second call — exceeds limit
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("testTool");
    }

    @Test
    void call_throwsRateLimitExceededException_withCorrectToolName() {
        when(delegate.call("input")).thenReturn("ok");
        when(toolDefinition.name()).thenReturn("getExchangeRate");

        RateLimitedToolCallback cb = RateLimitedToolCallback.wrap(delegate)
                .requests(1)
                .per(RateLimitedTool.RateLimit.MINUTE)
                .build();

        cb.call("input");

        assertThatThrownBy(() -> cb.call("input"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("getExchangeRate");
    }

    @Test
    void call_doesNotDelegateWhenRateLimitExceeded() {
        when(delegate.call("input")).thenReturn("ok");

        RateLimitedToolCallback cb = RateLimitedToolCallback.wrap(delegate)
                .requests(1)
                .per(RateLimitedTool.RateLimit.MINUTE)
                .build();

        cb.call("input");

        assertThatThrownBy(() -> cb.call("input"))
                .isInstanceOf(RateLimitExceededException.class);

        verify(delegate, times(1)).call("input"); // delegate called only once
    }

    @Test
    void build_throwsIllegalStateException_whenRequestsIsZero() {
        assertThatThrownBy(() -> RateLimitedToolCallback.wrap(delegate)
                .requests(0)
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requests");
    }

    @Test
    void build_throwsIllegalStateException_whenRequestsIsNegative() {
        assertThatThrownBy(() -> RateLimitedToolCallback.wrap(delegate)
                .requests(-1)
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requests");
    }

    @Test
    void getToolDefinition_delegatesToDelegate() {
        RateLimitedToolCallback cb = RateLimitedToolCallback.wrap(delegate)
                .requests(10)
                .per(RateLimitedTool.RateLimit.MINUTE)
                .build();

        assertThat(cb.getToolDefinition()).isSameAs(toolDefinition);
    }

    @Test
    void getToolMetadata_delegatesToDelegate() {
        RateLimitedToolCallback cb = RateLimitedToolCallback.wrap(delegate)
                .requests(10)
                .per(RateLimitedTool.RateLimit.MINUTE)
                .build();

        assertThat(cb.getToolMetadata()).isSameAs(toolMetadata);
    }

}