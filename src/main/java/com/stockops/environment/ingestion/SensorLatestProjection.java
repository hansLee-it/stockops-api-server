package com.stockops.environment.ingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Latest-value projection for an environment sensor.
 * Separates append-only history from the current operational view.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Entity
@Table(name = "sensor_latest")
public class SensorLatestProjection {

    @Id
    @Column(name = "sensor_device_id", nullable = false)
    private Long sensorDeviceId;

    @Column(name = "value")
    private Double value;

    @Column(name = "value_kind", length = 50)
    private String valueKind;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "recorded_at")
    private Instant recordedAt;

    @Column(name = "sequence_id")
    private Long sequenceId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touchUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    public SensorLatestProjection() {
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

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
