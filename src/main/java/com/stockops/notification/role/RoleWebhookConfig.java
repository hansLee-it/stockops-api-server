package com.stockops.notification.role;

import com.stockops.entity.BaseEntity;
import com.stockops.notification.webhook.WebhookEndpointConfig.WebhookProviderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Maps an application role to a webhook channel (e.g. a Teams channel) that receives
 * notices/announcements routed to that role.
 *
 * @author StockOps Team
 * @since 2.3
 */
@Entity
@Table(name = "role_webhook_configs")
public class RoleWebhookConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Role name that receives notices on this channel (e.g. ADMIN, CENTER_MANAGER). */
    @Column(name = "role", nullable = false, length = 100)
    private String role;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 20)
    private WebhookProviderType providerType = WebhookProviderType.TEAMS;

    @Column(name = "webhook_url", nullable = false, length = 2048)
    private String webhookUrl;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    public RoleWebhookConfig() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getRole() {
        return this.role;
    }

    public void setRole(final String role) {
        this.role = role;
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
}
