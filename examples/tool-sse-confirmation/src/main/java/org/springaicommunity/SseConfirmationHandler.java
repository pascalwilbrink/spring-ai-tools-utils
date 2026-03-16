package org.springaicommunity;

import org.springaicommunity.tool.confirmation.AbstractConfirmationHandler;
import org.springaicommunity.tool.confirmation.ConfirmationRequest;
import org.springaicommunity.tool.confirmation.properties.ConfirmationProperties;
import org.springaicommunity.tool.confirmation.store.ConfirmationStore;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Confirmation handler that pushes pending confirmations to browser clients
 * via Server-Sent Events. Each confirmation appears as an SSE event on the
 * {@code /confirmations/stream} endpoint; clients approve or reject by calling
 * the REST endpoints exposed by {@link ConfirmationController}.
 */
@Component
public class SseConfirmationHandler extends AbstractConfirmationHandler {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseConfirmationHandler(ConfirmationStore store, ConfirmationProperties props) {
        super(store, props);
    }

    /**
     * Registers an SSE emitter so it receives future confirmation events.
     * The emitter is automatically removed when the client disconnects.
     */
    public void addEmitter(SseEmitter emitter) {
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));
    }

    @Override
    protected void onPendingConfirmation(String confirmationId, ConfirmationRequest request) {
        SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name("confirmation")
                .data(Map.of(
                        "id",     confirmationId,
                        "tool",   request.toolDefinition().name(),
                        "input",  request.toolInput(),
                        "reason", request.reason()
                ));

        List<SseEmitter> dead = new java.util.ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(event);
            } catch (IOException e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }
}
