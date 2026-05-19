package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

/**
 * Environment monitoring sensor device master entity.
 * Soft-deleted devices remain available for historical logs but stay hidden from active queries.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "sensor_devices")
@SQLRestriction("deleted = false")
public class SensorDevice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "location", nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false, length = 100)
    private SensorType sensorType;

    @Column(name = "external_sensor_id", nullable = false)
    private String externalSensorId;

    @Column(name = "mqtt_topic", length = 500)
    private String mqttTopic;

    @Column(name = "source_channel", length = 100)
    private String sourceChannel;

    @Column(name = "unit", length = 50)
    private String unit;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "calibration", columnDefinition = "jsonb")
    private String calibration;

    @Column(name = "noise_sigma")
    private Double noiseSigma;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public SensorDevice() {
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

    public String getLocation() {
        return this.location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public SensorType getSensorType() {
        return this.sensorType;
    }

    public void setSensorType(final SensorType sensorType) {
        this.sensorType = sensorType;
    }

    public String getExternalSensorId() {
        return this.externalSensorId;
    }

    public void setExternalSensorId(final String externalSensorId) {
        this.externalSensorId = externalSensorId;
    }

    public String getMqttTopic() {
        return this.mqttTopic;
    }

    public void setMqttTopic(final String mqttTopic) {
        this.mqttTopic = mqttTopic;
    }

    public String getSourceChannel() {
        return this.sourceChannel;
    }

    public void setSourceChannel(final String sourceChannel) {
        this.sourceChannel = sourceChannel;
    }

    public String getUnit() {
        return this.unit;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public String getCalibration() {
        return this.calibration;
    }

    public void setCalibration(final String calibration) {
        this.calibration = calibration;
    }

    public Double getNoiseSigma() {
        return this.noiseSigma;
    }

    public void setNoiseSigma(final Double noiseSigma) {
        this.noiseSigma = noiseSigma;
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
