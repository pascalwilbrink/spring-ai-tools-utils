package org.springaicommunity.tool.confirmation.callback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springaicommunity.tool.confirmation.ConfirmationHandler;
import org.springaicommunity.tool.confirmation.ConfirmationResult;
import org.springaicommunity.tool.confirmation.exception.ToolRejectionException;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfirmableToolCallbackTest {

    @Mock ToolCallback delegate;
    @Mock ToolDefinition toolDefinition;
    @Mock ConfirmationHandler handler;

    @BeforeEach
    void setUp() {
        when(delegate.getToolDefinition()).thenReturn(toolDefinition);
        when(toolDefinition.name()).thenReturn("testTool");
    }

    @Test
    void call_executesDelegate_whenApproved() {
        when(handler.confirm(any())).thenReturn(ConfirmationResult.approved());
        when(delegate.call("input")).thenReturn("result");

        ConfirmableToolCallback callback = ConfirmableToolCallback.wrap(delegate)
            .confirmation(handler)
            .build();

        assertThat(callback.call("input")).isEqualTo("result");
        verify(delegate).call("input");
    }

    @Test
    void call_throwsToolRejectionException_whenRejected() {
        when(handler.confirm(any())).thenReturn(ConfirmationResult.rejected("not allowed"));

        ConfirmableToolCallback callback = ConfirmableToolCallback.wrap(delegate)
            .confirmation(handler)
            .build();

        assertThatThrownBy(() -> callback.call("input"))
            .isInstanceOf(ToolRejectionException.class)
            .hasMessageContaining("not allowed");

        verify(delegate, never()).call(any());
    }

    @Test
    void call_withToolContext_executesDelegateWithContext_whenApproved() {
        ToolContext ctx = mock(ToolContext.class);
        when(handler.confirm(any())).thenReturn(ConfirmationResult.approved());
        when(delegate.call("input", ctx)).thenReturn("result");

        ConfirmableToolCallback callback = ConfirmableToolCallback.wrap(delegate)
            .confirmation(handler)
            .build();

        assertThat(callback.call("input", ctx)).isEqualTo("result");
        verify(delegate).call("input", ctx);
    }

    @Test
    void call_withToolContext_throwsToolRejectionException_whenRejected() {
        ToolContext ctx = mock(ToolContext.class);
        when(handler.confirm(any())).thenReturn(ConfirmationResult.rejected("denied"));

        ConfirmableToolCallback callback = ConfirmableToolCallback.wrap(delegate)
            .confirmation(handler)
            .build();

        assertThatThrownBy(() -> callback.call("input", ctx))
            .isInstanceOf(ToolRejectionException.class);

        verify(delegate, never()).call(any(), any());
    }

    @Test
    void build_throwsNullPointerException_whenNoHandlerSet() {
        assertThatThrownBy(() -> ConfirmableToolCallback.wrap(delegate).build())
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void getToolDefinition_delegatesToDelegate() {
        ConfirmableToolCallback callback = ConfirmableToolCallback.wrap(delegate)
            .confirmation(handler)
            .build();

        assertThat(callback.getToolDefinition()).isSameAs(toolDefinition);
    }
}