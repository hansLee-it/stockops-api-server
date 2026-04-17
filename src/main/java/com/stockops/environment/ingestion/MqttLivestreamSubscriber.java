package com.stockops.environment.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

/**
 * Subscribes to Sensimul live MQTT telemetry after the Spring context is ready.
 * MQTT failures are logged so the application can keep running in degraded mode.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Component
public class MqttLivestreamSubscriber implements SmartInitializingSingleton {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttLivestreamSubscriber.class);

    private final MqttIngestionProperties properties;
    private final TelemetryIngestionService telemetryIngestionService;
    private final ObjectMapper objectMapper;
    private final AtomicLong errorCounter = new AtomicLong();

    private MqttClient mqttClient;

    /**
     * Creates the MQTT live subscriber.
     *
     * @param properties ingestion properties
     * @param telemetryIngestionService ingestion service
     * @param objectMapper jackson object mapper
     */
    public MqttLivestreamSubscriber(
            final MqttIngestionProperties properties,
            final TelemetryIngestionService telemetryIngestionService,
            final ObjectMapper objectMapper) {
        this.properties = properties;
        this.telemetryIngestionService = telemetryIngestionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Starts the MQTT subscription after all singletons are initialized.
     */
    @Override
    public void afterSingletonsInstantiated() {
        if (!properties.isEnabled()) {
            LOGGER.info("MQTT ingestion disabled by configuration");
            return;
        }

        try {
            mqttClient = new MqttClient(properties.getBrokerUrl(), resolveClientId(), new MemoryPersistence());

            final MqttConnectionOptions options = new MqttConnectionOptions();
            options.setServerURIs(new String[]{properties.getBrokerUrl()});
            options.setAutomaticReconnect(true);
            options.setCleanStart(false);

            mqttClient.setCallback(createCallback());

            mqttClient.connect(options);
            mqttClient.subscribe(SensimulTopics.liveSensorFilter(), properties.getQos());

            LOGGER.info(
                    "MQTT connection established broker={} topicFilter={} qos={}",
                    properties.getBrokerUrl(),
                    SensimulTopics.liveSensorFilter(),
                    properties.getQos());
            LOGGER.info(
                    "Subscribed to Sensimul live telemetry topic filter={} broker={} qos={}",
                    SensimulTopics.liveSensorFilter(),
                    properties.getBrokerUrl(),
                    properties.getQos());
        } catch (Exception exception) {
            LOGGER.warn(
                    "MQTT broker unavailable - telemetry ingestion running in degraded mode: {}",
                    exception.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        if (mqttClient == null) {
            return;
        }

        try {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
            mqttClient.close();
        } catch (MqttException exception) {
            LOGGER.debug("Failed to close MQTT client cleanly", exception);
        }
    }

    private MqttCallback createCallback() {
        return (MqttCallback) java.lang.reflect.Proxy.newProxyInstance(
                MqttCallback.class.getClassLoader(),
                new Class<?>[]{MqttCallback.class},
                (proxy, method, args) -> {
                    final String methodName = method.getName();
                    if ("messageArrived".equals(methodName) && args != null && args.length == 2) {
                        handleMessage((String) args[0], (MqttMessage) args[1]);
                        return null;
                    }
                    if (("connectionLost".equals(methodName) || "disconnected".equals(methodName))
                            && args != null
                            && args.length == 1) {
                        final Throwable cause = args[0] instanceof Throwable ? (Throwable) args[0] : null;
                        LOGGER.warn("Disconnected from MQTT broker={}", properties.getBrokerUrl(), cause);
                        return null;
                    }
                    if ("mqttErrorOccurred".equals(methodName) && args != null && args.length == 1 && args[0] instanceof Throwable) {
                        LOGGER.warn("MQTT client error while connected to broker={}", properties.getBrokerUrl(), (Throwable) args[0]);
                        return null;
                    }
                    return null;
                });
    }

    private void handleMessage(final String topic, final MqttMessage message) {
        try {
            final SensimulTopics.ParsedLiveTopic parsedTopic = SensimulTopics.parseLiveTopic(topic);
            if (!parsedTopic.valid()) {
                incrementErrorCounter();
                LOGGER.error("Received telemetry on malformed topic: {}", topic);
                return;
            }

            final SensimulPayload incomingPayload = objectMapper.readValue(message.getPayload(), SensimulPayload.class);
            final SensimulPayload normalizedPayload = new SensimulPayload(
                    parsedTopic.siteId(),
                    parsedTopic.sensorId(),
                    incomingPayload.sensorType(),
                    incomingPayload.valueKind(),
                    incomingPayload.value(),
                    incomingPayload.unit(),
                    incomingPayload.status(),
                    incomingPayload.timestamp(),
                    incomingPayload.sequenceId(),
                    incomingPayload.schemaVersion());

            telemetryIngestionService.ingest(normalizedPayload);
        } catch (Exception exception) {
            final long failures = errorCounter.incrementAndGet();
            LOGGER.error("Failed to process MQTT telemetry message from topic={} parseErrors={}", topic, failures, exception);
        }
    }

    private void incrementErrorCounter() {
        errorCounter.incrementAndGet();
    }

    private String resolveClientId() {
        if (properties.getClientId() != null && !properties.getClientId().isBlank()) {
            return properties.getClientId();
        }
        return "stockops-ingestion-" + UUID.randomUUID();
    }
}
