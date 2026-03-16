package org.springaicommunity.tool.confirmation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springaicommunity.tool.confirmation.properties.ConfirmationProperties;
import org.springaicommunity.tool.confirmation.store.InMemoryConfirmationStore;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.time.Duration;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AbstractConfirmationHandlerTest {

    @Mock ToolDefinition toolDefinition;

    private InMemoryConfirmationStore store;
    private ConfirmationProperties properties;
    private TestConfirmationHandler handler;

    /** Minimal concrete subclass that records the last pending id. */
    static class TestConfirmationHandler extends AbstractConfirmationHandler {

        String lastConfirmationId;

        TestConfirmationHandler(InMemoryConfirmationStore store, ConfirmationProperties props) {
            super(store, props);
        }

        @Override
        protected void onPendingConfirmation(String confirmationId, ConfirmationRequest request) {
            this.lastConfirmationId = confirmationId;
        }
    }

    @BeforeEach
    void setUp() {
        store = new InMemoryConfirmationStore();
        properties = new ConfirmationProperties(100, Duration.ofSeconds(5));
        handler = new TestConfirmationHandler(store, properties);
    }

    @Test
    void confirm_returnsApproved_whenRespondCalledWithTrue() throws Exception {
        ConfirmationRequest request = new ConfirmationRequest(toolDefinition, "{\"arg\":1}", "approval required");

        // Approve asynchronously after a short delay so confirm() can block first.
        var executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            Thread.sleep(150);
            handler.respond(handler.lastConfirmationId, true, null);
            return null;
        });

        ConfirmationResult result = handler.confirm(request);

        assertThat(result.isApproved()).isTrue();
        executor.shutdown();
    }

    @Test
    void confirm_returnsRejected_whenRespondCalledWithFalse() {
        ConfirmationRequest request = new ConfirmationRequest(toolDefinition, "{}", "approval required");

        var executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            Thread.sleep(50);
            handler.respond(handler.lastConfirmationId, false, "user denied");
            return null;
        });

        ConfirmationResult result = handler.confirm(request);

        assertThat(result.isApproved()).isFalse();
        assertThat(result.rejectionMessage()).isEqualTo("user denied");
        executor.shutdown();
    }

    @Test
    void hasPending_returnsTrueWhileWaiting_andFalseAfterResolve() {
        ConfirmationRequest request = new ConfirmationRequest(toolDefinition, "{}", "approval required");

        var executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            Thread.sleep(50);
            assertThat(handler.hasPending(handler.lastConfirmationId)).isTrue();
            handler.respond(handler.lastConfirmationId, true, null);
            return null;
        });

        handler.confirm(request);

        // After confirm() returns the future has been invalidated.
        assertThat(handler.hasPending("any-id")).isFalse();
        executor.shutdown();
    }

    @Test
    void respond_throwsIllegalArgumentException_forUnknownId() {
        assertThatThrownBy(() -> handler.respond("unknown-id", true, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("unknown-id");
    }

    @Test
    void respond_throwsIllegalArgumentException_whenRejectingWithNoReason() {
        assertThatThrownBy(() -> handler.respond("any-id", false, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reason");

        assertThatThrownBy(() -> handler.respond("any-id", false, "  "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reason");
    }

    @Test
    void confirm_returnsRejected_onTimeout() {
        // Use a very short timeout so the test doesn't block long.
        ConfirmationProperties shortTimeout = new ConfirmationProperties(100, Duration.ofMillis(100));
        TestConfirmationHandler timedOutHandler = new TestConfirmationHandler(store, shortTimeout);

        ConfirmationRequest request = new ConfirmationRequest(toolDefinition, "{}", "approval required");
        ConfirmationResult result = timedOutHandler.confirm(request);

        assertThat(result.isApproved()).isFalse();
        assertThat(result.rejectionMessage()).isEqualTo("Timed out");
    }
}