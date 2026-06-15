-- V51: Associate environment controllers with a warehouse (mirrors sensor_devices.warehouse_id).
--
-- Controllers previously had no spatial association; warehouse_id lets center/warehouse-scoped
-- alert routing and the dashboard place controllers under a warehouse. Nullable so existing rows
-- survive; new registrations require it at the API layer. Existing controllers need backfill.

ALTER TABLE environment_controllers ADD COLUMN warehouse_id BIGINT;

ALTER TABLE environment_controllers
    ADD CONSTRAINT fk_environment_controllers_warehouse
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id);

CREATE INDEX idx_environment_controllers_warehouse ON environment_controllers (warehouse_id);

COMMENT ON COLUMN environment_controllers.warehouse_id IS '제어기가 설치된 창고 FK (알림 라우팅 기준)';
