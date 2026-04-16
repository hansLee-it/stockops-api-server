package com.stockops.repository;

import com.stockops.entity.SensorReading;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for append-only sensor reading history.
 *
 * @author StockOps Team
 * @since 1.0
 */
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    List<SensorReading> findBySensorDeviceIdOrderByRecordedAtDesc(Long sensorDeviceId, Pageable pageable);

    Optional<SensorReading> findTopBySensorDeviceIdOrderByRecordedAtDesc(Long sensorDeviceId);

    Optional<SensorReading> findTopBySensorDeviceIdAndSequenceIdOrderByRecordedAtDesc(Long sensorDeviceId, Long sequenceId);

    Optional<SensorReading> findTopBySensorDeviceIdOrderBySequenceIdDescRecordedAtDesc(Long sensorDeviceId);

    /**
     * Returns time-series history for a sensor after the provided cutoff.
     *
     * @param sensorDeviceId sensor device id
     * @param cutoff UTC cutoff timestamp
     * @return readings sorted in chronological order
     */
    @Query("""
            SELECT r
              FROM SensorReading r
             WHERE r.sensorDeviceId = :sensorDeviceId
               AND r.recordedAt >= :cutoff
             ORDER BY r.recordedAt ASC
            """)
    List<SensorReading> findHistoryBySensorDeviceIdAndRecordedAtAfter(
            @Param("sensorDeviceId") Long sensorDeviceId,
            @Param("cutoff") Instant cutoff);

    @Query(value = "SELECT * FROM sensor_readings WHERE recorded_at < :cutoff", nativeQuery = true)
    List<SensorReading> findAllBeforeRetentionCutoff(@Param("cutoff") Instant cutoff);

    /**
     * Deletes retained sensor readings older than the configured cutoff timestamp.
     *
     * @param cutoff UTC cutoff timestamp
     * @return number of deleted readings
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM SensorReading r WHERE r.recordedAt < :cutoff")
    int deleteByRecordedAtBefore(@Param("cutoff") Instant cutoff);
}
