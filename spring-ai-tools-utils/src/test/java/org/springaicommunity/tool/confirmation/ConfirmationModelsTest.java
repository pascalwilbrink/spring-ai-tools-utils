package org.springaicommunity.tool.confirmation;

import org.junit.jupiter.api.Test;
import org.springaicommunity.tool.confirmation.exception.ToolRejectionException;
import org.springaicommunity.tool.confirmation.properties.ConfirmationProperties;
import org.springaicommunity.tool.confirmation.store.PendingConfirmation;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfirmationModelsTest {

    // ── ConfirmationResult ────────────────────────────────────────────────────

    @Test
    void confirmationResult_approved_isApprovedTrue() {
        ConfirmationResult result = ConfirmationResult.approved();

        assertThat(result.isApproved()).isTrue();
        assertThat(result.status()).isEqualTo(ConfirmationResult.Status.APPROVED);
        assertThat(result.rejectionMessage()).isNull();
    }

    @Test
    void confirmationResult_rejected_isApprovedFalse() {
        ConfirmationResult result = ConfirmationResult.rejected("policy violation");

        assertThat(result.isApproved()).isFalse();
        assertThat(result.status()).isEqualTo(ConfirmationResult.Status.REJECTED);
        assertThat(result.rejectionMessage()).isEqualTo("policy violation");
    }

    // ── ToolRejectionException ────────────────────────────────────────────────

    @Test
    void toolRejectionException_exposesToolNameAndInput() {
        ToolRejectionException ex =
            new ToolRejectionException("myTool", "{\"id\":1}", "user denied");

        assertThat(ex.getToolName()).isEqualTo("myTool");
        assertThat(ex.getToolInput()).isEqualTo("{\"id\":1}");
        assertThat(ex.getMessage()).contains("myTool").contains("user denied");
    }

    @Test
    void toolRejectionException_isRuntimeException() {
        assertThat(new ToolRejectionException("t", "i", "r"))
            .isInstanceOf(RuntimeException.class);
    }

    // ── AutoApproveConfirmationHandler ────────────────────────────────────────

    @Test
    void autoApproveConfirmationHandler_alwaysApproves() {
        ToolDefinition def = mock(ToolDefinition.class);
        when(def.name()).thenReturn("myTool");

        ConfirmationRequest request = new ConfirmationRequest(def, "{}", "Please approve");
        ConfirmationResult result = AutoApproveConfirmationHandler.INSTANCE.confirm(request);

        assertThat(result.isApproved()).isTrue();
    }

    @Test
    void autoRejectConfirmationHandler_alwaysRejects() {
        ToolDefinition def = mock(ToolDefinition.class);
        when(def.name()).thenReturn("myTool");

        ConfirmationRequest request = new ConfirmationRequest(def, "{}", "Please approve");
        ConfirmationResult result = AutoRejectConfirmationHandler.INSTANCE.confirm(request);

        assertThat(result.isApproved()).isFalse();
    }


    @Test
    void autoApproveConfirmationHandler_instanceIsSingleton() {
        assertThat(AutoApproveConfirmationHandler.INSTANCE)
            .isSameAs(AutoApproveConfirmationHandler.INSTANCE);
    }

    // ── ConfirmationProperties ────────────────────────────────────────────────

    @Test
    void confirmationProperties_defaults() {
        ConfirmationProperties props = new ConfirmationProperties();

        assertThat(props.maxPending()).isEqualTo(1_000);
        assertThat(props.timeout()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void confirmationProperties_customValues() {
        ConfirmationProperties props = new ConfirmationProperties(50, Duration.ofSeconds(10));

        assertThat(props.maxPending()).isEqualTo(50);
        assertThat(props.timeout()).isEqualTo(Duration.ofSeconds(10));
    }

    // ── PendingConfirmation ───────────────────────────────────────────────────

    @Test
    void pendingConfirmation_of_populatesFieldsFromRequest() {
        ToolDefinition def = mock(ToolDefinition.class);
        when(def.name()).thenReturn("searchTool");

        ConfirmationRequest request = new ConfirmationRequest(def, "{\"q\":\"test\"}", "confirm?");
        PendingConfirmation pending = PendingConfirmation.of("id-123", request);

        assertThat(pending.confirmationId()).isEqualTo("id-123");
        assertThat(pending.toolName()).isEqualTo("searchTool");
        assertThat(pending.toolInput()).isEqualTo("{\"q\":\"test\"}");
        assertThat(pending.createdAt()).isNotNull();
    }
}