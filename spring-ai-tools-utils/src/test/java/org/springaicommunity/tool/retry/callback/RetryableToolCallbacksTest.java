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

    // ── tool fixtures ─────────────────────────────────────────────────────────

    static class AnnotatedTools {
        @Tool(name = "myTool")
        @RetryableTool(maxRetries = 3)
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

    // ── setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        retryableToolCallbacks = new RetryableToolCallbacks(applicationContext);
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void wrap_returnsOriginalCallback_whenAnnotationAbsent() throws Exception {
        Method method = NotAnnotatedTools.class.getDeclaredMethod("myTool", String.class);

        ToolCallback result = retryableToolCallbacks.wrap(delegate, method, new NotAnnotatedTools());

        assertThat(result).isSameAs(delegate);
    }

    @Test
    void wrap_returnsRetryableToolCallback_whenAnnotationPresent() throws Exception {
        Method method = AnnotatedTools.class.getDeclaredMethod("myTool", String.class);

        ToolCallback result = retryableToolCallbacks.wrap(delegate, method, new AnnotatedTools());

        assertThat(result).isInstanceOf(RetryableToolCallback.class);
    }

    @Test
    void wrap_retriesUsingMaxRetriesFromAnnotation() throws Exception {
        when(delegate.call("input", null))
            .thenThrow(new RuntimeException("fail"))
            .thenThrow(new RuntimeException("fail"))
            .thenReturn("ok");

        Method method = AnnotatedTools.class.getDeclaredMethod("myTool", String.class);
        ToolCallback wrapped = retryableToolCallbacks.wrap(delegate, method, new AnnotatedTools());

        assertThat(wrapped.call("input")).isEqualTo("ok");
    }

    @Test
    void wrap_throwsIllegalStateException_whenMaxRetriesIsZero() throws Exception {
        Method method = ZeroRetriesTools.class.getDeclaredMethod("myTool", String.class);

        assertThatThrownBy(() -> retryableToolCallbacks.wrap(delegate, method, new ZeroRetriesTools()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("maxRetries");
    }
}
