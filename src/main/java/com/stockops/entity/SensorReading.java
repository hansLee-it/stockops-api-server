package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Append-only environment sensor reading entity.
 * Stores normalized values and raw payload snapshots for historical analysis.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "sensor_readings")
public class SensorReading extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_device_id", nullable = false)
    private Long sensorDeviceId;

    @Column(name = "value", nullable = false)
    private Double value;

    @Column(name = "value_kind", nullable = false, length = 50)
    private String valueKind;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "sequence_id")
    private Long sequenceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    public SensorReading() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getSensorDeviceId() {
        return this.sensorDeviceId;
    }

    public void setSensorDeviceId(final Long sensorDeviceId) {
        this.sensorDeviceId = sensorDeviceId;
    }

    public Double getValue() {
        return this.value;
    }

    public void setValue(final Double value) {
        this.value = value;
    }

    public String getValueKind() {
        return this.valueKind;
    }

    public void setValueKind(final String valueKind) {
        this.valueKind = valueKind;
    }

    public String getUnit() {
        return this.unit;
    }

    public void setUnit(final String unit) {
        this.unit = unit;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public Instant getRecordedAt() {
        return this.recordedAt;
    }

    public void setRecordedAt(final Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public Long getSequenceId() {
        return this.sequenceId;
    }

    public void setSequenceId(final Long sequenceId) {
        this.sequenceId = sequenceId;
    }

    public String getRawPayload() {
        return this.rawPayload;
    }

    public void setRawPayload(final String rawPayload) {
        this.rawPayload = rawPayload;
    }
}
