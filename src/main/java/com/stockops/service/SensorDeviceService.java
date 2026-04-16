package com.stockops.service;

import com.stockops.dto.SensorDeviceRequest;
import com.stockops.dto.SensorDeviceResponse;
import com.stockops.entity.SensorDevice;
import com.stockops.exception.ConflictException;
import com.stockops.exception.InvalidOperationException;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.integration.sensimul.ParsedSensorTopic;
import com.stockops.integration.sensimul.SensimulTopics;
import com.stockops.repository.SensorDeviceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sensor device master business logic.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
public class SensorDeviceService {

    private final SensorDeviceRepository sensorDeviceRepository;

    /**
     * Creates the service.
     *
     * @param sensorDeviceRepository sensor device repository
     */
    public SensorDeviceService(final SensorDeviceRepository sensorDeviceRepository) {
        this.sensorDeviceRepository = sensorDeviceRepository;
    }

    /**
     * Creates a new sensor device or reactivates a matching soft-deleted sensor.
     *
     * @param request creation payload
     * @return created or reactivated sensor device
     */
    @Transactional
    public SensorDeviceResponse createSensorDevice(final SensorDeviceRequest request) {
        final String mqttTopic = resolveCanonicalTopic(request);

        if (sensorDeviceRepository.findByMqttTopicAndDeletedFalse(mqttTopic).isPresent()) {
            throw new ConflictException(
                    "Sensor already exists for siteId/sensorId: " + request.siteId() + "/" + request.sensorId());
        }

        final SensorDevice sensorDevice = sensorDeviceRepository.findByMqttTopic(mqttTopic)
                .map(existing -> reactivate(existing, request, mqttTopic))
                .orElseGet(() -> createNew(request, mqttTopic));

        return toResponse(sensorDeviceRepository.save(sensorDevice));
    }

    /**
     * Returns an active sensor device by id.
     *
     * @param id sensor device identifier
     * @return sensor device response
     */
    @Transactional(readOnly = true)
    public SensorDeviceResponse getSensorDeviceById(final Long id) {
        return toResponse(findActiveById(id));
    }

    /**
     * Returns active sensor devices in a paged response.
     *
     * @param pageable paging parameters
     * @return paged sensor devices
     */
    @Transactional(readOnly = true)
    public Page<SensorDeviceResponse> getSensorDevices(final Pageable pageable) {
        return sensorDeviceRepository.findAllByDeletedFalse(pageable).map(this::toResponse);
    }

    /**
     * Updates an active sensor device.
     *
     * @param id sensor device identifier
     * @param request update payload
     * @return updated sensor device
     */
    @Transactional
    public SensorDeviceResponse updateSensorDevice(final Long id, final SensorDeviceRequest request) {
        final SensorDevice sensorDevice = findActiveById(id);
        final String mqttTopic = resolveCanonicalTopic(request);

        sensorDeviceRepository.findByMqttTopicAndDeletedFalse(mqttTopic)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "Sensor already exists for siteId/sensorId: " + request.siteId() + "/" + request.sensorId());
                });

        applyRequest(sensorDevice, request, mqttTopic);
        return toResponse(sensorDeviceRepository.save(sensorDevice));
    }

    /**
     * Soft-deletes an active sensor device.
     *
     * @param id sensor device identifier
     */
    @Transactional
    public void deleteSensorDevice(final Long id) {
        final SensorDevice sensorDevice = findActiveById(id);
        sensorDevice.setDeleted(true);
        sensorDevice.setActive(false);
        sensorDeviceRepository.save(sensorDevice);
    }

    /**
     * Reactivates a soft-deleted sensor device.
     *
     * @param id sensor device identifier
     * @return reactivated sensor device
     */
    @Transactional
    public SensorDeviceResponse reactivateSensorDevice(final Long id) {
        final SensorDevice sensorDevice = sensorDeviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor device not found: " + id));
        if (!sensorDevice.isDeleted()) {
            throw new ResourceNotFoundException("Sensor device not found: " + id);
        }

        sensorDeviceRepository.findByMqttTopicAndDeletedFalse(sensorDevice.getMqttTopic())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "Sensor already exists for topic: " + sensorDevice.getMqttTopic());
                });

        sensorDevice.setDeleted(false);
        sensorDevice.setActive(true);
        return toResponse(sensorDeviceRepository.save(sensorDevice));
    }

    /**
     * Finds an active sensor device by external identifiers.
     *
     * @param siteId Sensimul site identifier
     * @param sensorId Sensimul sensor identifier
     * @return sensor device response
     */
    @Transactional(readOnly = true)
    public SensorDeviceResponse getSensorDeviceByExternalIds(final String siteId, final String sensorId) {
        final String mqttTopic = buildTopic(siteId, sensorId);
        return toResponse(sensorDeviceRepository.findByMqttTopicAndDeletedFalse(mqttTopic)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sensor device not found for siteId/sensorId: " + siteId + "/" + sensorId)));
    }

    private SensorDevice createNew(final SensorDeviceRequest request, final String mqttTopic) {
        final SensorDevice sensorDevice = new SensorDevice();
        applyRequest(sensorDevice, request, mqttTopic);
        sensorDevice.setDeleted(false);
        sensorDevice.setActive(true);
        return sensorDevice;
    }

    private SensorDevice reactivate(
            final SensorDevice sensorDevice,
            final SensorDeviceRequest request,
            final String mqttTopic) {
        applyRequest(sensorDevice, request, mqttTopic);
        sensorDevice.setDeleted(false);
        sensorDevice.setActive(true);
        return sensorDevice;
    }

    private void applyRequest(
            final SensorDevice sensorDevice,
            final SensorDeviceRequest request,
            final String mqttTopic) {
        sensorDevice.setName(request.sensorId());
        sensorDevice.setLocation(request.location());
        sensorDevice.setSensorType(request.sensorType());
        sensorDevice.setExternalSensorId(request.sensorId());
        sensorDevice.setMqttTopic(mqttTopic);
        sensorDevice.setSourceChannel(request.sourceChannel());
    }

    private String resolveCanonicalTopic(final SensorDeviceRequest request) {
        final String expectedTopic = buildTopic(request.siteId(), request.sensorId());
        if (!expectedTopic.equals(request.mqttTopic())) {
            throw new InvalidOperationException(
                    "mqttTopic must match sensimul/sites/{siteId}/sensors/{sensorId}: " + expectedTopic);
        }
        return expectedTopic;
    }

    private SensorDevice findActiveById(final Long id) {
        return sensorDeviceRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sensor device not found: " + id));
    }

    private SensorDeviceResponse toResponse(final SensorDevice sensorDevice) {
        final ParsedSensorTopic parsedTopic = SensimulTopics.parseLiveTopic(sensorDevice.getMqttTopic())
                .orElseGet(() -> new ParsedSensorTopic(sensorDevice.getSourceChannel(), sensorDevice.getExternalSensorId()));
        return new SensorDeviceResponse(
                sensorDevice.getId(),
                sensorDevice.getName(),
                parsedTopic.siteId(),
                parsedTopic.sensorId(),
                sensorDevice.getSensorType(),
                sensorDevice.getLocation(),
                sensorDevice.getMqttTopic(),
                sensorDevice.getSourceChannel(),
                sensorDevice.isActive(),
                sensorDevice.isDeleted(),
                sensorDevice.getCreatedAt(),
                sensorDevice.getUpdatedAt());
    }

    private String buildTopic(final String siteId, final String sensorId) {
        return SensimulTopics.liveSensor(siteId, sensorId);
    }
}
