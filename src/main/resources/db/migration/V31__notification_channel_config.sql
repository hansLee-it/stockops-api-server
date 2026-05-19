-- V31: Notification channel configuration table
-- Stores per-center/warehouse alert type channel configurations
-- Each row defines which channels (SMS, EMAIL, WEBHOOK) are enabled for a specific alert type

CREATE TABLE notification_channel_configs (
    id              BIGSERIAL       PRIMARY KEY,
    center_id       BIGINT          NOT NULL REFERENCES centers(id) ON DELETE CASCADE,
    warehouse_id    BIGINT          REFERENCES warehouses(id) ON DELETE CASCADE,
    alert_type      VARCHAR(50)     NOT NULL,
    channels        JSONB           NOT NULL DEFAULT '[]',
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_channel_config_scope UNIQUE (center_id, warehouse_id, alert_type)
);

COMMENT ON TABLE notification_channel_configs IS '알림 채널 설정 - 센터/창고 및 알림 유형별 채널 활성화 설정';
COMMENT ON COLUMN notification_channel_configs.id IS '알림 채널 설정 PK';
COMMENT ON COLUMN notification_channel_configs.center_id IS '센터 ID (필수)';
COMMENT ON COLUMN notification_channel_configs.warehouse_id IS '창고 ID (NULL이면 센터 전체 설정)';
COMMENT ON COLUMN notification_channel_configs.alert_type IS '알림 유형 (TEMPERATURE, HUMIDITY 등)';
COMMENT ON COLUMN notification_channel_configs.channels IS '채널 설정 JSON 배열 (예: [{"type":"SMS","enabled":true,"webhookProvider":null}])';
COMMENT ON COLUMN notification_channel_configs.active IS '설정 활성 여부';
COMMENT ON COLUMN notification_channel_configs.created_at IS '생성 시각 (UTC 기준 저장)';
COMMENT ON COLUMN notification_channel_configs.updated_at IS '수정 시각 (UTC 기준 저장)';

CREATE INDEX IF NOT EXISTS idx_channel_config_center_id
    ON notification_channel_configs (center_id);
CREATE INDEX IF NOT EXISTS idx_channel_config_warehouse_id
    ON notification_channel_configs (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_channel_config_active
    ON notification_channel_configs (active);
