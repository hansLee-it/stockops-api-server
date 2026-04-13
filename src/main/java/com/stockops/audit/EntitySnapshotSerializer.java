package com.stockops.audit;

import jakarta.persistence.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serializes entity state into a compact JSON-like string for audit persistence.
 *
 * @author StockOps Team
 * @since 1.0
 */
public final class EntitySnapshotSerializer {

    private EntitySnapshotSerializer() {
    }

    /**
     * Serializes the supplied entity into a string snapshot.
     *
     * @param entity entity instance
     * @return serialized snapshot
     */
    public static String serialize(final Object entity) {
        if (entity == null) {
            return null;
        }

        final Map<String, Object> values = new LinkedHashMap<>();
        values.put("entity", entity.getClass().getSimpleName());

        Class<?> type = entity.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }

                field.setAccessible(true);
                try {
                    values.put(field.getName(), normalizeValue(field.get(entity)));
                } catch (IllegalAccessException ignored) {
                    values.put(field.getName(), "<unavailable>");
                }
            }
            type = type.getSuperclass();
        }

        return values.toString();
    }

    private static Object normalizeValue(final Object value) {
        if (value == null
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof CharSequence
                || value instanceof Enum<?>
                || value instanceof TemporalAccessor) {
            return value;
        }

        if (value instanceof Collection<?>) {
            return "<collection size=" + ((Collection<?>) value).size() + ">";
        }

        if (value.getClass().isAnnotationPresent(Entity.class)) {
            return value.getClass().getSimpleName() + "#" + extractEntityId(value);
        }

        return String.valueOf(value);
    }

    private static Object extractEntityId(final Object entity) {
        Class<?> type = entity.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                if (!"id".equals(field.getName())) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    return field.get(entity);
                } catch (IllegalAccessException ignored) {
                    return null;
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }
}
