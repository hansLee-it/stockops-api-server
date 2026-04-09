package com.stockops.service;

import com.stockops.dto.CreateLocationRequest;
import com.stockops.dto.LocationDTO;
import com.stockops.dto.UpdateLocationRequest;
import com.stockops.entity.Location;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.LocationRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for location master data.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
@Transactional
public class LocationService {

    private final LocationRepository locationRepository;

    /**
     * Creates the location service.
     *
     * @param locationRepository location repository
     */
    public LocationService(final LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    /**
     * Creates a new location.
     *
     * @param request location creation request
     * @return created location DTO
     */
    public LocationDTO createLocation(final CreateLocationRequest request) {
        if (locationRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Location code already exists");
        }

        final Location location = new Location();
        location.setCode(request.code());
        location.setName(request.name());
        location.setType(request.type());
        location.setZone(request.zone());
        location.setShelf(request.shelf());
        location.setLevel(request.level());

        return toDTO(locationRepository.save(location));
    }

    /**
     * Returns a location by identifier.
     *
     * @param id location identifier
     * @return location DTO
     */
    @Transactional(readOnly = true)
    public LocationDTO getLocationById(final Long id) {
        return toDTO(findLocationById(id));
    }

    /**
     * Returns a location by code.
     *
     * @param code location code
     * @return location DTO
     */
    @Transactional(readOnly = true)
    public LocationDTO getLocationByCode(final String code) {
        return toDTO(locationRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + code)));
    }

    /**
     * Returns all locations.
     *
     * @return all location DTOs
     */
    @Transactional(readOnly = true)
    public List<LocationDTO> getAllLocations() {
        return locationRepository.findAll().stream().map(this::toDTO).toList();
    }

    /**
     * Returns all locations of the given type.
     *
     * @param type location type
     * @return matching location DTOs
     */
    @Transactional(readOnly = true)
    public List<LocationDTO> getLocationsByType(final String type) {
        return locationRepository.findByType(type).stream().map(this::toDTO).toList();
    }

    /**
     * Updates an existing location.
     *
     * @param id location identifier
     * @param request update request
     * @return updated location DTO
     */
    public LocationDTO updateLocation(final Long id, final UpdateLocationRequest request) {
        final Location location = findLocationById(id);

        if (request.name() != null) {
            location.setName(request.name());
        }
        if (request.type() != null) {
            location.setType(request.type());
        }
        if (request.zone() != null) {
            location.setZone(request.zone());
        }
        if (request.shelf() != null) {
            location.setShelf(request.shelf());
        }
        if (request.level() != null) {
            location.setLevel(request.level());
        }

        return toDTO(locationRepository.save(location));
    }

    /**
     * Deletes a location.
     *
     * @param id location identifier
     */
    public void deleteLocation(final Long id) {
        if (!locationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Location not found: " + id);
        }
        locationRepository.deleteById(id);
    }

    private Location findLocationById(final Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + id));
    }

    private LocationDTO toDTO(final Location location) {
        return new LocationDTO(
                location.getId(),
                location.getCode(),
                location.getName(),
                location.getType(),
                location.getZone(),
                location.getShelf(),
                location.getLevel(),
                location.getCreatedAt(),
                location.getUpdatedAt());
    }
}
