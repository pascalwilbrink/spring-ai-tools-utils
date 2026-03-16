package org.springaicommunity.tool.confirmation.callback;

import org.springaicommunity.tool.confirmation.ConfirmationHandler;
import org.springaicommunity.tool.confirmation.ConfirmationRequest;
import org.springaicommunity.tool.confirmation.ConfirmationResult;
import org.springaicommunity.tool.confirmation.exception.ToolRejectionException;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import java.util.Objects;

/**
 * A {@link ToolCallback} decorator that requests human confirmation via a
 * {@link ConfirmationHandler} before delegating to the wrapped callback.
 * <p>
 * Instances are created through the fluent {@link Builder} returned by
 * {@link #wrap(ToolCallback)}.
 * </p>
 */
public class ConfirmableToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final ConfirmationHandler confirmationHandler;
    private final String reason;

    private ConfirmableToolCallback(Builder builder) {
        this.delegate = builder.delegate;
        this.confirmationHandler = builder.confirmationHandler;
        this.reason = builder.reason;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    /**
     * Requests confirmation and, if approved, delegates to the wrapped callback.
     *
     * @throws ToolRejectionException if the {@link ConfirmationHandler} rejects the request
     */
    @Override
    public String call(String toolInput, ToolContext toolContext) {
        ConfirmationResult result = confirmationHandler.confirm(
            new ConfirmationRequest(delegate.getToolDefinition(), toolInput, reason)
        );

        if (!result.isApproved()) {
            throw new ToolRejectionException(
                delegate.getToolDefinition().name(),
                toolInput,
                result.rejectionMessage()
            );
        }

        return toolContext != null
                ? delegate.call(toolInput, toolContext)
                : delegate.call(toolInput);
    }

    /**
     * Begins construction of a {@link ConfirmableToolCallback} wrapping the given delegate.
     *
     * @param delegate the callback to wrap
     * @return a new {@link Builder}
     */
    public static Builder wrap(ToolCallback delegate) {
        return new Builder(delegate);
    }

    /**
     * Fluent builder for {@link ConfirmableToolCallback}.
     */
    public static class Builder {

        private final ToolCallback delegate;
        private ConfirmationHandler confirmationHandler;
        private String reason;

        private Builder(ToolCallback delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        }

        /**
         * Sets the {@link ConfirmationHandler} responsible for obtaining user approval.
         *
         * @param handler the confirmation handler to use
         * @return this builder
         */
        public Builder confirmation(ConfirmationHandler handler) {
            this.confirmationHandler = handler;
            return this;
        }

        /**
         * Sets the human-readable reason presented during the confirmation request.
         *
         * @param reason the reason string to present to the user
         * @return this builder
         */
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        /**
         * Builds the {@link ConfirmableToolCallback}.
         *
         * @return the configured {@link ConfirmableToolCallback}
         * @throws NullPointerException if no {@link ConfirmationHandler} has been set
         */
        public ConfirmableToolCallback build() {
            Objects.requireNonNull(confirmationHandler, "ConfirmationHandler must not be null");
            return new ConfirmableToolCallback(this);
        }
    }
}
