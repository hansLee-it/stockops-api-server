package com.stockops.notification.escalation;

import com.stockops.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * Escalation rule belonging to a policy.
 * Each rule defines a notification level with delay, target roles, and channels.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Entity
@Table(name = "escalation_rules", uniqueConstraints = @UniqueConstraint(
        name = "uk_escalation_rules_policy_level",
        columnNames = {"policy_id", "level"}))
public class EscalationRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private EscalationPolicy policy;

    @Column(name = "level", nullable = false)
    private Integer level;

    @Column(name = "delay_minutes", nullable = false)
    private Integer delayMinutes = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "notify_roles", nullable = false, columnDefinition = "jsonb DEFAULT '[]'")
    private List<String> notifyRoles = List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "channels", nullable = false, columnDefinition = "jsonb DEFAULT '[\"EMAIL\"]'")
    private List<String> channels = List.of();

    public EscalationRule() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public EscalationPolicy getPolicy() {
        return this.policy;
    }

    public void setPolicy(final EscalationPolicy policy) {
        this.policy = policy;
    }

    public Integer getLevel() {
        return this.level;
    }

    public void setLevel(final Integer level) {
        this.level = level;
    }

    public Integer getDelayMinutes() {
        return this.delayMinutes;
    }

    public void setDelayMinutes(final Integer delayMinutes) {
        this.delayMinutes = delayMinutes;
    }

    public List<String> getNotifyRoles() {
        return this.notifyRoles;
    }

    public void setNotifyRoles(final List<String> notifyRoles) {
        this.notifyRoles = notifyRoles;
    }

    public List<String> getChannels() {
        return this.channels;
    }

    public void setChannels(final List<String> channels) {
        this.channels = channels;
    }
}