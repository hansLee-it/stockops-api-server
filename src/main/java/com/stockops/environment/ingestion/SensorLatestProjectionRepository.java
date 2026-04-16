package com.stockops.environment.ingestion;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for latest sensor state projections.
 *
 * @author StockOps Team
 * @since 1.0
 */
public interface SensorLatestProjectionRepository extends JpaRepository<SensorLatestProjection, Long> {

    /**
     * Finds the latest projection by sensor device id.
     *
     * @param sensorDeviceId sensor device id
     * @return latest projection when present
     */
    Optional<SensorLatestProjection> findBySensorDeviceId(Long sensorDeviceId);

    /**
     * Updates the latest projection only when the incoming event is newer.
     * Older sequences are rejected; equal sequences require a newer timestamp.
     *
     * @param sensorDeviceId sensor device id
     * @param value latest value
     * @param valueKind value kind
     * @param unit unit
     * @param status status
     * @param recordedAt event timestamp
     * @param sequenceId event sequence id
     * @param updatedAt projection update timestamp
     * @return number of updated rows
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SensorLatestProjection projection
               set projection.value = :value,
                   projection.valueKind = :valueKind,
                   projection.unit = :unit,
                   projection.status = :status,
                   projection.recordedAt = :recordedAt,
                   projection.sequenceId = :sequenceId,
                   projection.updatedAt = :updatedAt
             where projection.sensorDeviceId = :sensorDeviceId
               and (
                    projection.sequenceId is null
                    or :sequenceId > projection.sequenceId
                    or (:sequenceId = projection.sequenceId and :recordedAt > projection.recordedAt)
               )
            """)
    int updateIfNewer(
            @Param("sensorDeviceId") Long sensorDeviceId,
            @Param("value") Double value,
            @Param("valueKind") String valueKind,
            @Param("unit") String unit,
            @Param("status") String status,
            @Param("recordedAt") Instant recordedAt,
            @Param("sequenceId") Long sequenceId,
            @Param("updatedAt") Instant updatedAt);
}
