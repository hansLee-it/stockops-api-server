package com.stockops.inventory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes stock-change events to WebSocket subscribers via the STOMP broker.
 * Messages are sent after a successful database commit so callers always invoke
 * this <b>after</b> the transactional service method returns.
 *
 * <p>The topic destination is {@code /topic/stock}. Clients subscribe to receive
 * real-time notifications for inbound, outbound, and adjustment mutations.
 *
 * <p>Payload schema:
 * <pre>
 * {
 *   "eventType": "STOCK_CHANGE",
 *   "changeType": "INBOUND" | "OUTBOUND" | "ADJUSTMENT",
 *   "productId": Long,
 *   "locationId": Long,
 *   "quantity": int,
 *   "newTotal": int,
 *   "timestamp": ISO-8601 UTC
 * }
 * </pre>
 *
 * @author StockOps Team
 * @since 1.0
 * @see SimpMessagingTemplate
 * @see com.stockops.config.WebSocketConfig
 */
@Service
public class WebSocketStockPublisher {

    private static final String TOPIC_STOCK = "/topic/stock";
    private static final String EVENT_TYPE_STOCK_CHANGE = "STOCK_CHANGE";

    private final SimpMessagingTemplate simpMessagingTemplate;

    /**
     * Broadcasts a stock-change event to all subscribers of {@code /topic/stock}.
     *
     * @param changeType source of the change: {@code INBOUND}, {@code OUTBOUND}, or {@code ADJUSTMENT}
     * @param productId product that was mutated
     * @param locationId location where the change occurred
     * @param quantity absolute quantity of the change (always positive; direction implied by changeType)
     * @param newTotal inventory balance after the change
     */
    public void publishStockChange(final String changeType,
                                  final Long productId,
                                  final Long locationId,
                                  final int quantity,
                                  final int newTotal) {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", EVENT_TYPE_STOCK_CHANGE);
        payload.put("changeType", changeType);
        payload.put("productId", productId);
        payload.put("locationId", locationId);
        payload.put("quantity", quantity);
        payload.put("newTotal", newTotal);
        payload.put("timestamp", Instant.now().toString());

        simpMessagingTemplate.convertAndSend(TOPIC_STOCK, payload);
    }

    public WebSocketStockPublisher(final SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

}