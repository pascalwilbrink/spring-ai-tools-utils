package org.springaicommunity;

import org.springaicommunity.tool.callback.ToolCallbacksFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

@Service
public class DatabaseAgent {

    private final ChatClient chatClient;
    private final ToolCallback[] tools;

    public DatabaseAgent(
        ChatClient.Builder builder,
        ToolCallbacksFactory toolCallbacksFactory,
        DatabaseTools databaseTools
    ) {

        this.tools = toolCallbacksFactory.from(databaseTools);

        this.chatClient = builder
                .defaultSystem("""
                You are a helpful database assistant.
                Always tell the user what you are about to do before doing it.
                If a tool fails, explain what happened based on the error returned.
                """)
                .build();
    }

    public String run(String userMessage) {
        return chatClient.prompt()
            .user(userMessage)
            .toolCallbacks(tools)
            .call()
            .content();
    }
}