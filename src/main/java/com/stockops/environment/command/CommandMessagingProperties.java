package com.stockops.environment.command;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for controller-command messaging.
 *
 * <p>When {@code enabled} is false (default), {@code ControllerCommandService} keeps the legacy
 * synchronous HTTP path to Sensimul. When true, commands are published to the MQTT broker
 * (Mosquitto on-prem, AWS IoT Core endpoint in cloud — both speak MQTT) and Sensimul echoes an
 * ACK on the ack topic, which an MQTT subscriber consumes to finalize the command status.
 *
 * @author StockOps Team
 * @since 2.6
 */
@ConfigurationProperties(prefix = "stockops.command-messaging")
public class CommandMessagingProperties {

    private boolean enabled = false;
    private String brokerUrl = "tcp://localhost:1883";
    private int qos = 1;
    private String clientId;
    private Duration ackTimeout = Duration.ofSeconds(30);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void setBrokerUrl(final String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(final int qos) {
        this.qos = qos;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public Duration getAckTimeout() {
        return ackTimeout;
    }

    public void setAckTimeout(final Duration ackTimeout) {
        this.ackTimeout = ackTimeout;
    }
}
