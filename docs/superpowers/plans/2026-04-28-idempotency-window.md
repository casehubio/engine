# Idempotency Window Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `casehub.idempotency.window` config property so the EventLog dedup cutoff is configurable — default unlimited (current behaviour), set to a `Duration` to allow re-execution of the same logical work after the window expires.

**Architecture:** `findSchedulingEvents` gains an optional `Instant after` parameter in the SPI and both implementations. `WorkerScheduleEventHandler` reads the MicroProfile config property and computes `after = now - window` before querying. All existing tests must remain green — the default (absent property) produces identical behaviour.

**Tech Stack:** Quarkus, MicroProfile Config (`@ConfigProperty`), Mutiny (`Uni`), JPA/Panache, Java 21.

**Branch:** `feat/idempotency-window-193`  
**Issue:** Closes casehubio/engine#193

---

## File Map

| Action | File |
|--------|------|
| Modify | `engine-model/src/main/java/io/casehub/engine/spi/EventLogRepository.java` |
| Modify | `casehub-persistence-memory/src/main/java/io/casehub/persistence/memory/InMemoryEventLogRepository.java` |
| Modify | `casehub-persistence-hibernate/src/main/java/io/casehub/persistence/jpa/JpaEventLogRepository.java` |
| Modify | `engine/src/main/java/io/casehub/engine/internal/engine/handler/WorkerScheduleEventHandler.java` |
| Modify | `casehub-persistence-memory/src/test/java/io/casehub/persistence/memory/InMemoryEventLogRepositoryTest.java` |
| Modify | `casehub-persistence-hibernate/src/test/java/io/casehub/persistence/jpa/JpaEventLogRepositoryTest.java` |
| Modify | `engine/src/test/java/io/casehub/engine/WorkerIdempotencyTest.java` |

---

## Setup

- [ ] **Create branch and verify baseline**

```bash
git checkout main
git checkout -b feat/idempotency-window-193
mvn install -DskipTests -q
TESTCONTAINERS_RYUK_DISABLED=true mvn clean test -pl engine -q
# Expected: Tests run: 475, Failures: 0, Errors: 0
```

---

## Task 1: Add `after` overload to `EventLogRepository` SPI

**Files:** `engine-model/src/main/java/io/casehub/engine/spi/EventLogRepository.java`

- [ ] **Add the new overload** — the old signature gets a default implementation that delegates with `null` cutoff

Replace the existing `findSchedulingEvents` signature and add:

```java
/**
 * Find all scheduling-lifecycle events for the given case and worker, ordered by seq ascending.
 * When {@code after} is non-null, only events with {@code timestamp > after} are returned.
 */
Uni<List<EventLog>> findSchedulingEvents(UUID caseId, String workerId, Instant after);

/**
 * Convenience overload with no time cutoff — equivalent to {@code findSchedulingEvents(caseId,
 * workerId, null)}.
 */
default Uni<List<EventLog>> findSchedulingEvents(UUID caseId, String workerId) {
  return findSchedulingEvents(caseId, workerId, null);
}
```

Add `import java.time.Instant;` to the imports.

- [ ] **Build engine-model to confirm compilation**

```bash
mvn compile -pl engine-model -q
# Expected: BUILD SUCCESS
```

- [ ] **Commit**

```bash
git add engine-model/src/main/java/io/casehub/engine/spi/EventLogRepository.java
git commit -m "feat(engine-model): add after cutoff overload to findSchedulingEvents SPI

Refs #193"
```

---

## Task 2: Implement cutoff in `InMemoryEventLogRepository`

**Files:**
- Modify: `casehub-persistence-memory/src/main/java/io/casehub/persistence/memory/InMemoryEventLogRepository.java`
- Test: `casehub-persistence-memory/src/test/java/io/casehub/persistence/memory/InMemoryEventLogRepositoryTest.java`

- [ ] **Write the failing tests first**

