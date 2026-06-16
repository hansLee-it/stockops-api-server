-- V52: Role-based notice routing — map roles to webhook channels, and let a notice target roles.
--
-- A notice/announcement is delivered to the webhook channel(s) mapped to its target roles
-- (empty target_roles = broadcast to every enabled role channel).

CREATE TABLE role_webhook_configs (
    id            BIGSERIAL    PRIMARY KEY,
    role          VARCHAR(100) NOT NULL,
    provider_type VARCHAR(20)  NOT NULL DEFAULT 'TEAMS',
    webhook_url   VARCHAR(2048) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE role_webhook_configs IS '역할별 웹훅 채널 매핑 (공지/전체알림 라우팅)';
COMMENT ON COLUMN role_webhook_configs.role IS '대상 역할명 (예: ADMIN)';
COMMENT ON COLUMN role_webhook_configs.provider_type IS '웹훅 provider (TEAMS 등)';
COMMENT ON COLUMN role_webhook_configs.webhook_url IS '대상 채널 웹훅 URL';
COMMENT ON COLUMN role_webhook_configs.enabled IS '활성 여부';

CREATE INDEX idx_role_webhook_configs_role ON role_webhook_configs (role);

ALTER TABLE notices ADD COLUMN target_roles JSONB NOT NULL DEFAULT '[]';
COMMENT ON COLUMN notices.target_roles IS '공지 대상 역할 JSON 배열 (비어 있으면 전체 역할 채널로 발송)';
