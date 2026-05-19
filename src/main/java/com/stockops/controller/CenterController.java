package com.stockops.controller;

import com.stockops.entity.Center;
import com.stockops.service.CenterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Center management.
 *
 * @author StockOps Team
 * @since 2.0
 */
@RestController
@RequestMapping("/api/v1/centers")
public class CenterController {

    private final CenterService centerService;

    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_READ')")
    public List<Center> getAllCenters() {
        return centerService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_READ')")
    public Center getCenterById(@PathVariable Long id) {
        return centerService.findById(id);
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_READ')")
    public Center getCenterByCode(@PathVariable String code) {
        return centerService.findByCode(code);
    }

    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_CREATE')")
    public Center createCenter(@RequestBody Center center) {
        return centerService.create(center);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_UPDATE')")
    public Center updateCenter(@PathVariable Long id, @RequestBody Center center) {
        return centerService.update(id, center);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_DELETE')")
    public ResponseEntity<Void> deleteCenter(@PathVariable Long id) {
        centerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    public CenterController(final CenterService centerService) {
        this.centerService = centerService;
    }

}
