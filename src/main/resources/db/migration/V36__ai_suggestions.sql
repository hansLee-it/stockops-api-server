CREATE TABLE analytics.ai_suggestions (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(100) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    title VARCHAR(200),
    summary VARCHAR(500),
    reason TEXT,
    recommended_action TEXT,
    target_type VARCHAR(100),
    target_id BIGINT,
    target_scope_type VARCHAR(50) NOT NULL,
    target_scope_id BIGINT NOT NULL,
    payload_json JSONB NOT NULL DEFAULT '{}',
    confidence_score DOUBLE PRECISION,
    source VARCHAR(100) NOT NULL,
    source_type VARCHAR(100) NOT NULL,
    created_by_user_id BIGINT,
    created_from_app VARCHAR(100),
    forecast_source_type VARCHAR(100),
    forecast_source_id BIGINT,
    forecast_model_version VARCHAR(100),
    forecast_generated_at TIMESTAMP WITH TIME ZONE,
    forecast_source_payload_json JSONB,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    visible_to_app VARCHAR(50) NOT NULL,
    approval_mode VARCHAR(100) NOT NULL,
    requested_on_behalf_user_id BIGINT,
    requested_scope_type VARCHAR(50),
    requested_scope_id BIGINT,
    expires_at TIMESTAMP WITH TIME ZONE,
    reviewed_by_user_id BIGINT,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    approved_by_user_id BIGINT,
    approved_at TIMESTAMP WITH TIME ZONE,
    executed_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    execution_result JSONB,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_suggestion_status
    ON analytics.ai_suggestions (status, id);

CREATE INDEX idx_ai_suggestion_scope
    ON analytics.ai_suggestions (target_scope_type, target_scope_id, status, id);
