ALTER TABLE analytics.ai_forecast_snapshots
    ADD COLUMN IF NOT EXISTS model_version VARCHAR(50) NOT NULL DEFAULT 'statistical';
