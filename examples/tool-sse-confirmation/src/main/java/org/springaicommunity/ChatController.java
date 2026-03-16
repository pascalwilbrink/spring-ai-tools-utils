package org.springaicommunity;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller that exposes the AI agent as a simple chat endpoint.
 *
 * <p>Send a {@code POST /chat} request with a JSON body like:
 * <pre>{@code { "message": "Place an order for 3 laptops" }}</pre>
 * The response is the agent's text reply. If the tool call requires confirmation,
 * the request will block until the user approves or rejects via the SSE flow.
 */
@RestController
class ChatController {

    private final OrderAgent agent;

    ChatController(OrderAgent agent) {
        this.agent = agent;
    }

    @PostMapping("/chat")
    Map<String, String> chat(@RequestBody ChatMessage message) {
        String response = agent.run(message.message());
        return Map.of("response", response);
    }
}
