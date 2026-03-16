package org.springaicommunity;

import org.springaicommunity.tool.fallback.annotation.FallbackTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class DatabaseTools {

    // fallback to method with cause
    @Tool(description = "Execute a SQL query and return the results")
    @FallbackTool(method = "queryFailed")
    String executeQuery(String sql) {
        throw new RuntimeException("Database connection refused"); // simulated failure
    }

    String queryFailed(String sql, Throwable cause) {
        return "Query '%s' could not be executed: %s".formatted(sql, cause.getMessage());
    }


    // fallback to static message
    @Tool(description = "Get the current database status")
    @FallbackTool(message = "Database status is currently unavailable.")
    String getDatabaseStatus() {
        throw new RuntimeException("Status endpoint is down"); // simulated failure
    }


    // fallback without cause parameter
    @Tool(description = "List all available tables")
    @FallbackTool(method = "tablesFailed")
    String listTables() {
        throw new RuntimeException("Could not connect"); // simulated failure
    }

    String tablesFailed() {
        return "Table listing is currently unavailable. Please try again later.";
    }
}