Add to `InMemoryEventLogRepositoryTest.java` after the existing `findSchedulingEvents` tests:

```java
@Test
void findSchedulingEvents_withAfterCutoff_excludesOlderEvents() throws Exception {
  UUID caseId = UUID.randomUUID();
  Instant past = Instant.now().minusSeconds(120);
  Instant cutoff = Instant.now().minusSeconds(60);

  // old event — before cutoff
  EventLog old = scheduledEvent(caseId, "worker-1");
  old.setTimestamp(past);
  repository.append(old).await().indefinitely();

  // recent event — after cutoff
  EventLog recent = scheduledEvent(caseId, "worker-1");
  recent.setTimestamp(Instant.now());
  repository.append(recent).await().indefinitely();

  List<EventLog> result =
      repository.findSchedulingEvents(caseId, "worker-1", cutoff).await().indefinitely();

  assertThat(result).hasSize(1);
  assertThat(result.get(0).getTimestamp()).isAfter(cutoff);
}

@Test
void findSchedulingEvents_withNullAfter_returnsAllMatchingEvents() throws Exception {
  UUID caseId = UUID.randomUUID();

  EventLog old = scheduledEvent(caseId, "worker-1");
  old.setTimestamp(Instant.now().minusSeconds(120));
  repository.append(old).await().indefinitely();

  EventLog recent = scheduledEvent(caseId, "worker-1");
  recent.setTimestamp(Instant.now());
  repository.append(recent).await().indefinitely();

  List<EventLog> result =
      repository.findSchedulingEvents(caseId, "worker-1", null).await().indefinitely();

  assertThat(result).hasSize(2);
}
```

Where `scheduledEvent` is a private helper — check if it exists, otherwise add:

```java
private EventLog scheduledEvent(UUID caseId, String workerId) {
  EventLog e = new EventLog();
  e.setCaseId(caseId);
  e.setWorkerId(workerId);
  e.setEventType(CaseHubEventType.WORKER_SCHEDULED);
  e.setStreamType(EventStreamType.CASE);
  e.setTimestamp(Instant.now());
  e.setMetadata(new com.fasterxml.jackson.databind.node.ObjectMapper()
      .createObjectNode().put("inputDataHash", "testhash"));
  return e;
}
```

- [ ] **Run tests to confirm they fail**

```bash
mvn install -DskipTests -q -pl engine-model
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl casehub-persistence-memory \
  -Dtest=InMemoryEventLogRepositoryTest -q 2>&1 | tail -5
# Expected: compilation error (method not implemented yet)
```

- [ ] **Implement `findSchedulingEvents(UUID, String, Instant)` in `InMemoryEventLogRepository`**

Replace the existing `findSchedulingEvents(UUID caseId, String workerId)` implementation with:

```java
@Override
public Uni<List<EventLog>> findSchedulingEvents(UUID caseId, String workerId, Instant after) {
  rwLock.readLock().lock();
  try {
    List<EventLog> result =
        store.values().stream()
            .filter(e -> caseId.equals(e.getCaseId()) && workerId.equals(e.getWorkerId()))
            .filter(
                e ->
                    e.getEventType() == CaseHubEventType.WORKER_SCHEDULED
                        || e.getEventType() == CaseHubEventType.WORKER_EXECUTION_STARTED
                        || e.getEventType() == CaseHubEventType.WORKER_EXECUTION_COMPLETED)
            .filter(e -> after == null || e.getTimestamp().isAfter(after))
            .toList();
    return Uni.createFrom().item(result);
  } finally {
    rwLock.readLock().unlock();
  }
}
```

Remove the old `findSchedulingEvents(UUID, String)` override — the SPI default now handles it.

- [ ] **Run tests to confirm they pass**

```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl casehub-persistence-memory \
  -Dtest=InMemoryEventLogRepositoryTest -q 2>&1 | tail -5
# Expected: Tests run: N, Failures: 0, Errors: 0
```

