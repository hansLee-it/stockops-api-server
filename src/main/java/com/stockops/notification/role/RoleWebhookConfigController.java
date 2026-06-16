package com.stockops.notification.role;

import com.stockops.exception.ResourceNotFoundException;
import com.stockops.notification.webhook.WebhookEndpointConfig.WebhookProviderType;
import jakarta.validation.constraints.NotBlank;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD for role → webhook channel mappings used by notice/announcement routing.
 *
 * @author StockOps Team
 * @since 2.3
 */
@RestController
@RequestMapping("/api/v1/role-webhook-configs")
public class RoleWebhookConfigController {

    private final RoleWebhookConfigRepository repository;

    public RoleWebhookConfigController(final RoleWebhookConfigRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleWebhookConfigResponse>> list() {
        return ResponseEntity.ok(repository.findAll().stream().map(RoleWebhookConfigResponse::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleWebhookConfigResponse> create(@RequestBody final RoleWebhookConfigRequest request) {
        final RoleWebhookConfig config = new RoleWebhookConfig();
        apply(config, request);
        return ResponseEntity.status(201).body(RoleWebhookConfigResponse.from(repository.save(config)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleWebhookConfigResponse> update(@PathVariable final Long id,
            @RequestBody final RoleWebhookConfigRequest request) {
        final RoleWebhookConfig config = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoleWebhookConfig not found: " + id));
        apply(config, request);
        return ResponseEntity.ok(RoleWebhookConfigResponse.from(repository.save(config)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void apply(final RoleWebhookConfig config, final RoleWebhookConfigRequest request) {
        config.setRole(request.role());
        config.setProviderType(request.providerType() == null
                ? WebhookProviderType.TEAMS
                : WebhookProviderType.valueOf(request.providerType()));
        config.setWebhookUrl(request.webhookUrl());
        config.setEnabled(request.enabled() == null ? true : request.enabled());
    }

    /** Create/update payload. */
    public record RoleWebhookConfigRequest(
            @NotBlank String role,
            String providerType,
            @NotBlank String webhookUrl,
            Boolean enabled) {
    }

    /** Response payload — the webhook URL is not echoed back. */
    public record RoleWebhookConfigResponse(
            Long id,
            String role,
            String providerType,
            boolean enabled) {
        static RoleWebhookConfigResponse from(final RoleWebhookConfig config) {
            return new RoleWebhookConfigResponse(
                    config.getId(),
                    config.getRole(),
                    config.getProviderType().name(),
                    config.isEnabled());
        }
    }
}
