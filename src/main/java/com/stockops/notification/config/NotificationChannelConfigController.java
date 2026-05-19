package com.stockops.notification.config;

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
 * REST controller for notification channel configuration CRUD and webhook testing.
 *
 * @author StockOps Team
 * @since 2.0
 */
@RestController
@RequestMapping("/api/v1/notification-channel-configs")
public class NotificationChannelConfigController {

    private final NotificationChannelConfigService configService;

    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_READ')")
    public List<NotificationChannelConfigDTO> listConfigs(
            @RequestParam Long centerId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String alertType) {

        List<NotificationChannelConfig> configs = configService.findAllByCenterId(centerId);

        return configs.stream()
                .filter(c -> warehouseId == null || warehouseId.equals(c.getWarehouseId()) || c.getWarehouseId() == null)
                .filter(c -> alertType == null || alertType.equals(c.getAlertType()))
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_READ')")
    public NotificationChannelConfigDTO getConfig(@PathVariable Long id) {
        return toDto(configService.findById(id));
    }

    @GetMapping("/resolve")
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_READ')")
    public ResponseEntity<NotificationChannelConfigDTO> resolveConfig(
            @RequestParam Long centerId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam String alertType) {

        return configService.resolveChannels(centerId, warehouseId, alertType)
                .map(config -> ResponseEntity.ok(toDto(config)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_CREATE')")
    public NotificationChannelConfigDTO createConfig(@RequestBody NotificationChannelConfigRequest request) {
        return toDto(configService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_UPDATE')")
    public NotificationChannelConfigDTO updateConfig(@PathVariable Long id,
                                                      @RequestBody NotificationChannelConfigRequest request) {
        return toDto(configService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_DELETE')")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        configService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test-webhook")
    @PreAuthorize("@permissionChecker.hasPermission('CENTER_UPDATE')")
    public WebhookTestResult testWebhook(@PathVariable Long id) {
        return configService.testWebhook(id);
    }

    private NotificationChannelConfigDTO toDto(NotificationChannelConfig config) {
        List<ChannelEntryDTO> channelDtos = config.getChannels().stream()
                .map(ch -> new ChannelEntryDTO(
                        ch.getType().name(),
                        ch.isEnabled(),
                        ch.getWebhookProvider()))
                .toList();

        return new NotificationChannelConfigDTO(
                config.getId(),
                config.getCenterId(),
                config.getWarehouseId(),
                config.getAlertType(),
                channelDtos,
                config.isActive(),
                config.getCreatedAt(),
                config.getUpdatedAt());
    }

    public NotificationChannelConfigController(final NotificationChannelConfigService configService) {
        this.configService = configService;
    }

}