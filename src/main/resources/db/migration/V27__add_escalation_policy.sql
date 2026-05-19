-- V27: Escalation Policy Schema
-- 에스컬레이션 정책 및 규칙 테이블 추가
-- 환경 모니터링 알림의 다단계 에스컬레이션 지원

-- ============================================
-- 1. ESCALATION POLICIES TABLE
-- ============================================
CREATE TABLE escalation_policies (
    id BIGSERIAL PRIMARY KEY,
    center_id BIGINT NOT NULL,
    warehouse_id BIGINT,
    alert_type VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_escalation_policies_center FOREIGN KEY (center_id) REFERENCES centers(id),
    CONSTRAINT fk_escalation_policies_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouses(id)
);

COMMENT ON TABLE escalation_policies IS '에스컬레이션 정책 마스터 - 센터/창고 및 알림 유형별 에스컬레이션 규칙 그룹';
COMMENT ON COLUMN escalation_policies.id IS '에스컬레이션 정책 PK';
COMMENT ON COLUMN escalation_policies.center_id IS '센터 ID (필수)';
COMMENT ON COLUMN escalation_policies.warehouse_id IS '창고 ID (NULL이면 센터 전체 정책)';
COMMENT ON COLUMN escalation_policies.alert_type IS '알림 유형 (TEMPERATURE, HUMIDITY, AIR_QUALITY 등)';
COMMENT ON COLUMN escalation_policies.active IS '정책 활성 여부';
COMMENT ON COLUMN escalation_policies.created_at IS '생성 시각 (UTC 기준 저장)';
COMMENT ON COLUMN escalation_policies.updated_at IS '수정 시각 (UTC 기준 저장)';

CREATE INDEX IF NOT EXISTS idx_escalation_policies_center_id
    ON escalation_policies (center_id);

CREATE INDEX IF NOT EXISTS idx_escalation_policies_warehouse_id
    ON escalation_policies (warehouse_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_escalation_policies_scope
    ON escalation_policies (center_id, warehouse_id, alert_type, active);

-- ============================================
-- 2. ESCALATION RULES TABLE
-- ============================================
CREATE TABLE escalation_rules (
    id BIGSERIAL PRIMARY KEY,
    policy_id BIGINT NOT NULL REFERENCES escalation_policies(id) ON DELETE CASCADE,
    level INTEGER NOT NULL CHECK (level BETWEEN 1 AND 3),
    delay_minutes INTEGER NOT NULL DEFAULT 0,
    notify_roles JSONB NOT NULL DEFAULT '[]',
    channels JSONB NOT NULL DEFAULT '["EMAIL"]',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_escalation_rules_policy_level UNIQUE (policy_id, level)
);

COMMENT ON TABLE escalation_rules IS '에스컬레이션 규칙 - 정책별 다단계 알림 규칙';
COMMENT ON COLUMN escalation_rules.id IS '에스컬레이션 규칙 PK';
COMMENT ON COLUMN escalation_rules.policy_id IS '소속 정책 ID';
COMMENT ON COLUMN escalation_rules.level IS '에스컬레이션 레벨 (1-3)';
COMMENT ON COLUMN escalation_rules.delay_minutes IS '이전 레벨 이후 대기 시간 (분)';
COMMENT ON COLUMN escalation_rules.notify_roles IS '알림 대상 역할 JSON 배열 (예: ["ROLE_CENTER_MANAGER"])';
COMMENT ON COLUMN escalation_rules.channels IS '알림 채널 JSON 배열 (예: ["EMAIL", "SMS"])';
COMMENT ON COLUMN escalation_rules.created_at IS '생성 시각 (UTC 기준 저장)';
COMMENT ON COLUMN escalation_rules.updated_at IS '수정 시각 (UTC 기준 저장)';

CREATE INDEX IF NOT EXISTS idx_escalation_rules_policy_id
    ON escalation_rules (policy_id);
