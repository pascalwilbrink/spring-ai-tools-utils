package org.springaicommunity.tool.guardrails;

import org.junit.jupiter.api.Test;
import org.springaicommunity.tool.guardrails.exception.GuardrailViolationException;
import org.springaicommunity.tool.guardrails.input.InputGuardrailResult;
import org.springaicommunity.tool.guardrails.output.OutputGuardrailResult;

import static org.assertj.core.api.Assertions.assertThat;

class GuardrailModelsTest {

    // ── InputGuardrailResult ──────────────────────────────────────────────────

    @Test
    void inputGuardrailResult_pass_isPassTrue() {
        InputGuardrailResult result = InputGuardrailResult.pass("safe input");

        assertThat(result.isPass()).isTrue();
        assertThat(result.sanitizedInput()).isEqualTo("safe input");
        assertThat(result.failureMessage()).isNull();
        assertThat(result.status()).isEqualTo(InputGuardrailResult.Status.PASS);
    }

    @Test
    void inputGuardrailResult_blocked_isPassFalse() {
        InputGuardrailResult result = InputGuardrailResult.blocked("contains PII");

        assertThat(result.isPass()).isFalse();
        assertThat(result.sanitizedInput()).isNull();
        assertThat(result.failureMessage()).isEqualTo("contains PII");
        assertThat(result.status()).isEqualTo(InputGuardrailResult.Status.BLOCKED);
    }

    // ── OutputGuardrailResult ─────────────────────────────────────────────────

    @Test
    void outputGuardrailResult_pass_isPassTrue() {
        OutputGuardrailResult result = OutputGuardrailResult.pass("safe output");

        assertThat(result.isPass()).isTrue();
        assertThat(result.sanitizedOutput()).isEqualTo("safe output");
        assertThat(result.failureMessage()).isNull();
        assertThat(result.status()).isEqualTo(OutputGuardrailResult.Status.PASS);
    }

    @Test
    void outputGuardrailResult_blocked_isPassFalse() {
        OutputGuardrailResult result = OutputGuardrailResult.blocked("leaked secret");

        assertThat(result.isPass()).isFalse();
        assertThat(result.sanitizedOutput()).isNull();
        assertThat(result.failureMessage()).isEqualTo("leaked secret");
        assertThat(result.status()).isEqualTo(OutputGuardrailResult.Status.BLOCKED);
    }

    // ── GuardrailViolationException ───────────────────────────────────────────

    @Test
    void guardrailViolationException_exposesToolNameAndInput() {
        GuardrailViolationException ex =
            new GuardrailViolationException("myTool", "{\"q\":\"secret\"}", "blocked by policy");

        assertThat(ex.getToolName()).isEqualTo("myTool");
        assertThat(ex.getToolInput()).isEqualTo("{\"q\":\"secret\"}");
        assertThat(ex.getMessage()).contains("myTool").contains("blocked by policy");
    }

    @Test
    void guardrailViolationException_isRuntimeException() {
        assertThat(new GuardrailViolationException("t", "i", "r"))
            .isInstanceOf(RuntimeException.class);
    }
}