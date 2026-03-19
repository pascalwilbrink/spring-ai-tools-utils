package org.springaicommunity;

import org.springaicommunity.tool.callback.ToolCallbacksFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around {@link ChatClient} that exposes the weather tools
 * decorated with ratelimit
 */
@Component
public class WeatherAgent {

    private final ChatClient chatClient;

    WeatherAgent(ChatModel model, ToolCallbacksFactory factory, WeatherTools tools) {
        this.chatClient = ChatClient.builder(model)
            .defaultToolCallbacks(factory.from(tools))
            .build();
    }

    /**
     * Sends a user message to the agent and returns the model's response.
     * Tool calls annotated with {@code @RetryableTool} will be automatically
     * retried on failure before the error is propagated.
     */
    public String run(String message) {
        return chatClient.prompt(message).call().content();
    }
}
