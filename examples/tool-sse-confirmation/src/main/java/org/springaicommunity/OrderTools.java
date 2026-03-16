package org.springaicommunity;

import org.springaicommunity.tool.confirmation.annotation.ConfirmableTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example tools representing order-management operations.
 * Destructive operations are gated behind {@code @ConfirmableTool} so that a
 * human must approve them before the AI can proceed.
 */
@Component
public class OrderTools {

    private List<Map<String, String>> orders = new ArrayList<>();

    public OrderTools() {
        this.orders.add(
            Map.of(
                "id",
                "ORD-001",
                "product",
                "Laptop",
                "qty",
                "2",
                "status",
                "pending"
            )
        );
        this.orders.add(
            Map.of(
                "id",
                "ORD-002",
                "product",
                "Monitor",
                "qty",
                "1",
                "status",
                "pending"
            )
        );
    }
    @Tool(description = "List all pending orders")
    public List<Map<String, String>> listOrders() {
        return orders;
    }

    @Tool(description = "Place a new order for a product")
    @ConfirmableTool(
        handler  = SseConfirmationHandler.class,
        reason   = "Placing an order will charge the customer's account."
    )
    public String placeOrder(String product, int quantity) {
        // In a real app this would call an order-management system
        String orderId = "ORD-" + System.currentTimeMillis();
        orders.add(Map.of(
            "id",
            orderId,
            "product",
            product,
            "qty",
            String.valueOf(quantity),
            "status",
            "pending"
        ));
        return "Order %s placed: %d × %s".formatted(orderId, quantity, product);
    }

    @Tool(description = "Cancel an existing order by its ID")
    @ConfirmableTool(
        handler = SseConfirmationHandler.class,
        reason  = "Cancelling an order cannot be undone."
    )
    public String cancelOrder(String orderId) {
        this.orders = this.orders.stream().filter((o) -> !o.getOrDefault("id", "").equals(orderId)).toList();

        return "Order %s has been cancelled.".formatted(orderId);
    }
}
