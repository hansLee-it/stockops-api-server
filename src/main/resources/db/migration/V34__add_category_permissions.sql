INSERT INTO permissions (code, description)
SELECT source.code, source.description
FROM (VALUES
    ('CATEGORY_CREATE', 'Create categories'),
    ('CATEGORY_DELETE', 'Delete categories'),
    ('CATEGORY_READ', 'Read categories'),
    ('CATEGORY_UPDATE', 'Update categories')
) AS source (code, description)
WHERE NOT EXISTS (
    SELECT 1 FROM permissions target WHERE target.code = source.code
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND p.code IN ('CATEGORY_CREATE', 'CATEGORY_DELETE', 'CATEGORY_READ', 'CATEGORY_UPDATE')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions existing
      WHERE existing.role_id = r.id
        AND existing.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('CATEGORY_CREATE', 'CATEGORY_READ', 'CATEGORY_UPDATE')
WHERE r.name = 'MANAGER'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions existing
      WHERE existing.role_id = r.id
        AND existing.permission_id = p.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'CATEGORY_READ'
WHERE r.name IN ('USER', 'STAFF')
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions existing
      WHERE existing.role_id = r.id
        AND existing.permission_id = p.id
  );
