package org.springaicommunity.tool.fallback.callback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springaicommunity.tool.fallback.annotation.FallbackTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FallbackToolCallbacksTest {

    @Mock ApplicationContext applicationContext;
    @Mock ToolCallback delegate;

    FallbackToolCallbacks fallbackToolCallbacks;

    // ── tool classes used as test fixtures ───────────────────────────────────

    static class MessageFallbackTools {
        @Tool(name = "myTool")
        @FallbackTool(message = "service unavailable")
        public String myTool(String input) { return input; }
    }

    static class MethodFallbackTools {
        @Tool(name = "myTool")
        @FallbackTool(method = "myFallback")
        public String myTool(String input) { return input; }

        public String myFallback(String input, Throwable cause) { return "fallback"; }
    }

    static class NoFallbackTools {
        @Tool(name = "myTool")
        public String myTool(String input) { return input; }
    }

    static class BothSpecifiedTools {
        @Tool(name = "myTool")
        @FallbackTool(method = "myFallback", message = "msg")
        public String myTool(String input) { return input; }

        public String myFallback() { return "fallback"; }
    }

    static class NeitherSpecifiedTools {
        @Tool(name = "myTool")
        @FallbackTool
        public String myTool(String input) { return input; }
    }

    static class MissingFallbackMethodTools {
        @Tool(name = "myTool")
        @FallbackTool(method = "doesNotExist")
        public String myTool(String input) { return input; }
    }

    // ── setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        fallbackToolCallbacks = new FallbackToolCallbacks(applicationContext);
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void wrap_returnsOriginalCallback_whenNoAnnotationPresent() throws Exception {
        Method method = NoFallbackTools.class.getDeclaredMethod("myTool", String.class);
        ToolCallback result = fallbackToolCallbacks.wrap(delegate, method, new NoFallbackTools());

        assertThat(result).isSameAs(delegate);
    }

    @Test
    void wrap_returnsFallbackToolCallback_whenMessageAnnotationPresent() throws Exception {
        Method method = MessageFallbackTools.class.getDeclaredMethod("myTool", String.class);
        ToolCallback result = fallbackToolCallbacks.wrap(delegate, method, new MessageFallbackTools());

        assertThat(result).isInstanceOf(FallbackToolCallback.class);
    }

    @Test
    void wrap_returnsFallbackToolCallback_whenMethodAnnotationPresent() throws Exception {
        Method method = MethodFallbackTools.class.getDeclaredMethod("myTool", String.class);
        ToolCallback result = fallbackToolCallbacks.wrap(delegate, method, new MethodFallbackTools());

        assertThat(result).isInstanceOf(FallbackToolCallback.class);
    }

    @Test
    void wrap_messageFallback_returnsStaticMessage_whenDelegateThrows() throws Exception {
        when(delegate.call("input")).thenThrow(new RuntimeException("boom"));

        Method method = MessageFallbackTools.class.getDeclaredMethod("myTool", String.class);
        ToolCallback wrapped = fallbackToolCallbacks.wrap(delegate, method, new MessageFallbackTools());

        assertThat(wrapped.call("input")).isEqualTo("service unavailable");
    }

    @Test
    void wrap_methodFallback_invokesMethod_whenDelegateThrows() throws Exception {
        when(delegate.call("input")).thenThrow(new RuntimeException("boom"));

        MethodFallbackTools toolObject = new MethodFallbackTools();
        Method method = MethodFallbackTools.class.getDeclaredMethod("myTool", String.class);
        ToolCallback wrapped = fallbackToolCallbacks.wrap(delegate, method, toolObject);

        assertThat(wrapped.call("input")).isEqualTo("fallback");
    }

    @Test
    void wrap_throwsIllegalStateException_whenNeitherMethodNorMessageSpecified() throws Exception {
        Method method = NeitherSpecifiedTools.class.getDeclaredMethod("myTool", String.class);

        assertThatThrownBy(() -> fallbackToolCallbacks.wrap(delegate, method, new NeitherSpecifiedTools()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("method()").hasMessageContaining("message()");
    }

    @Test
    void wrap_throwsIllegalStateException_whenBothMethodAndMessageSpecified() throws Exception {
        Method method = BothSpecifiedTools.class.getDeclaredMethod("myTool", String.class);

        assertThatThrownBy(() -> fallbackToolCallbacks.wrap(delegate, method, new BothSpecifiedTools()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("cannot specify both");
    }

    @Test
    void wrap_throwsIllegalStateException_whenFallbackMethodNotFound() throws Exception {
        Method method = MissingFallbackMethodTools.class.getDeclaredMethod("myTool", String.class);

        assertThatThrownBy(() -> fallbackToolCallbacks.wrap(delegate, method, new MissingFallbackMethodTools()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("doesNotExist");
    }
}