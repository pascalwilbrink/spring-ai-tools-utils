package org.springaicommunity.tool.ratelimit.callback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springaicommunity.tool.ratelimit.annotation.RateLimitedTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitedToolCallbacksTest {

    @Mock
    ApplicationContext applicationContext;
    @Mock
    ToolCallback delegate;

    RateLimitedToolCallbacks rateLimitedToolCallbacks;

    static class AnnotatedTools {
        @Tool(name = "myTool")
        @RateLimitedTool(requests = 10, per = RateLimitedTool.RateLimit.MINUTE)
        public String myTool(String input) { return input; }
    }

    static class AnnotatedPerSecondTools {
        @Tool(name = "myTool")
        @RateLimitedTool(requests = 5, per = RateLimitedTool.RateLimit.SECOND)
        public String myTool(String input) { return input; }
    }

    static class AnnotatedPerHourTools {
        @Tool(name = "myTool")
        @RateLimitedTool(requests = 100, per = RateLimitedTool.RateLimit.HOUR)
        public String myTool(String input) { return input; }
    }

    static class NotAnnotatedTools {
        @Tool(name = "myTool")
        public String myTool(String input) { return input; }
    }

    static class ZeroRequestsTools {
        @Tool(name = "myTool")
        @RateLimitedTool(requests = 0)
        public String myTool(String input) { return input; }
    }

    static class NegativeRequestsTools {
        @Tool(name = "myTool")
        @RateLimitedTool(requests = -1)
        public String myTool(String input) { return input; }
    }

    @BeforeEach
    void setUp() {
        rateLimitedToolCallbacks = new RateLimitedToolCallbacks(applicationContext);
    }

    @Test
    void wrap_returnsOriginalCallback_whenAnnotationAbsent() throws Exception {
        Method method = NotAnnotatedTools.class.getDeclaredMethod("myTool", String.class);

        ToolCallback result = rateLimitedToolCallbacks.wrap(delegate, method);

        assertThat(result).isSameAs(delegate);
    }

    @Test
    void wrap_returnsRateLimitedToolCallback_whenAnnotationPresent() throws Exception {
        Method method = AnnotatedTools.class.getDeclaredMethod("myTool", String.class);

        ToolCallback result = rateLimitedToolCallbacks.wrap(delegate, method);

        assertThat(result).isInstanceOf(RateLimitedToolCallback.class);
    }

    @Test
    void wrap_returnsRateLimitedToolCallback_withPerSecond() throws Exception {
        Method method = AnnotatedPerSecondTools.class.getDeclaredMethod("myTool", String.class);

        ToolCallback result = rateLimitedToolCallbacks.wrap(delegate, method);

        assertThat(result).isInstanceOf(RateLimitedToolCallback.class);
    }

    @Test
    void wrap_returnsRateLimitedToolCallback_withPerHour() throws Exception {
        Method method = AnnotatedPerHourTools.class.getDeclaredMethod("myTool", String.class);

        ToolCallback result = rateLimitedToolCallbacks.wrap(delegate, method);

        assertThat(result).isInstanceOf(RateLimitedToolCallback.class);
    }

    @Test
    void wrap_appliesRateLimit_fromAnnotation() throws Exception {
        when(delegate.call("input")).thenReturn("ok");

        Method method = AnnotatedTools.class.getDeclaredMethod("myTool", String.class);
        ToolCallback wrapped = rateLimitedToolCallbacks.wrap(delegate, method);

        // first call should succeed
        assertThat(wrapped.call("input")).isEqualTo("ok");
    }

    @Test
    void wrap_throwsIllegalStateException_whenRequestsIsZero() throws Exception {
        Method method = ZeroRequestsTools.class.getDeclaredMethod("myTool", String.class);

        assertThatThrownBy(() -> rateLimitedToolCallbacks.wrap(delegate, method))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requests");
    }

    @Test
    void wrap_throwsIllegalStateException_whenRequestsIsNegative() throws Exception {
        Method method = NegativeRequestsTools.class.getDeclaredMethod("myTool", String.class);

        assertThatThrownBy(() -> rateLimitedToolCallbacks.wrap(delegate, method))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requests");
    }
}
