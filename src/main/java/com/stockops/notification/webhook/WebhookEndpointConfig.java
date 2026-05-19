package com.stockops.notification.webhook;

import com.stockops.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Persistent configuration for a webhook endpoint.
 * Stores the target URL, provider type, and optional extra config (JSON)
 * per center/warehouse scope.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "webhook_endpoint_config")
public class WebhookEndpointConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "center_id")
    private Long centerId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 20)
    private WebhookProviderType providerType;

    @Column(name = "webhook_url", nullable = false, length = 2048)
    private String webhookUrl;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /**
     * JSON string for provider-specific configuration such as
     * custom headers, secrets, or template overrides.
     * Stored as TEXT to accommodate complex configurations.
     */
    @Column(name = "extra_config", columnDefinition = "TEXT")
    private String extraConfig;

    /**
     * Supported webhook provider types.
     */
    public enum WebhookProviderType {
        SLACK,
        NOTION,
        DISCORD,
        TEAMS,
        GENERIC
    }

    public WebhookEndpointConfig() {
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

    public WebhookProviderType getProviderType() {
        return this.providerType;
    }

    public void setProviderType(final WebhookProviderType providerType) {
        this.providerType = providerType;
    }

    public String getWebhookUrl() {
        return this.webhookUrl;
    }

    public void setWebhookUrl(final String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public String getExtraConfig() {
        return this.extraConfig;
    }

    public void setExtraConfig(final String extraConfig) {
        this.extraConfig = extraConfig;
    }
}