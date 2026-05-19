ALTER TABLE products
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE products
    DROP CONSTRAINT IF EXISTS products_barcode_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_products_barcode_active
    ON products (barcode, deleted);

CREATE INDEX IF NOT EXISTS idx_products_deleted
    ON products (deleted);
