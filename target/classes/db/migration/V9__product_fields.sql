ALTER TABLE products
    ADD COLUMN default_price DECIMAL(12,2) NOT NULL DEFAULT 0;

ALTER TABLE products
    ADD COLUMN safety_stock_quantity INTEGER NOT NULL DEFAULT 0;

UPDATE products
SET default_price = 0,
    safety_stock_quantity = 0
WHERE default_price IS NULL
   OR safety_stock_quantity IS NULL;
