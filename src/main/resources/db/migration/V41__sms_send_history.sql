-- Backfills the missing CREATE TABLE for the SmsSendHistory JPA entity (schema mismatch fix).
CREATE TABLE IF NOT EXISTS sms_send_history (
    id BIGSERIAL PRIMARY KEY,
    message_id VARCHAR(100),
    phone_number VARCHAR(20) NOT NULL,
    message VARCHAR(1600) NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message VARCHAR(500),
    attempt_count INTEGER NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sms_history_message_id UNIQUE (message_id)
);

CREATE INDEX IF NOT EXISTS idx_sms_send_history_phone_created_at
    ON sms_send_history (phone_number, created_at DESC);
