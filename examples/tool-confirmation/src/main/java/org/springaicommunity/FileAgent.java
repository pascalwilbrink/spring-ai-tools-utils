package org.springaicommunity;

import org.springaicommunity.tool.callback.ToolCallbacksFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;


@Service
public class FileAgent {

    private final ChatClient chatClient;
    private final ToolCallback[] tools;

    public FileAgent(
        ChatClient.Builder builder,
        FileTools fileTools,
        ToolCallbacksFactory toolCallbacksFactory
    ) {

        this.tools = toolCallbacksFactory.from(fileTools);

        this.chatClient = builder
                .defaultSystem("""
                You are a helpful file management assistant.
                Always tell the user what you are about to do before doing it.
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
