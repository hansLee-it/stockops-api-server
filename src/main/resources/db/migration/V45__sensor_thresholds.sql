-- Per-sensor two-level threshold configuration.
-- When any bound is set, the server derives alert severity from the measured value
-- (outside crit bounds → CRITICAL, outside warn bounds → WARNING, inside → normal).
-- Sensors with no bounds keep the legacy behavior of trusting the Sensimul payload status.
ALTER TABLE sensor_devices ADD COLUMN warn_min DOUBLE PRECISION;
ALTER TABLE sensor_devices ADD COLUMN warn_max DOUBLE PRECISION;
ALTER TABLE sensor_devices ADD COLUMN crit_min DOUBLE PRECISION;
ALTER TABLE sensor_devices ADD COLUMN crit_max DOUBLE PRECISION;

COMMENT ON COLUMN sensor_devices.warn_min IS '경고 하한 (미설정 시 payload status 판정)';
COMMENT ON COLUMN sensor_devices.warn_max IS '경고 상한';
COMMENT ON COLUMN sensor_devices.crit_min IS '위험 하한';
COMMENT ON COLUMN sensor_devices.crit_max IS '위험 상한';
