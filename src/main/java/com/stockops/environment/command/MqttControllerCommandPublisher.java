package com.stockops.environment.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockops.integration.sensimul.SensimulTopics;
import java.util.UUID;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Paho MQTTv5 implementation of {@link ControllerCommandPublisher}. Connects lazily and publishes
 * the command JSON to the controller command topic. Works against Mosquitto (on-prem) and the AWS
 * IoT Core MQTT endpoint alike (broker URL/TLS configured via env).
 *
 * <p>Only created when {@code stockops.command-messaging.enabled=true}.
 *
 * @author StockOps Team
 * @since 2.6
 */
@Component
@ConditionalOnProperty(name = "stockops.command-messaging.enabled", havingValue = "true")
public class MqttControllerCommandPublisher implements ControllerCommandPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttControllerCommandPublisher.class);

    private final CommandMessagingProperties properties;
    private final ObjectMapper objectMapper;
    private volatile MqttClient client;

    public MqttControllerCommandPublisher(final CommandMessagingProperties properties,
                                          final ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(final ControllerCommandMessage message) {
        try {
            final String topic = SensimulTopics.controllerCommand(message.siteId(), message.controllerId());
            final byte[] payload = objectMapper.writeValueAsBytes(message);
            final MqttMessage mqttMessage = new MqttMessage(payload);
            mqttMessage.setQos(properties.getQos());
            client().publish(topic, mqttMessage);
            LOGGER.debug("Published controller command correlationId={} to {}", message.correlationId(), topic);
        } catch (final Exception exception) {
            throw new IllegalStateException("Failed to publish controller command: " + exception.getMessage(),
                    exception);
        }
    }

    private MqttClient client() throws Exception {
        MqttClient local = this.client;
        if (local != null && local.isConnected()) {
            return local;
        }
        synchronized (this) {
            if (this.client == null) {
                final String clientId = properties.getClientId() != null && !properties.getClientId().isBlank()
                        ? properties.getClientId()
                        : "stockops-command-pub-" + UUID.randomUUID();
                this.client = new MqttClient(properties.getBrokerUrl(), clientId, new MemoryPersistence());
            }
            if (!this.client.isConnected()) {
                final MqttConnectionOptions options = new MqttConnectionOptions();
                options.setAutomaticReconnect(true);
                options.setCleanStart(true);
                this.client.connect(options);
            }
            return this.client;
        }
    }
}
