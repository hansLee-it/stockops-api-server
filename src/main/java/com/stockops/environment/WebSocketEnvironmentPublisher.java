package com.stockops.environment;

import com.stockops.entity.SensorDevice;
import com.stockops.entity.SensorReading;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes environment sensor readings to WebSocket subscribers.
 * Broadcasts JSON payload on /topic/environment after a SensorReading is persisted.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
public class WebSocketEnvironmentPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketEnvironmentPublisher.class);
    private static final String DESTINATION = "/topic/environment";

    private final SimpMessagingTemplate simpMessagingTemplate;

    /**
     * Creates the publisher.
     *
     * @param simpMessagingTemplate STOMP messaging template
     */
    public WebSocketEnvironmentPublisher(final SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    /**
     * Broadcasts a sensor reading to /topic/environment.
     * Payload contains eventType, sensorId, sensorType, value, locationId, timestamp.
     * This method does NOT persist anything — persistence happens in the caller.
     *
     * @param reading     the persisted sensor reading
     * @param sensorDevice the associated sensor device (used for sensorType and location)
     */
    public void publishSensorReading(final SensorReading reading, final SensorDevice sensorDevice) {
        final Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "ENV_SENSOR");
        payload.put("sensorId", sensorDevice.getExternalSensorId());
        payload.put("sensorType", sensorDevice.getSensorType().name());
        payload.put("value", reading.getValue());
        payload.put("locationId", sensorDevice.getLocation());
        payload.put("timestamp", reading.getRecordedAt().toString());

        simpMessagingTemplate.convertAndSend(DESTINATION, payload);
        LOGGER.debug("Broadcasted ENV_SENSOR for sensorId={}", sensorDevice.getExternalSensorId());
    }
}