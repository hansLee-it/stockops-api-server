package com.stockops.controller;

import com.stockops.dto.CreateLocationRequest;
import com.stockops.dto.LocationDTO;
import com.stockops.dto.UpdateLocationRequest;
import com.stockops.service.LocationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Location master data API controller.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/locations")
public class LocationController {

    private final LocationService locationService;

    /**
     * Creates the location controller.
     *
     * @param locationService location service
     */
    public LocationController(final LocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * Creates a location.
     *
     * @param request location creation request
     * @return created location DTO
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<LocationDTO> createLocation(@Valid @RequestBody final CreateLocationRequest request) {
        return ResponseEntity.created(URI.create("/api/v1/locations")).body(locationService.createLocation(request));
    }

    /**
     * Returns a location by identifier.
     *
     * @param id location identifier
     * @return location DTO
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<LocationDTO> getLocation(@PathVariable final Long id) {
        return ResponseEntity.ok(locationService.getLocationById(id));
    }

    /**
     * Returns a location by code.
     *
     * @param code location code
     * @return location DTO
     */
    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<LocationDTO> getLocationByCode(@PathVariable final String code) {
        return ResponseEntity.ok(locationService.getLocationByCode(code));
    }

    /**
     * Returns all locations or locations by type.
     *
     * @param type optional location type filter
     * @return location DTOs
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    public ResponseEntity<List<LocationDTO>> getAllLocations(@RequestParam(required = false) final String type) {
        if (type != null) {
            return ResponseEntity.ok(locationService.getLocationsByType(type));
        }
        return ResponseEntity.ok(locationService.getAllLocations());
    }

    /**
     * Updates a location.
     *
     * @param id location identifier
     * @param request update request
     * @return updated location DTO
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<LocationDTO> updateLocation(
            @PathVariable final Long id,
            @Valid @RequestBody final UpdateLocationRequest request) {
        return ResponseEntity.ok(locationService.updateLocation(id, request));
    }

    /**
     * Deletes a location.
     *
     * @param id location identifier
     * @return empty response
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLocation(@PathVariable final Long id) {
        locationService.deleteLocation(id);
        return ResponseEntity.noContent().build();
    }
}
