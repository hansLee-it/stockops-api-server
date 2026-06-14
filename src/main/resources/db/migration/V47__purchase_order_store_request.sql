-- Store-originated purchase requests.
-- A retail store creates a request with no center; an administrator approves it and designates
-- the requesting center + target warehouse (role-constrained by scope). Supplier-procurement
-- orders keep setting requesting_center_id at creation as before.
ALTER TABLE purchase_orders ADD COLUMN requesting_store_id BIGINT;
ALTER TABLE purchase_orders ADD CONSTRAINT fk_po_requesting_store
    FOREIGN KEY (requesting_store_id) REFERENCES stores(id);

-- requesting center is now assigned at approval for store requests, so it may be null initially
ALTER TABLE purchase_orders ALTER COLUMN requesting_center_id DROP NOT NULL;
