-- V32: AI model evaluation tracking table
-- Stores forecast accuracy metrics (MAE, RMSE, MAPE) per product and model version

CREATE TABLE analytics.ai_model_evaluations (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL,
    mae             NUMERIC(12, 4)  NOT NULL,
    rmse            NUMERIC(12, 4)  NOT NULL,
    mape            NUMERIC(12, 4)  NOT NULL,
    evaluated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    model_version   VARCHAR(50)     NOT NULL DEFAULT 'prophet',
    CONSTRAINT fk_ai_model_evaluations_product FOREIGN KEY (product_id) REFERENCES public.products(id)
);

COMMENT ON TABLE analytics.ai_model_evaluations IS 'AI 모델 성능 평가 결과 - 상품별/모델버전별 예측 정확도 지표';
COMMENT ON COLUMN analytics.ai_model_evaluations.id IS '평가 결과 PK';
COMMENT ON COLUMN analytics.ai_model_evaluations.product_id IS '상품 ID (필수)';
COMMENT ON COLUMN analytics.ai_model_evaluations.mae IS 'Mean Absolute Error (평균절대오차)';
COMMENT ON COLUMN analytics.ai_model_evaluations.rmse IS 'Root Mean Squared Error (평균제곱근오차)';
COMMENT ON COLUMN analytics.ai_model_evaluations.mape IS 'Mean Absolute Percentage Error (%) (평균절대백분율오차)';
COMMENT ON COLUMN analytics.ai_model_evaluations.evaluated_at IS '평가 시각 (UTC 기준 저장)';
COMMENT ON COLUMN analytics.ai_model_evaluations.model_version IS '평가된 모델 버전 (예: prophet, statistical)';

CREATE INDEX idx_ai_model_evaluations_product_id
    ON analytics.ai_model_evaluations (product_id);
CREATE INDEX idx_ai_model_evaluations_evaluated_at
    ON analytics.ai_model_evaluations (evaluated_at DESC);
CREATE INDEX idx_ai_model_evaluations_model_version
    ON analytics.ai_model_evaluations (model_version);
