package org.springaicommunity;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * REST controller exposing the SSE stream and approve/reject endpoints.
 *
 * <p>Typical flow:
 * <ol>
 *   <li>Client connects to {@code GET /confirmations/stream}.</li>
 *   <li>An AI tool annotated with {@code @ConfirmableTool} is invoked.</li>
 *   <li>A {@code confirmation} SSE event arrives on the stream with the confirmation id.</li>
 *   <li>Client calls {@code POST /confirmations/{id}/approve} or
 *       {@code POST /confirmations/{id}/reject} — the tool then proceeds or is rejected.</li>
 * </ol>
 */
@RestController
@RequestMapping("/confirmations")
class ConfirmationController {

    private final SseConfirmationHandler handler;

    ConfirmationController(SseConfirmationHandler handler) {
        this.handler = handler;
    }

    /**
     * Opens an SSE stream. Keep this connection alive; one {@code confirmation}
     * event is sent for every tool call that requires approval.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        handler.addEmitter(emitter);
        return emitter;
    }

    /**
     * Approves the pending confirmation identified by {@code id}.
     * The blocked tool-call thread is unblocked and the tool proceeds.
     */
    @PostMapping("/{id}/approve")
    ResponseEntity<Void> approve(@PathVariable String id) {
        handler.respond(id, true, null);
        return ResponseEntity.ok().build();
    }

    /**
     * Rejects the pending confirmation identified by {@code id}.
     * The tool-call thread receives a {@link org.springaicommunity.tool.confirmation.exception.ToolRejectionException}.
     *
     * @param body JSON body with a {@code reason} field explaining the rejection
     */
    @PostMapping("/{id}/reject")
    ResponseEntity<Void> reject(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        String reason = body.getOrDefault("reason", "Rejected by user");
        handler.respond(id, false, reason);
        return ResponseEntity.ok().build();
    }
}
