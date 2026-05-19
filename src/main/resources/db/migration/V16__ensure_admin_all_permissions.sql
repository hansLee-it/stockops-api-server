-- V16: Ensure ADMIN role has all permissions including ENVIRONMENT_*
-- This migration ensures ADMIN always has all permissions regardless of when they were added
-- Uses a simpler approach without CROSS JOIN to avoid any timing issues

-- 1. First ensure ADMIN has all permissions that exist at migration time
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

-- 2. Verify ENVIRONMENT_* permissions exist, insert if not
INSERT INTO permissions (code, description)
SELECT source.code, source.description
FROM (VALUES
('ENVIRONMENT_READ', 'Read environment monitoring data'),
('ENVIRONMENT_MANAGE', 'Manage environment sensors and controllers'),
('ENVIRONMENT_COMMAND', 'Send commands to environment controllers')
) AS source (code, description)
WHERE NOT EXISTS (
    SELECT 1 FROM permissions target WHERE target.code = source.code
);

-- 3. Now assign ENVIRONMENT_* to ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('ENVIRONMENT_READ', 'ENVIRONMENT_MANAGE', 'ENVIRONMENT_COMMAND')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions existing
    WHERE existing.role_id = r.id
      AND existing.permission_id = p.id
);

-- 4. Also grant all permissions to MANAGER role for environment
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'MANAGER'
  AND p.code IN ('ENVIRONMENT_READ', 'ENVIRONMENT_MANAGE', 'ENVIRONMENT_COMMAND')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions existing
    WHERE existing.role_id = r.id
      AND existing.permission_id = p.id
);
