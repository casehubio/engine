# CLAUDE.md

## Schema Management

This project has no installed instances to migrate between versions. Use a single SQL schema file — not incremental versioned migrations.

**Do not add:**
- Multiple Flyway migration files (`V1.0__x.sql`, `V1.1__x.sql`, etc.)
- Any `ALTER TABLE` or `UPDATE` migration scripts
- `quarkus.flyway.baseline-*` properties

**Do:**
- Keep all DDL in `casehub-persistence-hibernate/src/main/resources/db/migration/V1__schema.sql`
- If the schema changes, update `V1__schema.sql` directly (it runs on a fresh database)
- Use `quarkus.flyway.migrate-at-start=true` in `casehub-persistence-hibernate`

## Quartz

Quartz uses JDBC store in production (`quarkus.quartz.store-type=jdbc-cmt`). The Quartz tables are in `V1__schema.sql`.

In engine **tests only**, use RAM store to avoid needing Quartz tables:
```properties
%test.quarkus.quartz.store-type=ram
```

## Datasource Config

Production datasource config (both reactive and JDBC URLs) lives in `casehub-persistence-hibernate/src/main/resources/application.properties`. The engine only holds the reactive URL.
