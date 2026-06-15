-- Demo backfill: assign every sensor and controller to a warehouse so they show up
-- under a warehouse in the demo. NOT a Flyway migration (one-off demo data only) — run it
-- manually on the demo database AFTER V50/V51 have applied.
--
-- Strategy (per request): put ALL devices on the first warehouse, then move a handful onto
-- the second warehouse (if one exists) so the demo shows more than one populated warehouse.
-- Distribution is arbitrary by design — for the demo it only needs to be visible somewhere.
-- Idempotent: only touches rows still missing a warehouse / the chosen demo subset.

-- 1) Everything not yet mapped -> the lowest-id warehouse.
UPDATE sensor_devices
SET warehouse_id = (SELECT id FROM warehouses ORDER BY id LIMIT 1)
WHERE warehouse_id IS NULL;

UPDATE environment_controllers
SET warehouse_id = (SELECT id FROM warehouses ORDER BY id LIMIT 1)
WHERE warehouse_id IS NULL;

-- 2) Move a few onto the second warehouse (only if a second warehouse exists) for variety.
--    Adjust the count / picked ids to taste; this just grabs the 3 lowest-id devices.
UPDATE sensor_devices
SET warehouse_id = (SELECT id FROM warehouses ORDER BY id OFFSET 1 LIMIT 1)
WHERE (SELECT count(*) FROM warehouses) > 1
  AND id IN (SELECT id FROM sensor_devices ORDER BY id LIMIT 3);

UPDATE environment_controllers
SET warehouse_id = (SELECT id FROM warehouses ORDER BY id OFFSET 1 LIMIT 1)
WHERE (SELECT count(*) FROM warehouses) > 1
  AND id IN (SELECT id FROM environment_controllers ORDER BY id LIMIT 2);

-- Verify:
-- SELECT warehouse_id, count(*) FROM sensor_devices GROUP BY warehouse_id;
-- SELECT warehouse_id, count(*) FROM environment_controllers GROUP BY warehouse_id;
