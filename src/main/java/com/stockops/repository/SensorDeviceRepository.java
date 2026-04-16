package com.stockops.repository;

import com.stockops.entity.SensorDevice;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for environment sensor device masters.
 *
 * @author StockOps Team
 * @since 1.0
 */
public interface SensorDeviceRepository extends JpaRepository<SensorDevice, Long> {

    Optional<SensorDevice> findByIdAndDeletedFalse(Long id);

    Optional<SensorDevice> findByExternalSensorIdAndDeletedFalse(String externalSensorId);

    Optional<SensorDevice> findByMqttTopic(String mqttTopic);

    Optional<SensorDevice> findByMqttTopicAndDeletedFalse(String mqttTopic);

    Optional<SensorDevice> findByExternalSensorId(String externalSensorId);

    Page<SensorDevice> findAllByDeletedFalse(Pageable pageable);

    boolean existsByExternalSensorIdAndDeletedFalse(String externalSensorId);
}
