package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Append-only environment sensor reading entity.
 * Stores normalized values and raw payload snapshots for historical analysis.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Data
@Entity
@Table(name = "sensor_readings")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
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
}
