package org.springaicommunity.tool.ratelimit.exception;

/**
 * Thrown when a {@link org.springaicommunity.tool.ratelimit.callback.RateLimitedToolCallback}
 * rejects a tool invocation because the configured request rate has been exceeded.
 */
public class RateLimitExceededException extends RuntimeException {

    private final String toolName;
    private final String toolInput;

    /**
     * Constructs a new exception for the given tool and input.
     *
     * @param toolName  the name of the rate-limited tool
     * @param toolInput the raw input that triggered the rejection
     */
    public RateLimitExceededException(String toolName, String toolInput) {
        super("Rate limit exceeded for tool '%s'".formatted(toolName));
        this.toolName = toolName;
        this.toolInput = toolInput;
    }

    /**
     * Returns the name of the tool whose rate limit was exceeded.
     *
     * @return the tool name
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Returns the raw tool input that triggered the rate limit.
     *
     * @return the raw input string
     */
    public String getToolInput() {
        return toolInput;
    }
}
