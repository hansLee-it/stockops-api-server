-- Notification outbox for environment alerts.
-- Telemetry ingestion only inserts PENDING rows; a scheduled sender claims them with
-- FOR UPDATE SKIP LOCKED and delivers webhook/email, so duplicate notifications cannot
-- be produced by concurrent ingestion instances and failed deliveries are retried.
CREATE TABLE environment_alert_notifications (
    id              BIGSERIAL PRIMARY KEY,
    alert_id        BIGINT NOT NULL REFERENCES environment_alerts(id) ON DELETE CASCADE,
    trigger_type    VARCHAR(20) NOT NULL,
    severity        VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count   INT NOT NULL DEFAULT 0,
    last_error      VARCHAR(500),
    sent_at         TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE environment_alert_notifications IS '환경 알림 발송 아웃박스 (PENDING 행을 주기 발송기가 SKIP LOCKED로 집어 발송)';
COMMENT ON COLUMN environment_alert_notifications.trigger_type IS 'OPENED | ESCALATED';
COMMENT ON COLUMN environment_alert_notifications.status IS 'PENDING | SENT | FAILED | SUPERSEDED';

CREATE INDEX idx_alert_notifications_pending
    ON environment_alert_notifications (status, created_at);
