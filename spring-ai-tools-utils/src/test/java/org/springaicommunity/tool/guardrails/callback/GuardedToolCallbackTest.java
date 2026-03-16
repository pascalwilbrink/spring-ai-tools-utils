package org.springaicommunity.tool.guardrails.callback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springaicommunity.tool.guardrails.exception.GuardrailViolationException;
import org.springaicommunity.tool.guardrails.input.InputGuardrailResult;
import org.springaicommunity.tool.guardrails.input.ToolInputGuardrail;
import org.springaicommunity.tool.guardrails.output.OutputGuardrailResult;
import org.springaicommunity.tool.guardrails.output.ToolOutputGuardrail;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GuardedToolCallbackTest {

    @Mock ToolCallback delegate;
    @Mock ToolDefinition toolDefinition;
    @Mock ToolInputGuardrail inputGuardrail;
    @Mock ToolOutputGuardrail outputGuardrail;

    @BeforeEach
    void setUp() {
        when(delegate.getToolDefinition()).thenReturn(toolDefinition);
        when(toolDefinition.name()).thenReturn("testTool");
    }

    @Test
    void call_passesThrough_whenInputGuardrailPasses() {
        when(inputGuardrail.evaluate(eq(toolDefinition), eq("input")))
            .thenReturn(InputGuardrailResult.pass("input"));
        when(delegate.call("input")).thenReturn("output");

        GuardedToolCallback guarded = GuardedToolCallback.wrap(delegate)
            .inputGuardrail(inputGuardrail)
            .build();

        assertThat(guarded.call("input")).isEqualTo("output");
        verify(delegate).call("input");
    }

    @Test
    void call_throwsGuardrailViolationException_whenInputGuardrailBlocks() {
        when(inputGuardrail.evaluate(eq(toolDefinition), eq("bad-input")))
            .thenReturn(InputGuardrailResult.blocked("path traversal detected"));

        GuardedToolCallback guarded = GuardedToolCallback.wrap(delegate)
            .inputGuardrail(inputGuardrail)
            .build();

        assertThatThrownBy(() -> guarded.call("bad-input"))
            .isInstanceOf(GuardrailViolationException.class)
            .hasMessageContaining("path traversal detected");

        verify(delegate, never()).call(any());
    }

    @Test
    void call_usesSanitizedInput_fromInputGuardrail() {
        when(inputGuardrail.evaluate(eq(toolDefinition), eq("raw")))
            .thenReturn(InputGuardrailResult.pass("sanitized"));
        when(delegate.call("sanitized")).thenReturn("output");

        GuardedToolCallback guarded = GuardedToolCallback.wrap(delegate)
            .inputGuardrail(inputGuardrail)
            .build();

        guarded.call("raw");

        verify(delegate).call("sanitized");
    }

    @Test
    void call_chainsMultipleInputGuardrails_inOrder() {
        ToolInputGuardrail second = mock(ToolInputGuardrail.class);

        when(inputGuardrail.evaluate(eq(toolDefinition), eq("raw")))
            .thenReturn(InputGuardrailResult.pass("step1"));
        when(second.evaluate(eq(toolDefinition), eq("step1")))
            .thenReturn(InputGuardrailResult.pass("step2"));
        when(delegate.call("step2")).thenReturn("output");

        GuardedToolCallback guarded = GuardedToolCallback.wrap(delegate)
            .inputGuardrail(inputGuardrail)
            .inputGuardrail(second)
            .build();

        guarded.call("raw");

        verify(delegate).call("step2");
    }

    @Test
    void call_passesThrough_whenOutputGuardrailPasses() {
        when(outputGuardrail.evaluate(eq(toolDefinition), eq("output")))
            .thenReturn(OutputGuardrailResult.pass("output"));
        when(delegate.call("input")).thenReturn("output");

        GuardedToolCallback guarded = GuardedToolCallback.wrap(delegate)
            .outputGuardrail(outputGuardrail)
            .build();

        assertThat(guarded.call("input")).isEqualTo("output");
    }

    @Test
    void call_returnsSanitizedOutput_fromOutputGuardrail() {
        when(outputGuardrail.evaluate(eq(toolDefinition), eq("raw-output")))
            .thenReturn(OutputGuardrailResult.pass("clean-output"));
        when(delegate.call("input")).thenReturn("raw-output");

        GuardedToolCallback guarded = GuardedToolCallback.wrap(delegate)
            .outputGuardrail(outputGuardrail)
            .build();

        assertThat(guarded.call("input")).isEqualTo("clean-output");
    }

    @Test
    void call_throwsGuardrailViolationException_whenOutputGuardrailBlocks() {
        when(delegate.call("input")).thenReturn("sensitive");
        when(outputGuardrail.evaluate(eq(toolDefinition), eq("sensitive")))
            .thenReturn(OutputGuardrailResult.blocked("contains PII"));

        GuardedToolCallback guarded = GuardedToolCallback.wrap(delegate)
            .outputGuardrail(outputGuardrail)
            .build();

        assertThatThrownBy(() -> guarded.call("input"))
            .isInstanceOf(GuardrailViolationException.class)
            .hasMessageContaining("contains PII");
    }

    @Test
    void build_throwsIllegalStateException_whenNoGuardrailsConfigured() {
        assertThatThrownBy(() -> GuardedToolCallback.wrap(delegate).build())
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getToolDefinition_delegatesToDelegate() {
        GuardedToolCallback guarded = GuardedToolCallback.wrap(delegate)
            .inputGuardrail(inputGuardrail)
            .build();

        assertThat(guarded.getToolDefinition()).isSameAs(toolDefinition);
    }
}