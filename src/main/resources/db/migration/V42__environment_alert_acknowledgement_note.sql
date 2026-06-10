-- Administrators record their handling/action note when acknowledging an environment alert.
ALTER TABLE environment_alerts ADD COLUMN acknowledgement_note VARCHAR(1000);

COMMENT ON COLUMN environment_alerts.acknowledgement_note IS '관리자 처리내용 (확인/조치 시 입력)';
