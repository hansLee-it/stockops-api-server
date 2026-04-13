package com.stockops.audit;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreRemove;

/**
 * Entity listener that snapshots and persists mutation audit entries.
 *
 * @author StockOps Team
 * @since 1.0
 */
public class MutationAuditEntityListener {

    /**
     * Stores the baseline entity snapshot after loading from persistence.
     *
     * @param entity loaded entity instance
     */
    @PostLoad
    public void onPostLoad(final Object entity) {
        AuditSnapshotRegistry.capture(entity);
    }

    /**
     * Persists an audit entry for entity creation.
     *
     * @param entity persisted entity instance
     */
    @PostPersist
    public void onPostPersist(final Object entity) {
        persist(entity, "CREATE", null, AuditSnapshotRegistry.snapshot(entity));
    }

    /**
     * Persists an audit entry for entity update.
     *
     * @param entity updated entity instance
     */
    @PostUpdate
    public void onPostUpdate(final Object entity) {
        persist(entity, "UPDATE", AuditSnapshotRegistry.previous(entity), AuditSnapshotRegistry.snapshot(entity));
    }

    /**
     * Captures a delete snapshot before removal.
     *
     * @param entity entity scheduled for deletion
     */
    @PreRemove
    public void onPreRemove(final Object entity) {
        AuditSnapshotRegistry.capture(entity);
    }

    /**
     * Persists an audit entry for entity deletion.
     *
     * @param entity removed entity instance
     */
    @PostRemove
    public void onPostRemove(final Object entity) {
        persist(entity, "DELETE", AuditSnapshotRegistry.previous(entity), null);
    }

    private void persist(final Object entity,
                         final String action,
                         final String oldValue,
                         final String newValue) {
        final MutationAuditPersistenceService auditService = SpringContextHolder.getBean(MutationAuditPersistenceService.class);
        if (auditService != null) {
            auditService.record(entity, action, oldValue, newValue);
        }
        AuditSnapshotRegistry.clear(entity);
    }
}
