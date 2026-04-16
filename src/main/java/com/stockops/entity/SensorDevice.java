package com.stockops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
@Data
@Entity
@Table(name = "sensor_devices")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
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
}
