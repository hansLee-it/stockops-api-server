# Database Migrations (Flyway)

Schema for StockOps is managed by [Flyway](https://flywaydb.org/). Migrations run automatically
on application startup (`spring.flyway.enabled=true`). This document is the policy for adding and
maintaining them. It is **not** a migration itself — Flyway only loads `V*.sql` files, so this
`.md` is ignored by the migrator.

## Layout

```
src/main/resources/db/
  migration/                 # vendor-neutral migrations (run on every database, incl. H2 tests)
    V1__init_schema.sql
    ...
    V52__role_webhook_and_notice_target_roles.sql
  vendor/postgresql/         # PostgreSQL-only DDL (skipped on H2)
    V44__active_alert_unique_index.sql
    V49__remove_email_sms_channels.sql
```

Configured locations (`application.yml`):

```yaml
spring.flyway:
  locations: classpath:db/migration,classpath:db/vendor/{vendor}
  fail-on-missing-locations: false
```

- `{vendor}` resolves to the active database: `postgresql` in dev/prod, `h2` in `local`/test.
- There is no `db/vendor/h2` folder, so `fail-on-missing-locations: false` lets the H2 test boot
  skip it cleanly while still applying everything under `db/migration`.
- **All versions share one history line.** `V44` and `V49` live under `vendor/postgresql` only
  because their DDL is Postgres-specific (e.g. partial / `CONCURRENTLY` indexes, vendor casts) and
  would break on H2. The numbering across `migration/` and `vendor/postgresql/` is still strictly
  sequential and must not collide.

## Naming & versioning

- File name: `V<n>__<snake_case_description>.sql` (double underscore after the version).
- `<n>` is a single, strictly increasing integer. Current history is **V1–V52, contiguous**.
- **Next free version: `V53`.** Check the highest existing version (in both `migration/` and
  `vendor/postgresql/`) before adding one — do not reuse or skip numbers.
- Put new DDL/DML under `migration/` unless it is Postgres-specific; only then use
  `vendor/postgresql/`.

## The immutability rule (read this before "cleaning up")

**Never edit, rename, move, or delete a migration that has already been applied to any
environment.** Flyway records a checksum per applied migration in `flyway_schema_history` and
validates it on every startup. Changing an applied file causes existing dev/prod databases to fail
to boot with `Migration checksum mismatch` or `Detected applied migration not resolved locally`.

This applies even to cosmetic changes (a comment, whitespace, a rename). To change the schema,
**add a new migration** — never alter an old one.

Consequences for "consolidation":
- Squashing / merging / deleting historical migrations is **not safe** while those versions are
  applied somewhere (dev/prod). It is not a tidy-up; it breaks every provisioned database.
- The only safe way to collapse history is a **Flyway baseline**: generate a single squashed
  schema as a new baseline for *fresh* installs, set `baselineVersion`, and keep existing
  environments on their recorded history. This is a deliberate, separately-reviewed operation —
  only worth it when fresh-provision time is a real problem. (At ~50 migrations that apply in
  ~1–2s on an empty database, it currently is not.)
- Corrective migrations in this history (e.g. `V15`/`V17` adding a missing `updated_at`, `V35`
  normalizing enum casing, `V39` dropping deprecated tables) are normal and expected — they are
  not "cruft" to remove.

## Authoring conventions

- **Make migrations idempotent / re-runnable-safe** where practical, matching the existing style:
  - Seed rows: `INSERT INTO t (...) SELECT ... WHERE NOT EXISTS (SELECT 1 FROM t WHERE ...)`.
  - Columns/objects: `ADD COLUMN IF NOT EXISTS`, `DROP ... IF EXISTS`,
    `DROP CONSTRAINT IF EXISTS` before re-adding.
- Use `TIMESTAMP WITH TIME ZONE` for timestamps; every table backing a `BaseEntity` subclass needs
  `created_at` and `updated_at` (a recurring source of the early `fix_*` migrations).
- Keep vendor-specific syntax out of `db/migration/` so the H2 test schema stays buildable.

## What is NOT seeded by migrations

Schema, roles, and permissions are seeded via migrations. **Application accounts and dev/demo
fixtures are not** — they are seeded at runtime by `AuthDataLoader`
(`com.stockops.auth.AuthDataLoader`):

- The `admin@stockops.com` account is always created.
- Per-role test accounts **and ~45 sample stores** are created only when
  `stockops.test-accounts.password` is set (dev/demo); production leaves it blank and seeds
  neither. The two store-role accounts are linked to a sample store there.

Passwords are BCrypt-hashed at runtime, which is why these live in `AuthDataLoader` rather than in
a migration.
