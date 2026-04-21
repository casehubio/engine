/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.resilience.timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.casehub.api.model.CaseStatus;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.model.CaseInstance;
import io.vertx.mutiny.core.eventbus.EventBus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests — no Quarkus, no CDI. CaseTimeoutEnforcer is testable without a container because
 * it exposes a package-private constructor that accepts a {@link Clock} and {@link Duration}.
 *
 * <p>Note: PropagationContext (PR #119) is not yet merged into api/. Timeout detection falls back
 * to wall-clock elapsed time tracked internally by the enforcer. Once PR #119 lands, update these
 * tests to set an exhausted PropagationContext budget instead.
 */
class CaseTimeoutEnforcerTest {

  private CaseInstanceCache cache;
  private EventBus eventBus;
  private Clock fixedClock;

  @BeforeEach
  void setUp() {
    cache = new CaseInstanceCache();
    eventBus = mock(EventBus.class);
    fixedClock = Clock.fixed(Instant.parse("2026-04-21T10:00:00Z"), ZoneOffset.UTC);
  }

  // ---- Helper ---------------------------------------------------------------

  private CaseInstance runningInstance() {
    CaseInstance instance = new CaseInstance();
    instance.setUuid(UUID.randomUUID());
    instance.setState(CaseStatus.RUNNING);
    return instance;
  }

  private CaseTimeoutEnforcer enforcerWithMaxDuration(Duration maxDuration) {
    return new CaseTimeoutEnforcer(cache, eventBus, maxDuration, fixedClock);
  }

  // ---- No-op cases ----------------------------------------------------------

  @Test
  void emptyCache_scanForTimeouts_doesNothing() {
    CaseTimeoutEnforcer enforcer = enforcerWithMaxDuration(Duration.ofMinutes(5));
    enforcer.scanForTimeouts(); // must not throw
  }

  @Test
  void runningCase_firstObservation_recordsStartAndRemainRunning() {
    CaseInstance instance = runningInstance();
    cache.put(instance);

    CaseTimeoutEnforcer enforcer = enforcerWithMaxDuration(Duration.ofMinutes(5));
    // First scan: start time is recorded lazily as "now" (fixedClock = 10:00:00).
    // Elapsed = 0 — well within the 5-minute budget.
    enforcer.scanForTimeouts();

    assertThat(instance.getState()).isEqualTo(CaseStatus.RUNNING);
    assertThat(enforcer.getStartTime(instance.getUuid())).isPresent();
  }

  @Test
  void runningCase_withinBudget_remainsRunning() {
    CaseInstance instance = runningInstance();
    cache.put(instance);

    // Start time is 1 minute before "now"; max is 5 minutes → within budget
    Instant startedAt = Instant.parse("2026-04-21T09:59:00Z");
    CaseTimeoutEnforcer enforcer = enforcerWithMaxDuration(Duration.ofMinutes(5));
    enforcer.recordStartTime(instance.getUuid(), startedAt);

    enforcer.scanForTimeouts();

    assertThat(instance.getState()).isEqualTo(CaseStatus.RUNNING);
  }

  // ---- Timeout cases --------------------------------------------------------

  @Test
  void runningCase_exceededBudget_isTransitionedToFaulted() {
    CaseInstance instance = runningInstance();
    cache.put(instance);

    // Start time is 10 minutes before "now"; max is 5 minutes → exceeded
    Instant startedAt = Instant.parse("2026-04-21T09:50:00Z");
    CaseTimeoutEnforcer enforcer = enforcerWithMaxDuration(Duration.ofMinutes(5));
    enforcer.recordStartTime(instance.getUuid(), startedAt);

    enforcer.scanForTimeouts();

    assertThat(instance.getState()).isEqualTo(CaseStatus.FAULTED);
  }

  @Test
  void timedOutCase_faultedEventIsPublished() {
    CaseInstance instance = runningInstance();
    UUID caseId = instance.getUuid();
    cache.put(instance);

    Instant startedAt = Instant.parse("2026-04-21T09:50:00Z");
    CaseTimeoutEnforcer enforcer = enforcerWithMaxDuration(Duration.ofMinutes(5));
    enforcer.recordStartTime(caseId, startedAt);

    enforcer.scanForTimeouts();

    verify(eventBus).publish(EventBusAddresses.CASE_FAULTED, caseId.toString());
  }

  @Test
  void timedOutCase_startTimeIsRemovedAfterFault() {
    CaseInstance instance = runningInstance();
    UUID caseId = instance.getUuid();
    cache.put(instance);

    Instant startedAt = Instant.parse("2026-04-21T09:50:00Z");
    CaseTimeoutEnforcer enforcer = enforcerWithMaxDuration(Duration.ofMinutes(5));
    enforcer.recordStartTime(caseId, startedAt);

    enforcer.scanForTimeouts();

    assertThat(enforcer.getStartTime(caseId)).isEmpty();
  }

  // ---- Non-RUNNING cases are ignored ----------------------------------------

  @Test
  void completedCase_isNeverFaulted() {
    CaseInstance instance = runningInstance();
    instance.setState(CaseStatus.COMPLETED);
    cache.put(instance);

    Instant startedAt = Instant.parse("2026-04-21T09:00:00Z"); // 1 hour ago
    CaseTimeoutEnforcer enforcer = enforcerWithMaxDuration(Duration.ofMinutes(5));
    enforcer.recordStartTime(instance.getUuid(), startedAt);

    enforcer.scanForTimeouts();

    assertThat(instance.getState()).isEqualTo(CaseStatus.COMPLETED);
  }

  @Test
  void waitingCase_isNeverFaulted() {
    CaseInstance instance = runningInstance();
    instance.setState(CaseStatus.WAITING);
    cache.put(instance);

    Instant startedAt = Instant.parse("2026-04-21T09:00:00Z");
    CaseTimeoutEnforcer enforcer = enforcerWithMaxDuration(Duration.ofMinutes(5));
    enforcer.recordStartTime(instance.getUuid(), startedAt);

    enforcer.scanForTimeouts();

    assertThat(instance.getState()).isEqualTo(CaseStatus.WAITING);
  }

  // ---- Multiple cases -------------------------------------------------------

  @Test
  void multipleCases_onlyExhaustedOnesAreFaulted() {
    CaseInstance overdue = runningInstance();
    CaseInstance healthy = runningInstance();
    cache.put(overdue);
    cache.put(healthy);

    CaseTimeoutEnforcer enforcer = enforcerWithMaxDuration(Duration.ofMinutes(5));
    enforcer.recordStartTime(overdue.getUuid(), Instant.parse("2026-04-21T09:50:00Z")); // 10m ago
    enforcer.recordStartTime(healthy.getUuid(), Instant.parse("2026-04-21T09:59:00Z")); // 1m ago

    enforcer.scanForTimeouts();

    assertThat(overdue.getState()).isEqualTo(CaseStatus.FAULTED);
    assertThat(healthy.getState()).isEqualTo(CaseStatus.RUNNING);
  }

  @Test
  void secondScan_alreadyFaultedCase_isNotProcessedAgain() {
    CaseInstance instance = runningInstance();
    UUID caseId = instance.getUuid();
    cache.put(instance);

    CaseTimeoutEnforcer enforcer = enforcerWithMaxDuration(Duration.ofMinutes(5));
    enforcer.recordStartTime(caseId, Instant.parse("2026-04-21T09:50:00Z"));

    enforcer.scanForTimeouts(); // faults the case, removes start time
    assertThat(instance.getState()).isEqualTo(CaseStatus.FAULTED);

    // Second scan: state is FAULTED (not RUNNING), enforcer must skip it
    enforcer.scanForTimeouts();
    assertThat(instance.getState()).isEqualTo(CaseStatus.FAULTED);
  }
}
