package org.springaicommunity.tool.guardrails.callback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springaicommunity.tool.guardrails.annotation.InputGuardrail;
import org.springaicommunity.tool.guardrails.annotation.InputGuardrails;
import org.springaicommunity.tool.guardrails.annotation.OutputGuardrail;
import org.springaicommunity.tool.guardrails.input.InputGuardrailResult;
import org.springaicommunity.tool.guardrails.input.ToolInputGuardrail;
import org.springaicommunity.tool.guardrails.output.OutputGuardrailResult;
import org.springaicommunity.tool.guardrails.output.ToolOutputGuardrail;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuardedToolCallbacksTest {

    @Mock ApplicationContext applicationContext;
    @Mock ToolCallback delegate;

    GuardedToolCallbacks guardedToolCallbacks;

    static class PassingInputGuardrail implements ToolInputGuardrail {
        @Override public InputGuardrailResult evaluate(ToolDefinition toolDefinition, String toolInput) {
            return InputGuardrailResult.pass(toolInput);
        }
    }

    static class PassingOutputGuardrail implements ToolOutputGuardrail {
        @Override public OutputGuardrailResult evaluate(ToolDefinition toolDefinition, String toolOutput) {
            return OutputGuardrailResult.pass(toolOutput);
        }
    }

    static class NoGuardrailTools {
        public String myTool(String input) { return input; }
    }

    static class SingleInputGuardrailTools {
        @InputGuardrail(PassingInputGuardrail.class)
        public String myTool(String input) { return input; }
    }

    static class SingleOutputGuardrailTools {
        @OutputGuardrail(PassingOutputGuardrail.class)
        public String myTool(String input) { return input; }
    }

    static class MultipleInputGuardrailTools {
        @InputGuardrails({
            @InputGuardrail(PassingInputGuardrail.class),
            @InputGuardrail(PassingInputGuardrail.class)
        })
        public String myTool(String input) { return input; }
    }

    @BeforeEach
    void setUp() {
        guardedToolCallbacks = new GuardedToolCallbacks(applicationContext);
    }

    @Test
    void wrap_returnsOriginalCallback_whenNoGuardrailAnnotations() throws Exception {
        Method method = NoGuardrailTools.class.getDeclaredMethod("myTool", String.class);
        ToolCallback result = guardedToolCallbacks.wrap(delegate, method);

        assertThat(result).isSameAs(delegate);
    }

    @Test
    void wrap_returnsGuardedCallback_whenSingleInputGuardrail() throws Exception {
        when(applicationContext.getBean(PassingInputGuardrail.class))
            .thenReturn(new PassingInputGuardrail());

        Method method = SingleInputGuardrailTools.class.getDeclaredMethod("myTool", String.class);
        ToolCallback result = guardedToolCallbacks.wrap(delegate, method);

        assertThat(result).isInstanceOf(GuardedToolCallback.class);
    }

    @Test
    void wrap_returnsGuardedCallback_whenSingleOutputGuardrail() throws Exception {
        when(applicationContext.getBean(PassingOutputGuardrail.class))
            .thenReturn(new PassingOutputGuardrail());

        Method method = SingleOutputGuardrailTools.class.getDeclaredMethod("myTool", String.class);
        ToolCallback result = guardedToolCallbacks.wrap(delegate, method);

        assertThat(result).isInstanceOf(GuardedToolCallback.class);
    }

    @Test
    void wrap_returnsGuardedCallback_whenMultipleInputGuardrailsViaContainer() throws Exception {
        when(applicationContext.getBean(PassingInputGuardrail.class))
            .thenReturn(new PassingInputGuardrail());

        Method method = MultipleInputGuardrailTools.class.getDeclaredMethod("myTool", String.class);
        ToolCallback result = guardedToolCallbacks.wrap(delegate, method);

        assertThat(result).isInstanceOf(GuardedToolCallback.class);
    }

}