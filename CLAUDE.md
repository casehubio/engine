# CLAUDE.md

## No Migration Tooling

This project has no installed instances to migrate. Do not add:

- Flyway or Liquibase dependencies
- SQL migration files (`V*.sql`, `db/migration/` directories)
- `quarkus.flyway.*` or `quarkus.liquibase.*` properties
- JDBC-only dependencies (`quarkus-jdbc-postgresql`, `quarkus-agroal`) unless required for a non-migration reason

Schema is managed by Hibernate directly:
```properties
quarkus.hibernate-orm.schema-management.strategy=drop-and-create
```

If a schema change is needed, update the `@Entity` class. Hibernate recreates the schema on next startup.

## Persistence Architecture

Domain objects and SPI interfaces live in `engine-model` (no Quarkus, no JPA):

- `engine-model/src/main/java/io/casehub/engine/internal/model/` — `CaseMetaModel`, `CaseInstance`
- `engine-model/src/main/java/io/casehub/engine/internal/history/` — `EventLog`, `CaseHubEventType`, `EventStreamType`
- `engine-model/src/main/java/io/casehub/engine/spi/` — `CaseMetaModelRepository`, `CaseInstanceRepository`, `EventLogRepository`

Both `engine` and both persistence modules depend on `engine-model`. Neither persistence module depends on `engine`.

**Production implementation:** `casehub-persistence-hibernate` (JPA/Panache, PostgreSQL)
**Test implementation:** test-local copies in `engine/src/test/java/io/casehub/persistence/memory/`

Engine tests activate the memory implementations via `quarkus.arc.selected-alternatives`
in `engine/src/test/resources/application.properties` — no Docker required.

Domain objects (`CaseMetaModel`, `CaseInstance`, `EventLog`) are plain POJOs. The `id` field
is public (`public Long id`) and set by the repository after save.

## Engine Testing Without Docker

PR #86 added a `persistence-memory` Maven profile for running engine integration tests without Docker:

```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl engine \
  -Ppersistence-memory \
  -Dexcludes="**/SignalTest.java"
```

Without `-Ppersistence-memory`, `SignalDedupExtendedTest` and `WorkerScheduleDedupTest` fail at JUnit class discovery (not at test execution) with a `ClassSelector resolution failed` error — there is no clear indication that Docker is the root cause.

**Note on interface changes in `api/`:** After any rebase that modifies a public interface, run `mvn compile -q` across the full reactor before pushing. Tests scoped with `-pl api,engine` will not catch compilation failures in downstream modules such as `casehub-blackboard`.

## Quartz

Use RAM store — no JDBC store, no Quartz tables:
```properties
quarkus.quartz.store-type=ram
```
