package com.stockops.notification.config;

import com.stockops.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Configuration for which notification channels are used per alert type
 * per center/warehouse scope.
 *
 * <p>Each config row defines the enabled channels (SMS, EMAIL, WEBHOOK)
 * and optional webhook provider selection for a specific alert type
 * within a center or warehouse.</p>
 *
 * @author StockOps Team
 * @since 2.0
 * @see NotificationChannelConfigService
 */
@Entity
@Table(name = "notification_channel_configs", uniqueConstraints = @UniqueConstraint(
        name = "uk_channel_config_scope",
        columnNames = {"center_id", "warehouse_id", "alert_type"}))
public class NotificationChannelConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Center this config belongs to. */
    @Column(name = "center_id", nullable = false)
    private Long centerId;

    /** Warehouse this config belongs to (null = center-level). */
    @Column(name = "warehouse_id")
    private Long warehouseId;

    /** Alert type (e.g. TEMPERATURE, HUMIDITY, AIR_QUALITY). */
    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType;

    /**
     * JSON array of channel configurations.
     * Each entry: {"type": "SMS"|"EMAIL"|"WEBHOOK", "enabled": true/false, "webhookProvider": "SLACK"|...}
     * Stored as JSONB for flexible schema.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "channels", nullable = false, columnDefinition = "jsonb DEFAULT '[]'")
    private List<ChannelEntry> channels = List.of();

    /** Whether this config is active. */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Single channel entry within a NotificationChannelConfig.
     * Represents one notification channel (SMS, EMAIL, or WEBHOOK)
     * with its enabled status and optional webhook provider.
     *
     * @author StockOps Team
     * @since 2.0
     */
    public static class ChannelEntry {
        /** Channel type: SMS, EMAIL, or WEBHOOK. */
        @Enumerated(EnumType.STRING)
        private ChannelType type;

        /** Whether this channel is enabled for the alert type. */
        private boolean enabled;

        /** Webhook provider type (required when type is WEBHOOK). */
        private String webhookProvider;

        public ChannelEntry(final ChannelType type, final boolean enabled, final String webhookProvider) {
            this.type = type;
            this.enabled = enabled;
            this.webhookProvider = webhookProvider;
        }
    
        public ChannelType getType() {
            return this.type;
        }

        public void setType(final ChannelType type) {
            this.type = type;
        }

        public boolean isEnabled() {
            return this.enabled;
        }

        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }

        public String getWebhookProvider() {
            return this.webhookProvider;
        }

        public void setWebhookProvider(final String webhookProvider) {
            this.webhookProvider = webhookProvider;
        }
}

    /**
     * Supported notification channel types.
     */
    public enum ChannelType {
        SMS,
        EMAIL,
        WEBHOOK
    }

    public NotificationChannelConfig() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getCenterId() {
        return this.centerId;
    }

    public void setCenterId(final Long centerId) {
        this.centerId = centerId;
    }

    public Long getWarehouseId() {
        return this.warehouseId;
    }

    public void setWarehouseId(final Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getAlertType() {
        return this.alertType;
    }

    public void setAlertType(final String alertType) {
        this.alertType = alertType;
    }

    public List<ChannelEntry> getChannels() {
        return this.channels;
    }

    public void setChannels(final List<ChannelEntry> channels) {
        this.channels = channels;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }
}