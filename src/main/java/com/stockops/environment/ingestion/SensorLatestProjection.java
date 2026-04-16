package com.stockops.environment.ingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Latest-value projection for an environment sensor.
 * Separates append-only history from the current operational view.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Getter
@Setter
@Entity
@Table(name = "sensor_latest")
@NoArgsConstructor
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
}
