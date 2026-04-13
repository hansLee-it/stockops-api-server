-- V8: Warehouse query indexes
-- Adds a composite index to optimize warehouse lookups by center and status.

CREATE INDEX idx_warehouses_center_id_status ON warehouses(center_id, status);
