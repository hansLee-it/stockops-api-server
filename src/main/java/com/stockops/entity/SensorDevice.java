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

    /** Warehouse this sensor is installed in (FK to warehouses). Nullable for legacy rows
     *  registered before warehouse association existed; required for new registrations. */
    @Column(name = "warehouse_id")
    private Long warehouseId;

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

    @Column(name = "warn_min")
    private Double warnMin;

    @Column(name = "warn_max")
    private Double warnMax;

    @Column(name = "crit_min")
    private Double critMin;

    @Column(name = "crit_max")
    private Double critMax;

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

    public Long getWarehouseId() {
        return this.warehouseId;
    }

    public void setWarehouseId(final Long warehouseId) {
        this.warehouseId = warehouseId;
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

    public Double getWarnMin() {
        return this.warnMin;
    }

    public void setWarnMin(final Double warnMin) {
        this.warnMin = warnMin;
    }

    public Double getWarnMax() {
        return this.warnMax;
    }

    public void setWarnMax(final Double warnMax) {
        this.warnMax = warnMax;
    }

    public Double getCritMin() {
        return this.critMin;
    }

    public void setCritMin(final Double critMin) {
        this.critMin = critMin;
    }

    public Double getCritMax() {
        return this.critMax;
    }

    public void setCritMax(final Double critMax) {
        this.critMax = critMax;
    }

    /**
     * Returns whether any threshold bound is configured, which switches alert
     * evaluation from payload-status trust to server-side value comparison.
     *
     * @return true when at least one bound is set
     */
    public boolean hasThresholds() {
        return warnMin != null || warnMax != null || critMin != null || critMax != null;
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
