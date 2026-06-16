-- Intraday forecast proposal runs.
-- Unlike the once-daily analytics.ai_reorder_recommendations table (latest-only, upserted per
-- business_date), this table ACCUMULATES one proposal row per (business_date, run_slot) so the
-- intraday scheduler can record how a scope's reorder proposal shifts across the day.
-- run_slot is the scheduled hour-of-day in the business zone (e.g. 10, 15).
-- A proposal stays actionable (approve/reject) only until actionable_until (run_at + N days);
-- afterwards it remains as history but can no longer be approved or rejected.

CREATE TABLE analytics.forecast_proposal_runs (
    id BIGSERIAL PRIMARY KEY,
    business_date DATE NOT NULL,
    run_slot INTEGER NOT NULL,
    run_at TIMESTAMP WITH TIME ZONE NOT NULL,
    actionable_until TIMESTAMP WITH TIME ZONE NOT NULL,
    product_id BIGINT NOT NULL,
    center_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    current_stock_quantity INTEGER NOT NULL DEFAULT 0,
    safety_stock_quantity INTEGER NOT NULL DEFAULT 0,
    recommended_quantity INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    trailing_seven_day_average NUMERIC(12, 2) NOT NULL DEFAULT 0,
    same_weekday_average NUMERIC(12, 2) NOT NULL DEFAULT 0,
    weighted_daily_demand NUMERIC(12, 2) NOT NULL DEFAULT 0,
    seven_day_forecast_quantity INTEGER NOT NULL DEFAULT 0,
    lead_time_days INTEGER NOT NULL DEFAULT 1,
    lead_time_demand_quantity INTEGER NOT NULL DEFAULT 0,
    demand_event_count INTEGER NOT NULL DEFAULT 0,
    model_version VARCHAR(100),
    explanation_summary VARCHAR(500),
    approved_purchase_order_id BIGINT REFERENCES public.purchase_orders(id),
    approved_by_user_id BIGINT REFERENCES public.users(id),
    approved_at TIMESTAMP WITH TIME ZONE,
    rejected_by_user_id BIGINT REFERENCES public.users(id),
    rejected_at TIMESTAMP WITH TIME ZONE,
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_forecast_proposal_run_scope UNIQUE (business_date, run_slot, product_id, center_id, warehouse_id)
);

CREATE INDEX idx_forecast_proposal_run_lookup
    ON analytics.forecast_proposal_runs (business_date, run_slot, center_id, warehouse_id, product_id);

CREATE INDEX idx_forecast_proposal_run_actionable
    ON analytics.forecast_proposal_runs (status, actionable_until);
