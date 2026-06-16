package com.stockops.notification.role;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for role → webhook channel configuration.
 *
 * @author StockOps Team
 * @since 2.3
 */
@Repository
public interface RoleWebhookConfigRepository extends JpaRepository<RoleWebhookConfig, Long> {

    /** All enabled role channels (used to broadcast a notice with no explicit target roles). */
    List<RoleWebhookConfig> findByEnabledTrue();

    /** Enabled channels for the given roles. */
    List<RoleWebhookConfig> findByRoleInAndEnabledTrue(Collection<String> roles);
}
