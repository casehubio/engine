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

## Worker Provisioner SPIs

Eight interfaces in `api/src/main/java/io/casehub/api/spi/` (four blocking + four reactive mirrors):

- `WorkerProvisioner` / `ReactiveWorkerProvisioner` — provision and terminate workers
- `WorkerStatusListener` / `ReactiveWorkerStatusListener` — lifecycle callbacks (started, completed, stalled)
- `CaseChannelProvider` / `ReactiveCaseChannelProvider` — open/close/post to backend-agnostic channels
- `WorkerContextProvider` / `ReactiveWorkerContextProvider` — build startup context from ledger lineage

**Default implementations** in `engine/src/main/java/io/casehub/engine/internal/worker/`:
- `NoOpWorkerProvisioner`, `NoOpWorkerStatusListener`, `NoOpCaseChannelProvider`, `EmptyWorkerContextProvider`
- Four `@Alternative` reactive mirrors for optional reactive pipeline use

**SPI placement rule:** Operational SPIs (worker provisioning, lifecycle, channels) go in `api/spi/`; persistence SPIs (`CaseMetaModelRepository`, etc.) go in `engine-model/spi/`. This clarifies intent: operational SPIs are about external system integration; persistence SPIs are about data durability.

To add a new operational SPI: define the interface in `api/spi/`, add a no-op default in `engine/internal/worker/`, add contract tests in `api/src/test/java/io/casehub/api/spi/`, and add engine unit tests in `engine/src/test/java/io/casehub/engine/internal/worker/DefaultWorkerSpiImplementationsTest.java`.

**To test SPI wiring:** use `@Alternative @Priority(1) @ApplicationScoped` static inner classes in `@QuarkusTest` with `static` recording fields reset in `@BeforeEach`. This activates the recording bean globally across the test suite without Mockito. See `SpiWiringIntegrationTest` for the pattern.

## casehub-blackboard Module

Optional CMMN/Blackboard orchestration layer. Activated via CDI `@Alternative @Priority(10)` when on the classpath.

**Build and test:**
```bash
mvn install -DskipTests -q          # install deps to local repo first
TESTCONTAINERS_RYUK_DISABLED=true mvn clean test -pl casehub-blackboard
```

**Test conventions:**
- `@QuarkusTest` classes MUST be named `*Test.java` — never `*IT.java`
  (`*IT` is picked up by failsafe instead of surefire; produces `Tests run: 0` with no error)
- In-memory SPI implementations for `@QuarkusTest` are copied into
  `casehub-blackboard/src/test/java/io/casehub/persistence/memory/` (same pattern as engine)
- `src/test/resources/application.properties` sets `quarkus.http.test-port=0` and activates
  the in-memory alternatives via `quarkus.arc.selected-alternatives`

## Quartz

Use RAM store — no JDBC store, no Quartz tables:
```properties
quarkus.quartz.store-type=ram
```

## Ecosystem Conventions

All casehubio projects align on these conventions:

**Quarkus version:** `version.quarkus.platform` in root `pom.xml`, currently `3.32.2`. All ecosystem projects must match. When bumping, bump all projects together.

**GitHub Packages — dependency resolution:** Root `pom.xml` has `<repositories>` with `id=github` pointing to `https://maven.pkg.github.com/casehubio/*`. CI uses `server-id: github` + `GITHUB_TOKEN` in `actions/setup-java`.

**Cross-project dependency versions** are properties in root `pom.xml`:
- `version.io.quarkiverse.work` — quarkus-work-api and quarkus-work-core (`0.2-SNAPSHOT`)
- `version.io.quarkiverse.ledger` — quarkus-ledger (`0.2-SNAPSHOT`)

Submodule poms reference `${version.io.quarkiverse.work}` etc. — no hardcoded versions.

**Publishing:** `maven.deploy.skip=false` is the default in root `pom.xml` properties — the root parent POM (`io.casehub:parent`) IS published to GitHub Packages. Downstream consumers need it to resolve the effective POM of child artifacts (`api`, `engine`, etc.). Modules that should not be published override with `<maven.deploy.skip>true</maven.deploy.skip>` in their own `<properties>`.

## casehub-work-adapter Module

Bridges quarkus-work `WorkItemLifecycleEvent` CDI events to CaseHub `PlanItem` transitions via `BlackboardRegistry`. Choreography path only — fires `CONTEXT_CHANGED` for engine re-evaluation.

**Test setup** (when depending on `quarkus-work` full module):
- Add `quarkus-work-testing` test dep — provides `@Alternative @Priority` in-memory WorkItem stores
- Add `quarkus-jdbc-h2` test dep — quarkus-work JPA entities require a datasource even in tests
- Use `quarkus.arc.selected-alternatives` to activate `casehub-persistence-memory` repos
- Add `@Alternative @Priority(1)` static inner class stub for `WorkloadProvider` — quarkus-work ships `JpaWorkloadProvider` which clashes with `CasehubWorkloadProvider` from the engine
- Set `quarkus.quartz.store-type=ram` and `quarkus.hibernate-orm.schema-management.strategy=drop-and-create`

`callerRef` format: `case:{caseId}/pi:{planItemId}` — use `CallerRef.encode()` / `CallerRef.parse()`.
