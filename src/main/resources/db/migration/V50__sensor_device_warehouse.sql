-- V50: Replace sensor_devices.location (free text) with a structured warehouse association.
--
-- The free-text location column originated for CCTV labelling and is superseded by a real
-- warehouse FK, which also drives center/warehouse-scoped alert routing. warehouse_id is
-- nullable so existing rows survive the migration; new registrations require it at the API
-- layer. Existing sensors must be backfilled with their warehouse separately.

ALTER TABLE sensor_devices ADD COLUMN warehouse_id BIGINT;

ALTER TABLE sensor_devices
    ADD CONSTRAINT fk_sensor_devices_warehouse
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);

CREATE INDEX idx_sensor_devices_warehouse ON sensor_devices (warehouse_id);

COMMENT ON COLUMN sensor_devices.warehouse_id IS '센서가 설치된 창고 FK (알림 라우팅 기준)';

ALTER TABLE sensor_devices DROP COLUMN location;
