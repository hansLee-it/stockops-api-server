-- Environment alerts become the event store for threshold/state changes.
-- An alert stays ACTIVE (warning/critical) until the sensor normalizes (resolved_at set)
-- or an administrator acknowledges it (acknowledged = true).
ALTER TABLE environment_alerts ADD COLUMN resolved_at TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN environment_alerts.resolved_at IS '센서 정상화로 자동 해제된 시각 (UTC). NULL이면 미해제(활성) 상태';

CREATE INDEX IF NOT EXISTS idx_environment_alerts_active
    ON environment_alerts (sensor_device_id, resolved_at, acknowledged);
