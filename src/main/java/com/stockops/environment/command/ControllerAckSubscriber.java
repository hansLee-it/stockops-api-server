package com.stockops.environment.command;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockops.integration.sensimul.SensimulTopics;
import jakarta.annotation.PreDestroy;
import java.util.UUID;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Subscribes to controller-command ACK topics and finalizes command status via
 * {@link ControllerCommandAckService}. ACK handling is idempotent, so it is safe for multiple
 * load-balanced instances to receive the same ACK. MQTT failures are logged (degraded mode).
 *
 * <p>Only created when {@code stockops.command-messaging.enabled=true}.
 *
 * @author StockOps Team
 * @since 2.6
 */
@Component
@ConditionalOnProperty(name = "stockops.command-messaging.enabled", havingValue = "true")
public class ControllerAckSubscriber implements SmartInitializingSingleton {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerAckSubscriber.class);

    private final CommandMessagingProperties properties;
    private final ControllerCommandAckService ackService;
    private final ObjectMapper objectMapper;

    private MqttClient mqttClient;

    public ControllerAckSubscriber(final CommandMessagingProperties properties,
                                   final ControllerCommandAckService ackService,
                                   final ObjectMapper objectMapper) {
        this.properties = properties;
        this.ackService = ackService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterSingletonsInstantiated() {
        final String filter = SensimulTopics.controllerAckFilter();
        try {
            final String clientId = properties.getClientId() != null && !properties.getClientId().isBlank()
                    ? properties.getClientId() + "-ack"
                    : "stockops-command-ack-" + UUID.randomUUID();
            mqttClient = new MqttClient(properties.getBrokerUrl(), clientId, new MemoryPersistence());
            final MqttConnectionOptions options = new MqttConnectionOptions();
            options.setServerURIs(new String[]{properties.getBrokerUrl()});
            options.setAutomaticReconnect(true);
            options.setCleanStart(false);
            mqttClient.setCallback(createCallback());
            mqttClient.connect(options);
            mqttClient.subscribe(filter, properties.getQos());
            LOGGER.info("Subscribed to controller command ACKs filter={} broker={}",
                    filter, properties.getBrokerUrl());
        } catch (final Exception exception) {
            LOGGER.warn("MQTT broker unavailable - controller ACK consumption degraded: {}",
                    exception.getMessage());
        }
    }

    private MqttCallback createCallback() {
        return new MqttCallback() {
            @Override
            public void messageArrived(final String topic, final MqttMessage message) {
                try {
                    final ControllerCommandAck ack =
                            objectMapper.readValue(message.getPayload(), ControllerCommandAck.class);
                    ackService.applyAck(ack);
                } catch (final Exception exception) {
                    LOGGER.warn("Failed to process controller ACK on topic={}: {}", topic, exception.getMessage());
                }
            }

            @Override
            public void disconnected(final MqttDisconnectResponse disconnectResponse) {
                LOGGER.warn("Controller ACK subscriber disconnected: {}", disconnectResponse);
            }

            @Override
            public void mqttErrorOccurred(final MqttException exception) {
                LOGGER.warn("Controller ACK subscriber MQTT error: {}", exception.getMessage());
            }

            @Override
            public void deliveryComplete(final org.eclipse.paho.mqttv5.client.IMqttToken token) {
                // no-op: this client only subscribes
            }

            @Override
            public void connectComplete(final boolean reconnect, final String serverUri) {
                if (reconnect) {
                    try {
                        mqttClient.subscribe(SensimulTopics.controllerAckFilter(), properties.getQos());
                    } catch (final MqttException exception) {
                        LOGGER.warn("Failed to re-subscribe to ACK filter after reconnect: {}",
                                exception.getMessage());
                    }
                }
            }

            @Override
            public void authPacketArrived(final int reasonCode, final MqttProperties mqttProperties) {
                // no-op
            }
        };
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
        } catch (final MqttException exception) {
            LOGGER.warn("Error closing ACK subscriber MQTT client: {}", exception.getMessage());
        }
    }
}
