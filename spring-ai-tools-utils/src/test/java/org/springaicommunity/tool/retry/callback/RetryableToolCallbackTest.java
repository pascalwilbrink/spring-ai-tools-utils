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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetryableToolCallbackTest {

    @Mock ToolCallback delegate;
    @Mock ToolDefinition toolDefinition;

    @BeforeEach
    void setUp() {
        when(delegate.getToolDefinition()).thenReturn(toolDefinition);
    }

    @Test
    void call_returnsDelegateResult_whenDelegateSucceeds() {
        when(delegate.call("input", null)).thenReturn("ok");

        RetryableToolCallback cb = RetryableToolCallback.wrap(delegate)
            .maxRetries(3)
            .build();

        assertThat(cb.call("input")).isEqualTo("ok");
    }

    @Test
    void call_retriesAndSucceeds_whenDelegateFailsOnFirstAttempt() {
        when(delegate.call("input", null))
            .thenThrow(new RuntimeException("transient"))
            .thenReturn("ok on retry");

        RetryableToolCallback cb = RetryableToolCallback.wrap(delegate)
            .maxRetries(3)
            .build();

        assertThat(cb.call("input")).isEqualTo("ok on retry");
        verify(delegate, times(2)).call("input", null);
    }

    @Test
    void call_exhaustsRetries_andRethrowsLastException() {
        when(delegate.call("input", null)).thenThrow(new RuntimeException("always fails"));

        RetryableToolCallback cb = RetryableToolCallback.wrap(delegate)
            .maxRetries(2)
            .build();

        assertThatThrownBy(() -> cb.call("input"))
            .isInstanceOf(RuntimeException.class);
        // 1 initial + 2 retries = 3 total attempts
        verify(delegate, times(3)).call("input", null);
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
    void call_doesNotRetry_whenMaxRetriesIsOne_andDelegateFails() {
        when(delegate.call("input", null)).thenThrow(new RuntimeException("fail"));

        RetryableToolCallback cb = RetryableToolCallback.wrap(delegate)
            .maxRetries(1)
            .build();

        assertThatThrownBy(() -> cb.call("input"))
            .isInstanceOf(RuntimeException.class);
        // 1 initial + 1 retry = 2 total attempts
        verify(delegate, times(2)).call("input", null);
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
        var metadata = mock(org.springframework.ai.tool.metadata.ToolMetadata.class);
        when(delegate.getToolMetadata()).thenReturn(metadata);

        RetryableToolCallback cb = RetryableToolCallback.wrap(delegate)
            .maxRetries(3)
            .build();

        assertThat(cb.getToolMetadata()).isSameAs(metadata);
    }
}