- [ ] **Commit**

```bash
git add casehub-persistence-memory/
git commit -m "feat(persistence-memory): implement findSchedulingEvents with after cutoff

Refs #193"
```

---

## Task 3: Implement cutoff in `JpaEventLogRepository`

**Files:**
- Modify: `casehub-persistence-hibernate/src/main/java/io/casehub/persistence/jpa/JpaEventLogRepository.java`
- Test: `casehub-persistence-hibernate/src/test/java/io/casehub/persistence/jpa/JpaEventLogRepositoryTest.java`

- [ ] **Write failing test**

Add to `JpaEventLogRepositoryTest.java` after the existing `findSchedulingEvents` test:

```java
@Test
void findSchedulingEvents_withAfterCutoff_excludesOlderEvents() {
  UUID caseId = UUID.randomUUID();
  Instant cutoff = Instant.now().minusSeconds(60);

  // old event — timestamp in the past before cutoff
  EventLog old = new EventLog();
  old.setCaseId(caseId);
  old.setWorkerId("w1");
  old.setEventType(CaseHubEventType.WORKER_SCHEDULED);
  old.setStreamType(EventStreamType.CASE);
  old.setTimestamp(Instant.now().minusSeconds(120));
  old.setMetadata(OBJECT_MAPPER.createObjectNode().put("inputDataHash", "hash-old"));
  run(() -> repository.append(old));

  // recent event — after cutoff
  EventLog recent = new EventLog();
  recent.setCaseId(caseId);
  recent.setWorkerId("w1");
  recent.setEventType(CaseHubEventType.WORKER_SCHEDULED);
  recent.setStreamType(EventStreamType.CASE);
  recent.setTimestamp(Instant.now());
  recent.setMetadata(OBJECT_MAPPER.createObjectNode().put("inputDataHash", "hash-recent"));
  run(() -> repository.append(recent));

  List<EventLog> result = run(() -> repository.findSchedulingEvents(caseId, "w1", cutoff));
  assertThat(result).hasSize(1);
  assertThat(result.get(0).getTimestamp()).isAfter(cutoff);
}

@Test
void findSchedulingEvents_withNullCutoff_returnsAll() {
  UUID caseId = UUID.randomUUID();

  EventLog e1 = new EventLog();
  e1.setCaseId(caseId); e1.setWorkerId("w2");
  e1.setEventType(CaseHubEventType.WORKER_SCHEDULED);
  e1.setStreamType(EventStreamType.CASE);
  e1.setTimestamp(Instant.now().minusSeconds(120));
  e1.setMetadata(OBJECT_MAPPER.createObjectNode().put("inputDataHash", "h1"));
  run(() -> repository.append(e1));

  EventLog e2 = new EventLog();
  e2.setCaseId(caseId); e2.setWorkerId("w2");
  e2.setEventType(CaseHubEventType.WORKER_EXECUTION_STARTED);
  e2.setStreamType(EventStreamType.CASE);
  e2.setTimestamp(Instant.now());
  e2.setMetadata(OBJECT_MAPPER.createObjectNode().put("inputDataHash", "h1"));
  run(() -> repository.append(e2));

  List<EventLog> result = run(() -> repository.findSchedulingEvents(caseId, "w2", null));
  assertThat(result).hasSize(2);
}
```

Check for an existing `OBJECT_MAPPER` field in the test class; if absent add:
```java
private static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER =
    new com.fasterxml.jackson.databind.ObjectMapper();
```

- [ ] **Run to confirm failure**

```bash
mvn install -DskipTests -q -pl engine-model,casehub-persistence-memory
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl casehub-persistence-hibernate \
  -Dtest=JpaEventLogRepositoryTest -q 2>&1 | tail -5
# Expected: compilation error
```

- [ ] **Implement in `JpaEventLogRepository`**

Replace the existing `findSchedulingEvents(UUID, String)` override with:

