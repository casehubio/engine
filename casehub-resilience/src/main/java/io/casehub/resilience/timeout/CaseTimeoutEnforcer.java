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

import io.casehub.api.model.CaseStatus;
import io.casehub.engine.internal.engine.cache.CaseInstanceCache;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.model.CaseInstance;
import io.quarkus.scheduler.Scheduled;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;

/**
 * Scheduled scanner that faults RUNNING cases whose wall-clock age has exceeded the configured
 * budget.
 *
 * <p>Timeout detection is based on wall-clock time because {@code PropagationContext} (which will
 * carry a structured budget field) has not yet been merged into the {@code api} module. Once PR
 * #119 lands, replace the wall-clock tracking here with {@code
 * PropagationContext#isBudgetExhausted()}.
 *
 * <p>Start times are recorded lazily: the first time the scanner observes a case in RUNNING state
 * it records that instant as the effective start. This avoids competing with the engine's
 * request-reply {@code CASE_STARTED} event bus consumer ({@code CaseStartedEventHandler}), which
 * uses point-to-point {@code request()} semantics and must not have additional consumers competing
 * to handle the message.
 *
 * <p>Cases that exceed the budget are transitioned to {@link CaseStatus#FAULTED} and a {@link
 * EventBusAddresses#CASE_FAULTED} event is published.
 */
@ApplicationScoped
public class CaseTimeoutEnforcer {

  private static final Logger LOG = Logger.getLogger(CaseTimeoutEnforcer.class);

  /** Default max run time — override with {@code casehub.resilience.timeout.max-duration}. */
  static final Duration DEFAULT_MAX_DURATION = Duration.ofMinutes(5);

  private final CaseInstanceCache cache;
  private final EventBus eventBus;
  private final Duration maxDuration;
  private final Clock clock;

  /**
   * Key: caseId. Value: the instant the enforcer first observed the case in RUNNING state. Used as
   * a conservative wall-clock start time until PropagationContext is available (PR #119).
   */
  private final ConcurrentHashMap<UUID, Instant> runningStartTimes = new ConcurrentHashMap<>();

  /** Production constructor — injected by CDI. */
  @Inject
  public CaseTimeoutEnforcer(CaseInstanceCache cache, EventBus eventBus) {
    this(cache, eventBus, DEFAULT_MAX_DURATION, Clock.systemUTC());
  }

  /**
   * Test constructor — allows injecting a custom {@link Clock} and {@link Duration} without
   * starting a Quarkus container.
   */
  CaseTimeoutEnforcer(
      CaseInstanceCache cache, EventBus eventBus, Duration maxDuration, Clock clock) {
    this.cache = cache;
    this.eventBus = eventBus;
    this.maxDuration = maxDuration;
    this.clock = clock;
  }

  /**
   * Scheduled scan. Iterates all cached RUNNING cases and faults any whose elapsed wall-clock time
   * exceeds {@link #maxDuration}.
   *
   * <p>Called every second by default; override with {@code
   * casehub.resilience.timeout.check-interval}.
   */
  @Scheduled(every = "${casehub.resilience.timeout.check-interval:1s}")
  public void scanForTimeouts() {
    Instant now = Instant.now(clock);
    for (CaseInstance instance : cache.getAll()) {
      if (instance.getState() != CaseStatus.RUNNING) {
        continue;
      }
      UUID caseId = instance.getUuid();

      // Record start time lazily on first observation of a RUNNING case.
      Instant startedAt = runningStartTimes.computeIfAbsent(caseId, id -> now);

      Duration elapsed = Duration.between(startedAt, now);
      if (elapsed.compareTo(maxDuration) > 0) {
        LOG.warnf(
            "Case %s has been RUNNING for %s (limit: %s) — transitioning to FAULTED",
            caseId, elapsed, maxDuration);
        instance.setState(CaseStatus.FAULTED);
        runningStartTimes.remove(caseId);
        eventBus.publish(EventBusAddresses.CASE_FAULTED, caseId.toString());
      }
    }
  }

  /**
   * Records a start time for a case directly. Used in tests that bypass the normal scheduled scan
   * flow to set a back-dated start time that will trigger timeout detection.
   */
  void recordStartTime(UUID caseId, Instant startedAt) {
    runningStartTimes.put(caseId, startedAt);
  }

  /** Returns the recorded start time for a case, or {@link Optional#empty()} if not yet seen. */
  Optional<Instant> getStartTime(UUID caseId) {
    return Optional.ofNullable(runningStartTimes.get(caseId));
  }
}
