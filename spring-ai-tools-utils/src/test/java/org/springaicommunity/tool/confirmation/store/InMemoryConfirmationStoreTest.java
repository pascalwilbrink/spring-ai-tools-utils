package org.springaicommunity.tool.confirmation.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springaicommunity.tool.confirmation.ConfirmationRequest;
import org.springframework.ai.tool.definition.ToolDefinition;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InMemoryConfirmationStoreTest {

    @Mock ToolDefinition toolDefinition;

    private InMemoryConfirmationStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryConfirmationStore();
        when(toolDefinition.name()).thenReturn("testTool");
    }

    @Test
    void findById_returnsEmpty_whenNothingSaved() {
        assertThat(store.findById("missing")).isEmpty();
    }

    @Test
    void save_andFindById_roundtrip() {
        PendingConfirmation pending = PendingConfirmation.of(
            "id-1", new ConfirmationRequest(toolDefinition, "{}", "approval required"));

        store.save(pending);

        Optional<PendingConfirmation> found = store.findById("id-1");
        assertThat(found).isPresent();
        assertThat(found.get().confirmationId()).isEqualTo("id-1");
        assertThat(found.get().toolName()).isEqualTo("testTool");
        assertThat(found.get().toolInput()).isEqualTo("{}");
    }

    @Test
    void remove_deletesEntry() {
        store.save(PendingConfirmation.of(
            "id-2", new ConfirmationRequest(toolDefinition, "{}", "approval required")));

        store.remove("id-2");

        assertThat(store.findById("id-2")).isEmpty();
    }

    @Test
    void remove_isNoOp_forUnknownId() {
        // Should not throw
        store.remove("does-not-exist");
    }

    @Test
    void save_overwrites_existingEntry() {
        store.save(PendingConfirmation.of("id-3", new ConfirmationRequest(toolDefinition, "{\"v\":1}", "approval required")));
        store.save(PendingConfirmation.of("id-3", new ConfirmationRequest(toolDefinition, "{\"v\":2}", "approval required")));

        assertThat(store.findById("id-3").get().toolInput()).isEqualTo("{\"v\":2}");
    }
}