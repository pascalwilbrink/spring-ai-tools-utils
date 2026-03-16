package org.springaicommunity.tool.guardrails.builtin;

import org.junit.jupiter.api.Test;
import org.springaicommunity.tool.guardrails.input.InputGuardrailResult;
import org.springaicommunity.tool.guardrails.input.ToolInputGuardrail;
import org.springaicommunity.tool.guardrails.output.OutputGuardrailResult;
import org.springaicommunity.tool.guardrails.output.ToolOutputGuardrail;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuiltinGuardrailsTest {

    private ToolDefinition def(String name) {
        ToolDefinition d = mock(ToolDefinition.class);
        when(d.name()).thenReturn(name);
        return d;
    }

    // ── PathTraversalInputGuardrail ───────────────────────────────────────────

    @Test
    void pathTraversal_passes_cleanInput() {
        ToolInputGuardrail g = Guardrails.pathTraversal();
        InputGuardrailResult r = g.evaluate(def("t"), "/etc/hosts");
        assertThat(r.isPass()).isTrue();
        assertThat(r.sanitizedInput()).isEqualTo("/etc/hosts");
    }

    @Test
    void pathTraversal_blocks_dotDotSequence() {
        ToolInputGuardrail g = Guardrails.pathTraversal();
        assertThat(g.evaluate(def("t"), "../../etc/passwd").isPass()).isFalse();
    }

    // ── SqlInjectionInputGuardrail ────────────────────────────────────────────

    @Test
    void sqlInjection_passes_normalQuery() {
        ToolInputGuardrail g = Guardrails.sqlInjection();
        InputGuardrailResult r = g.evaluate(def("t"), "SELECT name FROM users WHERE id = 1");
        assertThat(r.isPass()).isTrue();
    }

    @Test
    void sqlInjection_blocks_dropKeyword() {
        ToolInputGuardrail g = Guardrails.sqlInjection();
        assertThat(g.evaluate(def("t"), "DROP TABLE users").isPass()).isFalse();
    }

    @Test
    void sqlInjection_blocks_commentSequence() {
        ToolInputGuardrail g = Guardrails.sqlInjection();
        assertThat(g.evaluate(def("t"), "1 OR 1=1 --").isPass()).isFalse();
    }

    @Test
    void sqlInjection_blocks_caseInsensitive() {
        ToolInputGuardrail g = Guardrails.sqlInjection();
        assertThat(g.evaluate(def("t"), "Delete from orders").isPass()).isFalse();
    }

    // ── MaxInputSizeInputGuardrail ────────────────────────────────────────────

    @Test
    void maxInputSize_passes_inputWithinLimit() {
        ToolInputGuardrail g = Guardrails.maxInputSize(100);
        InputGuardrailResult r = g.evaluate(def("t"), "hello");
        assertThat(r.isPass()).isTrue();
        assertThat(r.sanitizedInput()).isEqualTo("hello");
    }

    @Test
    void maxInputSize_blocks_inputExceedingLimit() {
        ToolInputGuardrail g = Guardrails.maxInputSize(5);
        InputGuardrailResult r = g.evaluate(def("t"), "toolong");
        assertThat(r.isPass()).isFalse();
        assertThat(r.failureMessage()).contains("7").contains("5");
    }

    @Test
    void maxInputSize_passes_inputExactlyAtLimit() {
        ToolInputGuardrail g = Guardrails.maxInputSize(5);
        assertThat(g.evaluate(def("t"), "hello").isPass()).isTrue();
    }

    // ── blockKeywords ─────────────────────────────────────────────────────────

    @Test
    void blockKeywords_passes_inputWithNoBlockedWord() {
        ToolInputGuardrail g = Guardrails.blockKeywords(List.of("secret", "confidential"));
        assertThat(g.evaluate(def("t"), "list all users").isPass()).isTrue();
    }

    @Test
    void blockKeywords_blocks_inputContainingBlockedWord() {
        ToolInputGuardrail g = Guardrails.blockKeywords(List.of("secret", "confidential"));
        InputGuardrailResult r = g.evaluate(def("myTool"), "retrieve secret data");
        assertThat(r.isPass()).isFalse();
        assertThat(r.failureMessage()).contains("secret").contains("myTool");
    }

    @Test
    void blockKeywords_isCaseInsensitive() {
        ToolInputGuardrail g = Guardrails.blockKeywords(List.of("secret"));
        assertThat(g.evaluate(def("t"), "reveal SECRET key").isPass()).isFalse();
    }

    // ── allowedTools ──────────────────────────────────────────────────────────

    @Test
    void allowedTools_passes_toolInAllowedSet() {
        ToolInputGuardrail g = Guardrails.allowedTools(Set.of("search", "summarize"));
        assertThat(g.evaluate(def("search"), "query").isPass()).isTrue();
    }

    @Test
    void allowedTools_blocks_toolNotInAllowedSet() {
        ToolInputGuardrail g = Guardrails.allowedTools(Set.of("search"));
        InputGuardrailResult r = g.evaluate(def("deleteTool"), "anything");
        assertThat(r.isPass()).isFalse();
        assertThat(r.failureMessage()).contains("deleteTool");
    }

    // ── SensitiveDataOutputGuardrail ──────────────────────────────────────────

    @Test
    void sensitiveData_passes_cleanOutput() {
        ToolOutputGuardrail g = Guardrails.sensitiveData();
        OutputGuardrailResult r = g.evaluate(def("t"), "User email: user@example.com");
        assertThat(r.isPass()).isTrue();
        assertThat(r.sanitizedOutput()).isEqualTo("User email: user@example.com");
    }

    @Test
    void sensitiveData_redacts_passwordInOutput() {
        ToolOutputGuardrail g = Guardrails.sensitiveData();
        OutputGuardrailResult r = g.evaluate(def("t"), "password: hunter2");
        assertThat(r.isPass()).isTrue();
        assertThat(r.sanitizedOutput()).contains("[REDACTED]").doesNotContain("hunter2");
    }

    @Test
    void sensitiveData_redacts_tokenInOutput() {
        ToolOutputGuardrail g = Guardrails.sensitiveData();
        OutputGuardrailResult r = g.evaluate(def("t"), "token=abc123xyz");
        assertThat(r.sanitizedOutput()).contains("[REDACTED]").doesNotContain("abc123xyz");
    }

    @Test
    void sensitiveData_redacts_apiKeyInOutput() {
        ToolOutputGuardrail g = Guardrails.sensitiveData();
        OutputGuardrailResult r = g.evaluate(def("t"), "api_key: sk-supersecretkey");
        assertThat(r.sanitizedOutput()).doesNotContain("sk-supersecretkey");
    }

    // ── MaxOutputSizeOutputGuardrail ──────────────────────────────────────────

    @Test
    void maxOutputSize_passes_outputWithinLimit() {
        ToolOutputGuardrail g = Guardrails.maxOutputSize(100);
        OutputGuardrailResult r = g.evaluate(def("t"), "short");
        assertThat(r.isPass()).isTrue();
        assertThat(r.sanitizedOutput()).isEqualTo("short");
    }

    @Test
    void maxOutputSize_blocks_outputExceedingLimit() {
        ToolOutputGuardrail g = Guardrails.maxOutputSize(5);
        OutputGuardrailResult r = g.evaluate(def("myTool"), "toolong");
        assertThat(r.isPass()).isFalse();
        assertThat(r.failureMessage()).contains("7").contains("5").contains("myTool");
    }

    // ── redactPattern ─────────────────────────────────────────────────────────

    @Test
    void redactPattern_replacesMatchingContent() {
        ToolOutputGuardrail g = Guardrails.redactPattern("\\d{4}-\\d{4}-\\d{4}-\\d{4}", "****-****-****-****");
        OutputGuardrailResult r = g.evaluate(def("t"), "Card: 1234-5678-9012-3456");
        assertThat(r.isPass()).isTrue();
        assertThat(r.sanitizedOutput())
            .isEqualTo("Card: ****-****-****-****")
            .doesNotContain("1234");
    }

    @Test
    void redactPattern_leavesNonMatchingContentUnchanged() {
        ToolOutputGuardrail g = Guardrails.redactPattern("\\d{3}-\\d{2}-\\d{4}", "[SSN]");
        OutputGuardrailResult r = g.evaluate(def("t"), "No SSN here");
        assertThat(r.sanitizedOutput()).isEqualTo("No SSN here");
    }
}