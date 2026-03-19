package org.springaicommunity.tool.retry.callback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springaicommunity.tool.retry.annotation.RetryableTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryableToolCallbacksTest {

    @Mock ApplicationContext applicationContext;
    @Mock ToolCallback delegate;

    RetryableToolCallbacks retryableToolCallbacks;

    static class AnnotatedTools {
        @Tool(name = "myTool")
        @RetryableTool(maxRetries = 3)
        public String myTool(String input) { return input; }
    }

    static class AnnotatedWithDelayTools {
        @Tool(name = "myTool")
        @RetryableTool(maxRetries = 3, delay = 500)
        public String myTool(String input) { return input; }
    }

    static class AnnotatedWithExponentialBackoffTools {
        @Tool(name = "myTool")
        @RetryableTool(maxRetries = 3, delay = 500, multiplier = 2.0)
        public String myTool(String input) { return input; }
    }

    static class AnnotatedWithRetryOnTools {
        @Tool(name = "myTool")
        @RetryableTool(maxRetries = 3, retryOn = { IllegalStateException.class })
        public String myTool(String input) { return input; }
    }

    static class AnnotatedWithNoRetryOnTools {
        @Tool(name = "myTool")
        @RetryableTool(maxRetries = 3, noRetryOn = { IllegalArgumentException.class })
        public String myTool(String input) { return input; }
    }

    static class NotAnnotatedTools {
        @Tool(name = "myTool")
        public String myTool(String input) { return input; }
    }

    static class ZeroRetriesTools {
        @Tool(name = "myTool")
        @RetryableTool(maxRetries = 0)
        public String myTool(String input) { return input; }
    }

    @BeforeEach
    void setUp() {
        retryableToolCallbacks = new RetryableToolCallbacks(applicationContext);
    }

    @Test
    void wrap_returnsOriginalCallback_whenAnnotationAbsent() throws Exception {
        Method method = NotAnnotatedTools.class.getDeclaredMethod("myTool", String.class);

        ToolCallback result = retryableToolCallbacks.wrap(delegate, method);

        assertThat(result).isSameAs(delegate);
    }

    @Test
    void wrap_returnsRetryableToolCallback_whenAnnotationPresent() throws Exception {
        Method method = AnnotatedTools.class.getDeclaredMethod("myTool", String.class);

        ToolCallback result = retryableToolCallbacks.wrap(delegate, method);

        assertThat(result).isInstanceOf(RetryableToolCallback.class);
    }

    @Test
    void wrap_retriesUntilSuccess() throws Exception {
        when(delegate.call("input"))
                .thenThrow(new RuntimeException("fail 1"))
                .thenThrow(new RuntimeException("fail 2"))
                .thenReturn("ok");

        Method method = AnnotatedTools.class.getDeclaredMethod("myTool", String.class);
        ToolCallback wrapped = retryableToolCallbacks.wrap(delegate, method);

        assertThat(wrapped.call("input")).isEqualTo("ok");
    }

    @Test
    void wrap_rethrowsAfterMaxRetriesExhausted() throws Exception {
        when(delegate.call("input"))
                .thenThrow(new RuntimeException("fail 1"))
                .thenThrow(new RuntimeException("fail 2"))
                .thenThrow(new RuntimeException("fail 3"))
                .thenThrow(new RuntimeException("fail 4"));

        Method method = AnnotatedTools.class.getDeclaredMethod("myTool", String.class);
        ToolCallback wrapped = retryableToolCallbacks.wrap(delegate, method);

        assertThatThrownBy(() -> wrapped.call("input"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("fail 4");
    }

    @Test
    void wrap_retriesOnSpecifiedException() throws Exception {
        when(delegate.call("input"))
                .thenThrow(new IllegalStateException("transient"))
                .thenReturn("ok");

        Method method = AnnotatedWithRetryOnTools.class
                .getDeclaredMethod("myTool", String.class);
        ToolCallback wrapped = retryableToolCallbacks.wrap(delegate, method);

        assertThat(wrapped.call("input")).isEqualTo("ok");
    }

    @Test
    void wrap_doesNotRetryOnNonSpecifiedException() throws Exception {
        when(delegate.call("input"))
                .thenThrow(new RuntimeException("not retryable"));

        Method method = AnnotatedWithRetryOnTools.class
                .getDeclaredMethod("myTool", String.class);
        ToolCallback wrapped = retryableToolCallbacks.wrap(delegate, method);

        assertThatThrownBy(() -> wrapped.call("input"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("not retryable");
    }

    @Test
    void wrap_doesNotRetryOnExcludedException() throws Exception {
        when(delegate.call("input"))
                .thenThrow(new IllegalArgumentException("excluded"));

        Method method = AnnotatedWithNoRetryOnTools.class
                .getDeclaredMethod("myTool", String.class);
        ToolCallback wrapped = retryableToolCallbacks.wrap(delegate, method);

        assertThatThrownBy(() -> wrapped.call("input"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("excluded");
    }

    @Test
    void wrap_retriesOnNonExcludedException() throws Exception {
        when(delegate.call("input"))
                .thenThrow(new RuntimeException("retryable"))
                .thenReturn("ok");

        Method method = AnnotatedWithNoRetryOnTools.class
                .getDeclaredMethod("myTool", String.class);
        ToolCallback wrapped = retryableToolCallbacks.wrap(delegate, method);

        assertThat(wrapped.call("input")).isEqualTo("ok");
    }

    @Test
    void wrap_returnsRetryableToolCallback_withCustomDelay() throws Exception {
        Method method = AnnotatedWithDelayTools.class
                .getDeclaredMethod("myTool", String.class);

        ToolCallback result = retryableToolCallbacks.wrap(delegate, method);

        assertThat(result).isInstanceOf(RetryableToolCallback.class);
    }

    @Test
    void wrap_returnsRetryableToolCallback_withExponentialBackoff() throws Exception {
        Method method = AnnotatedWithExponentialBackoffTools.class
                .getDeclaredMethod("myTool", String.class);

        ToolCallback result = retryableToolCallbacks.wrap(delegate, method);

        assertThat(result).isInstanceOf(RetryableToolCallback.class);
    }

    @Test
    void wrap_throwsIllegalStateException_whenMaxRetriesIsZero() throws Exception {
        Method method = ZeroRetriesTools.class.getDeclaredMethod("myTool", String.class);

        assertThatThrownBy(() -> retryableToolCallbacks.wrap(delegate, method))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("maxRetries");
    }

}