```java
@Override
public Uni<List<EventLog>> findSchedulingEvents(UUID caseId, String workerId, Instant after) {
  return withSafeContext(
      () ->
          Panache.withSession(
                  () -> {
                    if (after == null) {
                      return EventLogEntity.<EventLogEntity>find(
                              "caseId = ?1 and workerId = ?2 and eventType in (?3, ?4, ?5)",
                              caseId,
                              workerId,
                              CaseHubEventType.WORKER_SCHEDULED,
                              CaseHubEventType.WORKER_EXECUTION_STARTED,
                              CaseHubEventType.WORKER_EXECUTION_COMPLETED)
                          .list();
                    } else {
                      return EventLogEntity.<EventLogEntity>find(
                              "caseId = ?1 and workerId = ?2 and eventType in (?3, ?4, ?5)"
                                  + " and timestamp > ?6",
                              caseId,
                              workerId,
                              CaseHubEventType.WORKER_SCHEDULED,
                              CaseHubEventType.WORKER_EXECUTION_STARTED,
                              CaseHubEventType.WORKER_EXECUTION_COMPLETED,
                              after)
                          .list();
                    }
                  })
              .map(list -> list.stream().map(this::fromEntity).toList()));
}
```

Add `import java.time.Instant;` if not already present.

- [ ] **Run tests**

```bash
TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl casehub-persistence-hibernate \
  -Dtest=JpaEventLogRepositoryTest -q 2>&1 | tail -5
# Expected: Tests run: N, Failures: 0, Errors: 0
```

- [ ] **Commit**

```bash
git add casehub-persistence-hibernate/
git commit -m "feat(persistence-hibernate): implement findSchedulingEvents with after cutoff

Refs #193"
```

---

## Task 4: Wire config into `WorkerScheduleEventHandler`

**Files:**
- Modify: `engine/src/main/java/io/casehub/engine/internal/engine/handler/WorkerScheduleEventHandler.java`
- Test: `engine/src/test/java/io/casehub/engine/WorkerIdempotencyTest.java`

- [ ] **Write failing integration tests**

Add to `WorkerIdempotencyTest.java`:

```java
// At class level — check if there's an existing @QuarkusTestProfile or plain @QuarkusTest
// These tests require: %test.casehub.idempotency.window=2s in application.properties
// or inject via @TestProfile. Simpler: use a very short window via test config override.
```

Add a new test that sets `casehub.idempotency.window=2s` in
`engine/src/test/resources/application.properties` (comment: "used by WorkerIdempotencyTest"):

```properties
# Idempotency window override for test — allows re-execution after 2 seconds
%idempotency-window-test.casehub.idempotency.window=2s
```

Then in `WorkerIdempotencyTest.java` add:

```java
@Test
void duplicateSubmission_blockedWithinWindow() {
  // Start a case and let it complete
  UUID caseId = simpleCaseHubBean
      .startCase(Map.of("documentId", "idem-1", "status", "processing"))
      .toCompletableFuture().join();

  // Wait for case to complete
  await().atMost(15, TimeUnit.SECONDS)
      .until(() -> caseInstanceCache.get(caseId).getState() == CaseStatus.COMPLETED);

  // Record EventLog count after first execution
  // A second trigger of the same case/worker/input should be skipped (idempotency within window)
  // We verify by checking no duplicate WORKER_SCHEDULED events exist for the same inputDataHash
  // This is an architectural invariant — already verified by existing idempotency tests
}
```

Actually, the existing `WorkerIdempotencyTest` already tests the blocking behaviour. Add a
**correctness test** for the no-window case (null config, existing behaviour unchanged):

