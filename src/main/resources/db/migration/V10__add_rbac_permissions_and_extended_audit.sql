CREATE TABLE permissions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE role_permissions (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_role_permissions UNIQUE (role_id, permission_id)
);

ALTER TABLE audit_logs
    ALTER COLUMN entity_id DROP NOT NULL;

ALTER TABLE audit_logs
    ADD COLUMN target_identifier VARCHAR(255);

ALTER TABLE audit_logs
    ADD COLUMN performed_by_email VARCHAR(255);

INSERT INTO roles (name, description, created_at)
SELECT 'MANAGER', 'Operational manager', NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'MANAGER');

INSERT INTO roles (name, description, created_at)
SELECT 'STAFF', 'Operational staff', NOW()
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'STAFF');

INSERT INTO permissions (code, description)
SELECT source.code, source.description
FROM (VALUES
('AUDIT_LOG_READ', 'Read audit logs'),
('CENTER_CREATE', 'Create centers'),
('CENTER_DELETE', 'Delete centers'),
('CENTER_READ', 'Read centers'),
('CENTER_UPDATE', 'Update centers'),
('CYCLE_COUNT_CREATE', 'Create cycle counts'),
('CYCLE_COUNT_EXECUTE', 'Start and complete cycle counts'),
('CYCLE_COUNT_READ', 'Read cycle counts'),
('DASHBOARD_READ', 'Read dashboard data'),
('EXPIRY_ALERT_MANAGE', 'Manage expiry alerts and quarantine'),
('EXPIRY_ALERT_READ', 'Read expiry alerts'),
('INBOUND_CONFIRM', 'Confirm inbounds'),
('INBOUND_CREATE', 'Create inbounds'),
('INBOUND_READ', 'Read inbounds'),
('INVENTORY_ADJUST_APPROVE', 'Approve stock adjustments'),
('INVENTORY_ADJUST_CREATE', 'Create stock adjustments'),
('INVENTORY_ADJUST_READ', 'Read stock adjustments'),
('INVENTORY_READ', 'Read inventory'),
('LOCATION_CREATE', 'Create locations'),
('LOCATION_DELETE', 'Delete locations'),
('LOCATION_READ', 'Read locations'),
('LOCATION_UPDATE', 'Update locations'),
('OUTBOUND_CONFIRM', 'Confirm outbounds'),
('OUTBOUND_CREATE', 'Create outbounds'),
('OUTBOUND_READ', 'Read outbounds'),
('PRODUCT_CREATE', 'Create products'),
('PRODUCT_DELETE', 'Delete products'),
('PRODUCT_READ', 'Read products'),
('PRODUCT_UPDATE', 'Update products'),
('PURCHASE_ORDER_CREATE', 'Create purchase orders'),
('PURCHASE_ORDER_MANAGE', 'Manage purchase orders'),
('PURCHASE_ORDER_READ', 'Read purchase orders'),
('REASON_CODE_CREATE', 'Create reason codes'),
('REASON_CODE_DELETE', 'Delete reason codes'),
('REASON_CODE_READ', 'Read reason codes'),
('REASON_CODE_UPDATE', 'Update reason codes'),
('ROLE_CREATE', 'Create roles'),
('ROLE_DELETE', 'Delete roles'),
('ROLE_READ', 'Read roles'),
('ROLE_UPDATE', 'Update roles'),
('USER_CREATE', 'Create users'),
('USER_DELETE', 'Delete users'),
('USER_READ', 'Read users'),
('USER_UPDATE', 'Update users'),
('WAREHOUSE_CREATE', 'Create warehouses'),
('WAREHOUSE_DELETE', 'Delete warehouses'),
('WAREHOUSE_READ', 'Read warehouses'),
('WAREHOUSE_UPDATE', 'Update warehouses')
) AS source (code, description)
WHERE NOT EXISTS (
    SELECT 1 FROM permissions target WHERE target.code = source.code
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions existing
    WHERE existing.role_id = r.id
      AND existing.permission_id = p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'AUDIT_LOG_READ', 'CENTER_CREATE', 'CENTER_READ', 'CENTER_UPDATE',
    'CYCLE_COUNT_CREATE', 'CYCLE_COUNT_EXECUTE', 'CYCLE_COUNT_READ',
    'DASHBOARD_READ', 'EXPIRY_ALERT_MANAGE', 'EXPIRY_ALERT_READ',
    'INBOUND_CONFIRM', 'INBOUND_CREATE', 'INBOUND_READ',
    'INVENTORY_ADJUST_APPROVE', 'INVENTORY_ADJUST_CREATE', 'INVENTORY_ADJUST_READ',
    'INVENTORY_READ', 'LOCATION_CREATE', 'LOCATION_READ', 'LOCATION_UPDATE',
    'OUTBOUND_CONFIRM', 'OUTBOUND_CREATE', 'OUTBOUND_READ',
    'PRODUCT_CREATE', 'PRODUCT_READ', 'PRODUCT_UPDATE',
    'PURCHASE_ORDER_CREATE', 'PURCHASE_ORDER_MANAGE', 'PURCHASE_ORDER_READ',
    'REASON_CODE_READ', 'ROLE_READ', 'USER_READ', 'USER_UPDATE',
    'WAREHOUSE_CREATE', 'WAREHOUSE_READ', 'WAREHOUSE_UPDATE'
)
WHERE r.name = 'MANAGER'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions existing
    WHERE existing.role_id = r.id
      AND existing.permission_id = p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'CENTER_READ', 'CYCLE_COUNT_READ', 'DASHBOARD_READ', 'EXPIRY_ALERT_READ',
    'INBOUND_CREATE', 'INBOUND_READ', 'INVENTORY_ADJUST_CREATE', 'INVENTORY_ADJUST_READ',
    'INVENTORY_READ', 'LOCATION_READ', 'OUTBOUND_CREATE', 'OUTBOUND_READ',
    'PRODUCT_READ', 'PURCHASE_ORDER_READ', 'REASON_CODE_READ', 'WAREHOUSE_READ'
)
WHERE r.name IN ('USER', 'STAFF')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions existing
    WHERE existing.role_id = r.id
      AND existing.permission_id = p.id
);
