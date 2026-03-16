package org.springaicommunity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SSE-based tool-confirmation example.
 *
 * <h2>How it works</h2>
 * <ol>
 *   <li>Open <a href="http://localhost:8080">http://localhost:8080</a> in a browser.
 *       The page connects to the SSE stream automatically.</li>
 *   <li>Send a chat message via the UI or with curl:
 *       <pre>curl -s -X POST http://localhost:8080/chat \
 *   -H 'Content-Type: application/json' \
 *   -d '{"message":"Place an order for 2 laptops"}'</pre></li>
 *   <li>A confirmation card appears in the browser. Click <b>Approve</b> or <b>Reject</b>.</li>
 *   <li>The blocked tool-call thread is unblocked and the agent continues.</li>
 * </ol>
 *
 * <h2>Approve / reject via RestClient (programmatic)</h2>
 * <pre>{@code
 * RestClient client = RestClient.create("http://localhost:8080");
 *
 * // approve
 * client.post()
 *       .uri("/confirmations/{id}/approve", confirmationId)
 *       .retrieve()
 *       .toBodilessEntity();
 *
 * // reject
 * client.post()
 *       .uri("/confirmations/{id}/reject", confirmationId)
 *       .contentType(MediaType.APPLICATION_JSON)
 *       .body(Map.of("reason", "Budget not approved"))
 *       .retrieve()
 *       .toBodilessEntity();
 * }</pre>
 */
@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
