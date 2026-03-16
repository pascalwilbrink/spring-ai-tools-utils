package org.springaicommunity;

import org.springaicommunity.tool.callback.ToolCallbacksFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around {@link ChatClient} that exposes the order-management tools
 * decorated with guardrails and confirmation.
 */
@Component
public class OrderAgent {

    private final ChatClient chatClient;

    OrderAgent(ChatModel model, ToolCallbacksFactory factory, OrderTools tools) {
        this.chatClient = ChatClient.builder(model)
            .defaultToolCallbacks(factory.from(tools))
            .build();
    }

    /**
     * Sends a user message to the agent and returns the model's response.
     * Tool calls that require confirmation will block until the user approves
     * or rejects via the SSE + REST flow.
     */
    public String run(String message) {
        return chatClient.prompt(message).call().content();
    }
}
