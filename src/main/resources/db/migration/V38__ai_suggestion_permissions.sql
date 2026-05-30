INSERT INTO permissions (code, description)
SELECT source.code, source.description
FROM (VALUES
    ('AI_SUGGESTION_READ', 'Read AI suggestions'),
    ('AI_SUGGESTION_CREATE', 'Create AI suggestions'),
    ('AI_SUGGESTION_APPROVE', 'Approve AI suggestions'),
    ('AI_SUGGESTION_REJECT', 'Reject AI suggestions'),
    ('AI_SUGGESTION_EXECUTE', 'Execute approved AI suggestions')
) AS source (code, description)
WHERE NOT EXISTS (
    SELECT 1 FROM permissions target WHERE target.code = source.code
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'AI_SUGGESTION_READ',
    'AI_SUGGESTION_CREATE',
    'AI_SUGGESTION_APPROVE',
    'AI_SUGGESTION_REJECT',
    'AI_SUGGESTION_EXECUTE'
)
WHERE r.name IN ('ADMIN', 'MANAGER')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions existing
    WHERE existing.role_id = r.id
      AND existing.permission_id = p.id
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('AI_SUGGESTION_READ', 'AI_SUGGESTION_CREATE')
WHERE r.name IN ('USER', 'STAFF')
AND NOT EXISTS (
    SELECT 1 FROM role_permissions existing
    WHERE existing.role_id = r.id
      AND existing.permission_id = p.id
);
