ALTER TABLE analytics.daily_fill_rate_source
    ADD COLUMN IF NOT EXISTS shipped_quantity INTEGER NOT NULL DEFAULT 0;
