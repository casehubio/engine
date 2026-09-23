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
package io.casehub.engine.internal.improvement;

import io.casehub.api.model.stigmergy.CircuitBreakerState;
import io.casehub.api.model.stigmergy.HealthPolicy;
import io.casehub.engine.common.spi.Resettable;
import io.casehub.engine.common.spi.event.CircuitBreakerStateChangedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ImprovementCircuitBreaker implements Resettable {

  private final ConcurrentHashMap<UUID, CircuitBreakerState> states = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<UUID, Integer> halfOpenCount = new ConcurrentHashMap<>();
  private final Event<CircuitBreakerStateChangedEvent> stateChangedEvent;

  @Inject
  public ImprovementCircuitBreaker(Event<CircuitBreakerStateChangedEvent> stateChangedEvent) {
    this.stateChangedEvent = stateChangedEvent;
  }

  public CircuitBreakerState state(UUID caseId) {
    return states.getOrDefault(caseId, CircuitBreakerState.CLOSED);
  }

  public void evaluate(
      UUID caseId, String tenancyId, HealthScoreTracker tracker, HealthPolicy policy) {
    double score = tracker.computeScore(caseId, tenancyId, policy);
    double delta = tracker.delta(caseId, policy.effectiveHealthWindowMinutes());
    var current = state(caseId);
    CircuitBreakerState newState = current;

    switch (current) {
      case CLOSED -> {
        if (score < policy.effectiveHealthThreshold()
            || delta < -policy.effectiveHealthDeltaThreshold()) {
          newState = CircuitBreakerState.OPEN;
          states.put(caseId, newState);
        }
      }
      case OPEN -> {
        if (score >= policy.effectiveHealthThreshold()) {
          newState = CircuitBreakerState.HALF_OPEN;
          states.put(caseId, newState);
          halfOpenCount.put(caseId, 0);
        }
      }
      case HALF_OPEN -> {
        int completed = halfOpenCount.getOrDefault(caseId, 0);
        if (score < policy.effectiveHealthThreshold()) {
          newState = CircuitBreakerState.OPEN;
          states.put(caseId, newState);
        } else if (completed >= policy.effectiveHalfOpenMaxImprovements()) {
          newState = CircuitBreakerState.CLOSED;
          states.put(caseId, newState);
          halfOpenCount.remove(caseId);
        }
      }
    }

    if (newState != current) {
      stateChangedEvent.fireAsync(new CircuitBreakerStateChangedEvent(caseId, current, newState));
    }
  }

  public void recordImprovementInHalfOpen(UUID caseId) {
    halfOpenCount.computeIfPresent(caseId, (k, v) -> v + 1);
  }

  public void manualReset(UUID caseId) {
    var previous = states.put(caseId, CircuitBreakerState.CLOSED);
    halfOpenCount.remove(caseId);
    if (previous != null && previous != CircuitBreakerState.CLOSED) {
      stateChangedEvent.fireAsync(
          new CircuitBreakerStateChangedEvent(caseId, previous, CircuitBreakerState.CLOSED));
    }
  }

  @Override
  public void reset() {
    states.clear();
    halfOpenCount.clear();
  }
}
