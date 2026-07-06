package com.stockops.inventory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.stockops.entity.Location;
import com.stockops.repository.LocationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes stock-change events to WebSocket subscribers via the STOMP broker.
 * Messages are sent after a successful database commit so callers always invoke
 * this <b>after</b> the transactional service method returns.
 *
 * <p>Every event is sent to the firehose topic {@code /topic/stock} (subscribable only by
 * global-scope users) and, when the affected location resolves to a warehouse, also to the
 * per-warehouse topic {@code /topic/stock/{warehouseId}}. Scoped users may only subscribe to
 * warehouses within their scope (both rules enforced by {@code WebSocketAuthInterceptor}).
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
    private static final String TOPIC_STOCK_PREFIX = "/topic/stock/";
    private static final String EVENT_TYPE_STOCK_CHANGE = "STOCK_CHANGE";

    private final SimpMessagingTemplate simpMessagingTemplate;
    private final LocationRepository locationRepository;

    /**
     * Broadcasts a stock-change event to the global firehose topic and to the affected
     * warehouse's topic.
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

        final Long warehouseId = resolveWarehouseId(locationId);
        if (warehouseId != null) {
            simpMessagingTemplate.convertAndSend(TOPIC_STOCK_PREFIX + warehouseId, payload);
        }
    }

    private Long resolveWarehouseId(final Long locationId) {
        if (locationId == null) {
            return null;
        }
        return locationRepository.findById(locationId)
                .map(Location::getWarehouse)
                .map(warehouse -> warehouse == null ? null : warehouse.getId())
                .orElse(null);
    }

    public WebSocketStockPublisher(final SimpMessagingTemplate simpMessagingTemplate,
                                   final LocationRepository locationRepository) {
        this.simpMessagingTemplate = simpMessagingTemplate;
        this.locationRepository = locationRepository;
    }

}
