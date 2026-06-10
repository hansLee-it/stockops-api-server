-- Live sensor measurements are no longer persisted in bulk.
-- Real-time values are viewed client-side (admin-web MQTT/session); the API now stores
-- only threshold EVENTS in environment_alerts (see V40). Drop the measurement tables.
DROP TABLE IF EXISTS sensor_latest;
DROP TABLE IF EXISTS sensor_readings;
