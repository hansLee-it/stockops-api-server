-- V29: Category hierarchy for product classification
-- Self-referencing 3-level category (대분류 → 중분류 → 소분류)

-- ============================================
-- 1. CATEGORIES TABLE
-- ============================================
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    parent_id BIGINT,
    level INTEGER NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id)
);

COMMENT ON TABLE categories IS '상품 분류 계층 - 3단계 분류 (대분류/중분류/소분류)';
COMMENT ON COLUMN categories.id IS '카테고리 PK';
COMMENT ON COLUMN categories.name IS '카테고리 이름';
COMMENT ON COLUMN categories.code IS '카테고리 코드 (고유)';
COMMENT ON COLUMN categories.parent_id IS '상위 카테고리 ID (NULL이면 최상위)';
COMMENT ON COLUMN categories.level IS '레벨 (1=대분류, 2=중분류, 3=소분류)';
COMMENT ON COLUMN categories.sort_order IS '정렬 순서';
COMMENT ON COLUMN categories.active IS '활성 상태';
COMMENT ON COLUMN categories.created_at IS '생성 시각 (UTC)';
COMMENT ON COLUMN categories.updated_at IS '수정 시각 (UTC)';

CREATE INDEX IF NOT EXISTS idx_categories_parent ON categories (parent_id);
CREATE INDEX IF NOT EXISTS idx_categories_level ON categories (level);
CREATE INDEX IF NOT EXISTS idx_categories_active ON categories (active);
CREATE INDEX IF NOT EXISTS idx_categories_code ON categories (code);

-- ============================================
-- 2. ADD CATEGORY_ID TO PRODUCTS
-- ============================================
ALTER TABLE products ADD COLUMN IF NOT EXISTS category_id BIGINT;

COMMENT ON COLUMN products.category_id IS '상품 카테고리 FK (nullable - 기존 상품은 카테고리 없음)';

CREATE INDEX IF NOT EXISTS idx_products_category ON products (category_id);

-- ============================================
-- 3. SEED DATA - 3 LEVELS OF CATEGORIES
-- ============================================
INSERT INTO categories (name, code, parent_id, level, sort_order, active)
VALUES
    -- Level 1: 대분류 (Root categories)
    ('식품', 'FOOD', NULL, 1, 1, true),
    ('화장품', 'COSMETIC', NULL, 1, 2, true),
    ('생활용품', 'HOUSEHOLD', NULL, 1, 3, true);

-- Level 2: 중분류 (Children of FOOD)
INSERT INTO categories (name, code, parent_id, level, sort_order, active)
VALUES
    ('과자', 'FOOD_SNACK', (SELECT id FROM categories WHERE code = 'FOOD'), 2, 1, true),
    ('음료', 'FOOD_DRINK', (SELECT id FROM categories WHERE code = 'FOOD'), 2, 2, true),
    ('편의식', 'FOOD_CONVENIENCE', (SELECT id FROM categories WHERE code = 'FOOD'), 2, 3, true);

-- Level 2: 중분류 (Children of COSMETIC)
INSERT INTO categories (name, code, parent_id, level, sort_order, active)
VALUES
    ('스킨케어', 'COSMETIC_SKIN', (SELECT id FROM categories WHERE code = 'COSMETIC'), 2, 1, true),
    ('메이크업', 'COSMETIC_MAKEUP', (SELECT id FROM categories WHERE code = 'COSMETIC'), 2, 2, true),
    ('헤어케어', 'COSMETIC_HAIR', (SELECT id FROM categories WHERE code = 'COSMETIC'), 2, 3, true);

-- Level 2: 중분류 (Children of HOUSEHOLD)
INSERT INTO categories (name, code, parent_id, level, sort_order, active)
VALUES
    ('세제', 'HOUSEHOLD_DETERGENT', (SELECT id FROM categories WHERE code = 'HOUSEHOLD'), 2, 1, true),
    ('청소용품', 'HOUSEHOLD_CLEANING', (SELECT id FROM categories WHERE code = 'HOUSEHOLD'), 2, 2, true),
    ('생활잡화', 'HOUSEHOLD_MISC', (SELECT id FROM categories WHERE code = 'HOUSEHOLD'), 2, 3, true);

-- Level 3: 소분류 (Children of FOOD_SNACK)
INSERT INTO categories (name, code, parent_id, level, sort_order, active)
VALUES
    ('스낵', 'FOOD_SNACK_SNACK', (SELECT id FROM categories WHERE code = 'FOOD_SNACK'), 3, 1, true),
    ('초콜릿', 'FOOD_SNACK_CHOCOLATE', (SELECT id FROM categories WHERE code = 'FOOD_SNACK'), 3, 2, true),
    ('캔디', 'FOOD_SNACK_CANDY', (SELECT id FROM categories WHERE code = 'FOOD_SNACK'), 3, 3, true);

-- Level 3: 소분류 (Children of FOOD_DRINK)
INSERT INTO categories (name, code, parent_id, level, sort_order, active)
VALUES
    ('탄산음료', 'FOOD_DRINK_SODA', (SELECT id FROM categories WHERE code = 'FOOD_DRINK'), 3, 1, true),
    ('주스', 'FOOD_DRINK_JUICE', (SELECT id FROM categories WHERE code = 'FOOD_DRINK'), 3, 2, true),
    ('생수', 'FOOD_DRINK_WATER', (SELECT id FROM categories WHERE code = 'FOOD_DRINK'), 3, 3, true),
    ('커피', 'FOOD_DRINK_COFFEE', (SELECT id FROM categories WHERE code = 'FOOD_DRINK'), 3, 4, true);

-- Level 3: 소분류 (Children of COSMETIC_SKIN)
INSERT INTO categories (name, code, parent_id, level, sort_order, active)
VALUES
    ('토너', 'COSMETIC_SKIN_TONER', (SELECT id FROM categories WHERE code = 'COSMETIC_SKIN'), 3, 1, true),
    ('에센스', 'COSMETIC_SKIN_ESSENCE', (SELECT id FROM categories WHERE code = 'COSMETIC_SKIN'), 3, 2, true),
    ('크림', 'COSMETIC_SKIN_CREAM', (SELECT id FROM categories WHERE code = 'COSMETIC_SKIN'), 3, 3, true),
    ('마스크', 'COSMETIC_SKIN_MASK', (SELECT id FROM categories WHERE code = 'COSMETIC_SKIN'), 3, 4, true);

-- Level 3: 소분류 (Children of HOUSEHOLD_DETERGENT)
INSERT INTO categories (name, code, parent_id, level, sort_order, active)
VALUES
    ('세탁세제', 'HOUSEHOLD_DETERGENT_LAUNDRY', (SELECT id FROM categories WHERE code = 'HOUSEHOLD_DETERGENT'), 3, 1, true),
    ('주방세제', 'HOUSEHOLD_DETERGENT_KITCHEN', (SELECT id FROM categories WHERE code = 'HOUSEHOLD_DETERGENT'), 3, 2, true),
    ('샴푸', 'HOUSEHOLD_DETERGENT_SHAMPOO', (SELECT id FROM categories WHERE code = 'HOUSEHOLD_DETERGENT'), 3, 3, true);