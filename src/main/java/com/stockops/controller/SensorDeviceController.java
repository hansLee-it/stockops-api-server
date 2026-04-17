package com.stockops.controller;

import com.stockops.dto.SensorDeviceRequest;
import com.stockops.dto.SensorDeviceResponse;
import com.stockops.service.SensorDeviceService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sensor device lifecycle API controller.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/environment/sensors")
public class SensorDeviceController {

    private final SensorDeviceService sensorDeviceService;

    /**
     * Creates the controller.
     *
     * @param sensorDeviceService sensor device service
     */
    public SensorDeviceController(final SensorDeviceService sensorDeviceService) {
        this.sensorDeviceService = sensorDeviceService;
    }

    /**
     * Creates a sensor device.
     *
     * @param request creation payload
     * @return created sensor device
     */
    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_MANAGE')")
    public ResponseEntity<SensorDeviceResponse> createSensorDevice(@Valid @RequestBody final SensorDeviceRequest request) {
        final SensorDeviceResponse sensorDevice = sensorDeviceService.createSensorDevice(request);
        return ResponseEntity.created(URI.create("/api/v1/environment/sensors/" + sensorDevice.id())).body(sensorDevice);
    }

    /**
     * Returns active sensor devices.
     *
     * @param pageable paging parameters
     * @return paged active sensors
     */
    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_READ')")
    public ResponseEntity<Page<SensorDeviceResponse>> getSensorDevices(
            @PageableDefault(size = 20) final Pageable pageable) {
        return ResponseEntity.ok(sensorDeviceService.getSensorDevices(pageable));
    }

    /**
     * Returns an active sensor device by id.
     *
     * @param id sensor device identifier
     * @return sensor device response
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_READ')")
    public ResponseEntity<SensorDeviceResponse> getSensorDevice(@PathVariable final Long id) {
        return ResponseEntity.ok(sensorDeviceService.getSensorDeviceById(id));
    }

    /**
     * Updates an active sensor device.
     *
     * @param id sensor device identifier
     * @param request update payload
     * @return updated sensor device
     */
    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_MANAGE')")
    public ResponseEntity<SensorDeviceResponse> updateSensorDevice(
            @PathVariable final Long id,
            @Valid @RequestBody final SensorDeviceRequest request) {
        return ResponseEntity.ok(sensorDeviceService.updateSensorDevice(id, request));
    }

    /**
     * Soft-deletes an active sensor device.
     *
     * @param id sensor device identifier
     * @return no content response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_MANAGE')")
    public ResponseEntity<Void> deleteSensorDevice(@PathVariable final Long id) {
        sensorDeviceService.deleteSensorDevice(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reactivates a soft-deleted sensor device.
     *
     * @param id sensor device identifier
     * @return reactivated sensor device
     */
    @PostMapping("/{id}/reactivate")
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_MANAGE')")
    public ResponseEntity<SensorDeviceResponse> reactivateSensorDevice(@PathVariable final Long id) {
        return ResponseEntity.ok(sensorDeviceService.reactivateSensorDevice(id));
    }

    /**
     * Looks up an active sensor device by Sensimul external identifiers.
     *
     * @param siteId Sensimul site identifier
     * @param sensorId Sensimul sensor identifier
     * @return sensor device response
     */
    @GetMapping("/external/{siteId}/{sensorId}")
    @PreAuthorize("@permissionChecker.hasPermission('ENVIRONMENT_READ')")
    public ResponseEntity<SensorDeviceResponse> getSensorDeviceByExternalIds(
            @PathVariable final String siteId,
            @PathVariable final String sensorId) {
        return ResponseEntity.ok(sensorDeviceService.getSensorDeviceByExternalIds(siteId, sensorId));
    }
}
