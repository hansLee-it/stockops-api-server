package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Append-only environment alert entity.
 * Captures sensor-driven and system-generated operational alerts with acknowledgement metadata.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Data
@Entity
@Table(name = "environment_alerts")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EnvironmentAlert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sensor_device_id")
    private Long sensorDeviceId;

    @Column(name = "alert_type", nullable = false, length = 100)
    private String alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private AlertSeverity severity = AlertSeverity.INFO;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "acknowledged", nullable = false)
    private boolean acknowledged;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "acknowledged_by")
    private String acknowledgedBy;
}
