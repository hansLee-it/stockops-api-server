DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'analytics'
          AND table_name = 'daily_fill_rate_source'
          AND column_name = 'shipped_quantity'
    ) THEN
        ALTER TABLE analytics.daily_fill_rate_source
            ADD COLUMN shipped_quantity INTEGER NOT NULL DEFAULT 0;
    END IF;
END $$;