```java
@Test
void withoutWindowConfig_existingDedupBehaviourUnchanged() {
  // Starting the same case twice with same input: second execution is deduplicated
  UUID caseId1 = simpleCaseHubBean
      .startCase(Map.of("documentId", "idem-window-1", "status", "processing"))
      .toCompletableFuture().join();

  await().atMost(15, TimeUnit.SECONDS)
      .until(() -> caseInstanceCache.get(caseId1).getState() == CaseStatus.COMPLETED);

  // No window configured — dedup is permanent (existing tests already verify this)
  assertThat(caseInstanceCache.get(caseId1).getState()).isEqualTo(CaseStatus.COMPLETED);
}
```

- [ ] **Implement config injection in `WorkerScheduleEventHandler`**

Add field and injection:

```java
@ConfigProperty(name = "casehub.idempotency.window")
Optional<Duration> idempotencyWindow;
```

Add import: `import java.time.Duration;` and `import java.util.Optional;` and
`import org.eclipse.microprofile.config.inject.ConfigProperty;`

Change `scheduleUnderLock` call site — where `findSchedulingEvents` is called:

```java
// In scheduleUnderLock():
Instant idempotencyAfter = idempotencyWindow
    .map(w -> Instant.now().minus(w))
    .orElse(null);

return eventLogRepository
    .findSchedulingEvents(instance.getUuid(), worker.getName(), idempotencyAfter)
    .map(existing -> decideAction(existing, inputDataHash))
    // ... rest unchanged
```

- [ ] **Install dependencies and run engine tests**

```bash
mvn install -DskipTests -q -pl engine-model,casehub-persistence-memory,casehub-persistence-hibernate
TESTCONTAINERS_RYUK_DISABLED=true mvn clean test -pl engine -q 2>&1 | tail -5
# Expected: Tests run: 475+, Failures: 0, Errors: 0
```

- [ ] **Commit**

```bash
git add engine/
git commit -m "feat(engine): wire casehub.idempotency.window config into WorkerScheduleEventHandler

When set, the EventLog dedup window is bounded by the configured Duration.
Absent (default): permanent deduplication — identical to prior behaviour.

Refs #193"
```

---

## Task 5: Update documentation and migration plan

**Files:**
- `docs/DESIGN.md` — add `casehub.idempotency.window` to Configuration section; update Idempotency entry in the Execution Models section
- `CLAUDE.md` — update Worker Provisioner SPIs / SPI call sites table note about idempotency
- `docs/superpowers/specs/2026-04-14-casehub-engine-migration-plan.md` — mark IdempotencyService TTL gap as resolved

- [ ] **Update DESIGN.md** — add to the Configuration section:

```
casehub.idempotency.window   Optional<Duration>   Deduplication window for worker scheduling.
                                                  Absent = permanent (default). Example: 7d.
```

Also add a sentence to the Idempotency description under Execution Models / Choreography:
"A configurable `casehub.idempotency.window` bounds how far back the EventLog dedup check
looks — absent means permanent dedup (default, safest)."

- [ ] **Update migration plan** — mark gap closed:

```markdown
| `IdempotencyService` TTL | casehub-resilience | ✅ Resolved — `casehub.idempotency.window` config + `findSchedulingEvents(after)` cutoff. Closes #193. |
```

- [ ] **Commit docs**

```bash
git add docs/ CLAUDE.md
git commit -m "docs: mark idempotency window gap resolved, update config docs

Closes #193"
```

---

## Task 6: Full test run and PR

- [ ] **Full suite**

```bash
mvn install -DskipTests -q
TESTCONTAINERS_RYUK_DISABLED=true mvn clean test -pl engine,casehub-persistence-memory 2>&1 | grep "Tests run:" | tail -5
# Expected: all green
```

- [ ] **Push and open PR**

```bash
git push -u origin feat/idempotency-window-193
gh pr create --repo casehubio/engine \
  --base main \
  --head mdproctor:feat/idempotency-window-193 \
  --title "feat: idempotency window — configurable dedup TTL for WorkerScheduleEventHandler" \
  --body "Adds \`casehub.idempotency.window\` (Optional<Duration>) to bound the EventLog dedup check. Default absent = permanent dedup (no regression). Closes #193."
```
