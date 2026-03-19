package org.springaicommunity.tool.retry.callback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetryableToolCallbackTest {

    @Mock ToolCallback delegate;
    @Mock ToolDefinition toolDefinition;
    @Mock ToolMetadata toolMetadata;

    @BeforeEach
    void setUp() {
        when(delegate.getToolDefinition()).thenReturn(toolDefinition);
        when(delegate.getToolMetadata()).thenReturn(toolMetadata);
    }

    @Test
    void call_returnsDelegateResult_whenDelegateSucceeds() {
        when(delegate.call("input")).thenReturn("ok");

        RetryableToolCallback cb = RetryableToolCallback.wrap(delegate)
                .maxRetries(3)
                .build();

        assertThat(cb.call("input")).isEqualTo("ok");
        verify(delegate, times(1)).call("input");
    }

    @Test
    void call_retriesAndSucceeds_whenDelegateFailsOnFirstAttempt() {
        when(delegate.call("input"))
                .thenThrow(new RuntimeException("transient"))
                .thenReturn("ok on retry");

        RetryableToolCallback cb = RetryableToolCallback.wrap(delegate)
                .maxRetries(3)
                .build();

        assertThat(cb.call("input")).isEqualTo("ok on retry");
        verify(delegate, times(2)).call("input");
    }

    @Test
    void call_exhaustsRetries_andRethrowsLastException() {
        when(delegate.call("input"))
                .thenThrow(new RuntimeException("always fails"));

        RetryableToolCallback cb = RetryableToolCallback.wrap(delegate)
                .maxRetries(2)
                .build();

        assertThatThrownBy(() -> cb.call("input"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("always fails");

        // 1 initial + 2 retries = 3 total attempts
        verify(delegate, times(3)).call("input");
    }

    @Test
    void call_doesNotRetry_whenMaxRetriesIsOne_andDelegateFails() {
        when(delegate.call("input"))
                .thenThrow(new RuntimeException("fail"));

        RetryableToolCallback cb = RetryableToolCallback.wrap(delegate)
                .maxRetries(1)
                .build();

        assertThatThrownBy(() -> cb.call("input"))
                .isInstanceOf(RuntimeException.class);

        // 1 initial + 1 retry = 2 total attempts
        verify(delegate, times(2)).call("input");
    }

    @Test
    void call_withToolContext_delegatesToDelegateWithContext() {
        ToolContext ctx = mock(ToolContext.class);
        when(delegate.call("input", ctx)).thenReturn("ok");

        RetryableToolCallback cb = RetryableToolCallback.wrap(delegate)
                .maxRetries(3)
                .build();

        assertThat(cb.call("input", ctx)).isEqualTo("ok");
        verify(delegate, times(1)).call("input", ctx);
        verify(delegate, never()).call("input");
    }

    @Test
    void call_withToolContext_retriesAndSucceeds() {
        ToolContext ctx = mock(ToolContext.class);
        when(delegate.call("input", ctx))
                .thenThrow(new RuntimeException("transient"))
                .thenReturn("ok");

        RetryableToolCallback cb = RetryableToolCallback.wrap(delegate)
                .maxRetries(3)
                .build();

        assertThat(cb.call("input", ctx)).isEqualTo("ok");
        verify(delegate, times(2)).call("input", ctx);
    }

    @Test
    void call_retriesOnSpecifiedException() {
        when(delegate.call("input"))
                .thenThrow(new IllegalStateException("transient"))
                .thenReturn("ok");

        RetryableToolCallback cb = RetryableToolCallback.wrap(delegate)
                .maxRetries(3)
                .retryOn(IllegalStateException.class)
                .build();

        assertThat(cb.call("input")).isEqualTo("ok");
        verify(delegate, times(2)).call("input");
    }

    @Test
    void call_doesNotRetryOnNonSpecifiedException() {
        when(delegate.call("input"))
                .thenThrow(new RuntimeException("not retryable"));

        RetryableToolCallback cb = RetryableToolCallback.wrap(delegate)
                .maxRetries(3)
                .retryOn(IllegalStateException.class)
                .build();

        assertThatThrownBy(() -> cb.call("input"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("not retryable");

        verify(delegate, times(1)).call("input");
    }

    @Test
    void call_doesNotRetryOnExcludedException() {
        when(delegate.call("input"))
                .thenThrow(new IllegalArgumentException("excluded"));

        RetryableToolCallback cb = RetryableToolCallback.wrap(delegate)
                .maxRetries(3)
                .noRetryOn(IllegalArgumentException.class)
                .build();

        assertThatThrownBy(() -> cb.call("input"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("excluded");

        verify(delegate, times(1)).call("input");
    }

    @Test
    void call_retriesOnNonExcludedException() {
        when(delegate.call("input"))
                .thenThrow(new RuntimeException("retryable"))
                .thenReturn("ok");

        RetryableToolCallback cb = RetryableToolCallback.wrap(delegate)
                .maxRetries(3)
                .noRetryOn(IllegalArgumentException.class)
                .build();

        assertThat(cb.call("input")).isEqualTo("ok");
        verify(delegate, times(2)).call("input");
    }

    @Test
    void getToolDefinition_delegatesToDelegate() {
        RetryableToolCallback cb = RetryableToolCallback.wrap(delegate)
                .maxRetries(3)
                .build();

        assertThat(cb.getToolDefinition()).isSameAs(toolDefinition);
    }

    @Test
    void getToolMetadata_delegatesToDelegate() {
        RetryableToolCallback cb = RetryableToolCallback.wrap(delegate)
                .maxRetries(3)
                .build();

        assertThat(cb.getToolMetadata()).isSameAs(toolMetadata);
    }

}