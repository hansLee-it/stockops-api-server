package com.stockops.audit;

import com.stockops.entity.AuditLog;
import com.stockops.repository.AuditLogRepository;
import com.stockops.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.Instant;

/**
 * Persists mutation audit rows captured by JPA entity listeners.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
public class MutationAuditPersistenceService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Stores an audit log for the supplied entity mutation.
     *
     * @param entity mutated entity instance
     * @param action mutation action
     * @param oldValue previous snapshot
     * @param newValue new snapshot
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(final Object entity,
                       final String action,
                       final String oldValue,
                       final String newValue) {
        if (entity == null || entity instanceof AuditLog) {
            return;
        }

        final AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entity.getClass().getSimpleName());
        auditLog.setEntityId(extractEntityId(entity));
        auditLog.setTargetIdentifier(extractTargetIdentifier(entity));
        auditLog.setAction(action);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setPerformedBy(resolveUserId());
        auditLog.setPerformedByEmail(currentUserProvider.getCurrentUserEmail());
        auditLog.setPerformedAt(Instant.now());
        auditLogRepository.save(auditLog);
    }

    private Long resolveUserId() {
        try {
            return currentUserProvider.getCurrentUserId();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private Long extractEntityId(final Object entity) {
        final Object id = extractFieldValue(entity, "id");
        return id instanceof Number ? ((Number) id).longValue() : null;
    }

    private String extractTargetIdentifier(final Object entity) {
        for (String fieldName : new String[]{"code", "name", "email", "poNumber", "shipmentNumber", "lotNumber"}) {
            final Object value = extractFieldValue(entity, fieldName);
            if (value instanceof String stringValue && !stringValue.isBlank()) {
                return stringValue;
            }
        }
        final Long entityId = extractEntityId(entity);
        return entityId == null ? null : entity.getClass().getSimpleName() + "#" + entityId;
    }

    private Object extractFieldValue(final Object entity, final String fieldName) {
        Class<?> type = entity.getClass();
        while (type != null && type != Object.class) {
            try {
                final Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(entity);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return null;
            }
        }
        return null;
    }

    public MutationAuditPersistenceService(final AuditLogRepository auditLogRepository, final CurrentUserProvider currentUserProvider) {
        this.auditLogRepository = auditLogRepository;
        this.currentUserProvider = currentUserProvider;
    }
}
