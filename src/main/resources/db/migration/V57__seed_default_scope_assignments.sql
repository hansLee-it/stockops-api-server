-- Backfill for the fail-closed scope change: ScopeAccessService now treats "no scope
-- assignments" as "no data visibility" (previously it meant global visibility), with the
-- ADMIN role as the only implicit-global exception.
--
-- To preserve current behavior for existing accounts, every non-ADMIN user that has no
-- effective assignment yet (neither directly nor via their role) receives an explicit
-- user-level GLOBAL assignment. Narrowing a user later is a matter of replacing that one
-- row with CENTER/WAREHOUSE assignments — deliberately user-level rather than role-level,
-- so narrowing one user never requires touching a shared role.
INSERT INTO user_scope_assignments (user_id, scope_type, center_id, warehouse_id)
SELECT u.id, 'GLOBAL', NULL, NULL
FROM users u
JOIN roles r ON r.id = u.role_id
WHERE r.name <> 'ADMIN'
  AND NOT EXISTS (
      SELECT 1 FROM user_scope_assignments usa WHERE usa.user_id = u.id
  )
  AND NOT EXISTS (
      SELECT 1 FROM role_scope_assignments rsa WHERE rsa.role_id = u.role_id
  );
