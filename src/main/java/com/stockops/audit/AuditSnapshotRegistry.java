package com.stockops.audit;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Thread-local store of baseline entity snapshots for mutation auditing.
 *
 * @author StockOps Team
 * @since 1.0
 */
public final class AuditSnapshotRegistry {

    private static final ThreadLocal<Map<Object, String>> SNAPSHOTS = ThreadLocal.withInitial(IdentityHashMap::new);

    private AuditSnapshotRegistry() {
    }

    /**
     * Captures the current snapshot for an entity instance.
     *
     * @param entity entity instance
     */
    public static void capture(final Object entity) {
        SNAPSHOTS.get().put(entity, EntitySnapshotSerializer.serialize(entity));
    }

    /**
     * Returns the previous snapshot for an entity instance.
     *
     * @param entity entity instance
     * @return previously captured snapshot
     */
    public static String previous(final Object entity) {
        return SNAPSHOTS.get().get(entity);
    }

    /**
     * Returns a fresh snapshot for the current entity state.
     *
     * @param entity entity instance
     * @return current snapshot
     */
    public static String snapshot(final Object entity) {
        return EntitySnapshotSerializer.serialize(entity);
    }

    /**
     * Clears a stored snapshot for an entity instance.
     *
     * @param entity entity instance
     */
    public static void clear(final Object entity) {
        final Map<Object, String> snapshots = SNAPSHOTS.get();
        snapshots.remove(entity);
        if (snapshots.isEmpty()) {
            SNAPSHOTS.remove();
        }
    }
}
