-- V49: Drop email/SMS notification channels from existing configuration rows.
--
-- The email- and SMS-sending features were removed; webhook (e.g. Teams) is now the only
-- supported delivery channel. This sanitizes already-persisted JSONB so the reduced
-- ChannelType enum (WEBHOOK only) and the escalation dispatcher no longer encounter
-- EMAIL/SMS values. Non-destructive: only the affected rows are rewritten, and the
-- sms_send_history table is intentionally left in place (drop it separately if desired).
--
-- PostgreSQL-only: relies on jsonb_array_elements/jsonb_agg, which the H2 test database
-- does not provide, so this lives in the vendor-specific Flyway location. H2 starts from a
-- clean schema each run and never holds legacy EMAIL/SMS rows, so it does not need this.

-- notification_channel_configs.channels is a JSONB array of objects
-- ({"type": "...", "enabled": ..., "webhookProvider": ...}); keep only WEBHOOK entries.
UPDATE notification_channel_configs
SET channels = COALESCE(
        (SELECT jsonb_agg(elem)
         FROM jsonb_array_elements(channels) AS elem
         WHERE elem->>'type' = 'WEBHOOK'),
        '[]'::jsonb)
WHERE channels @> '[{"type": "EMAIL"}]'::jsonb
   OR channels @> '[{"type": "SMS"}]'::jsonb;

-- escalation_rules.channels is a JSONB array of channel-name strings (e.g. ["EMAIL","SMS"]);
-- strip the removed channel names.
UPDATE escalation_rules
SET channels = COALESCE(
        (SELECT jsonb_agg(elem)
         FROM jsonb_array_elements(channels) AS elem
         WHERE elem NOT IN ('"EMAIL"'::jsonb, '"SMS"'::jsonb)),
        '[]'::jsonb)
WHERE channels @> '"EMAIL"'::jsonb
   OR channels @> '"SMS"'::jsonb;

COMMENT ON COLUMN notification_channel_configs.channels
    IS '채널 설정 JSON 배열 (예: [{"type":"WEBHOOK","enabled":true,"webhookProvider":"TEAMS"}])';
COMMENT ON COLUMN escalation_rules.channels
    IS '알림 채널 JSON 배열 (예: ["WEBHOOK"])';
