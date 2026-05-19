-- V14: Environment Monitoring Permissions
-- 환경 모니터링 관련 권한 추가 및 역할 할당

-- ============================================
-- 1. ENVIRONMENT PERMISSIONS
-- ============================================
INSERT INTO permissions (code, description)
SELECT source.code, source.description
FROM (VALUES
('ENVIRONMENT_READ', 'Read environment monitoring data (sensors, controllers, dashboard, alerts, history)'),
('ENVIRONMENT_MANAGE', 'Manage environment sensors and controllers (create, update, delete)'),
('ENVIRONMENT_COMMAND', 'Send commands to environment controllers')
) AS source (code, description)
WHERE NOT EXISTS (
    SELECT 1 FROM permissions target WHERE target.code = source.code
);

-- ============================================
-- 2. ADMIN ROLE GETS ALL ENVIRONMENT PERMISSIONS
-- ============================================
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

-- ============================================
-- 3. MANAGER ROLE GETS READ + MANAGE + COMMAND
-- ============================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'ENVIRONMENT_READ',
    'ENVIRONMENT_MANAGE',
    'ENVIRONMENT_COMMAND'
)
WHERE r.name = 'MANAGER'
AND NOT EXISTS (
    SELECT 1 FROM role_permissions existing
    WHERE existing.role_id = r.id
      AND existing.permission_id = p.id
);

-- ============================================
-- 4. USER/STAFF ROLE GETS READ ONLY
-- ============================================
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('ENVIRONMENT_READ')
WHERE r.name IN ('USER', 'STAFF')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions existing
    WHERE existing.role_id = r.id
      AND existing.permission_id = p.id
);
