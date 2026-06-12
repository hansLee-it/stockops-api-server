-- At most ONE active (unresolved, unacknowledged) alert per sensor.
-- Closes the race where two ingestion instances both see "no active alert" and insert twice;
-- the loser gets a unique violation and retries through the escalate path.
-- PostgreSQL-only (partial indexes are unsupported by the H2 test database, so this lives in
-- the vendor-specific Flyway location).
CREATE UNIQUE INDEX ux_environment_alerts_active_sensor
    ON environment_alerts (sensor_device_id)
    WHERE resolved_at IS NULL AND acknowledged = false;
