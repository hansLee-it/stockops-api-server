package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

/**
 * Environment controller master entity.
 * Soft-deleted controllers remain preserved for command history while active queries exclude them.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "environment_controllers")
@SQLRestriction("deleted = false")
public class EnvironmentController extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "external_controller_id", nullable = false)
    private String externalControllerId;

    @Column(name = "mqtt_topic")
    private String mqttTopic;

    @Enumerated(EnumType.STRING)
    @Column(name = "controller_type", nullable = false, length = 50)
    private ControllerType controllerType = ControllerType.VENTILATION;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_axis", nullable = false, length = 50)
    private EnvironmentAxis targetAxis;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ControllerStatus status = ControllerStatus.INACTIVE;

    @Column(name = "output_level", nullable = false)
    private Integer outputLevel = 0;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public EnvironmentController() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getExternalControllerId() {
        return this.externalControllerId;
    }

    public void setExternalControllerId(final String externalControllerId) {
        this.externalControllerId = externalControllerId;
    }

    public String getMqttTopic() {
        return this.mqttTopic;
    }

    public void setMqttTopic(final String mqttTopic) {
        this.mqttTopic = mqttTopic;
    }

    public ControllerType getControllerType() {
        return this.controllerType;
    }

    public void setControllerType(final ControllerType controllerType) {
        this.controllerType = controllerType;
    }

    public EnvironmentAxis getTargetAxis() {
        return this.targetAxis;
    }

    public void setTargetAxis(final EnvironmentAxis targetAxis) {
        this.targetAxis = targetAxis;
    }

    public ControllerStatus getStatus() {
        return this.status;
    }

    public void setStatus(final ControllerStatus status) {
        this.status = status;
    }

    public Integer getOutputLevel() {
        return this.outputLevel;
    }

    public void setOutputLevel(final Integer outputLevel) {
        this.outputLevel = outputLevel;
    }

    public boolean isDeleted() {
        return this.deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }
}
