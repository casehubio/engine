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

import io.casehub.api.model.stigmergy.HealthScoreSnapshot;
import io.casehub.api.model.stigmergy.ImprovementOutcome;
import io.casehub.api.model.stigmergy.RollbackPolicy;
import io.casehub.engine.common.spi.Resettable;
import io.casehub.engine.common.spi.event.RegressionDetectedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class RegressionDetector implements Resettable {

  public record MonitoredImprovement(
      UUID improvementCaseId,
      String category,
      String target,
      HealthScoreSnapshot baseline,
      Instant mergedAt,
      int checksRemaining) {}

  private final ConcurrentHashMap<UUID, List<MonitoredImprovement>> monitors =
      new ConcurrentHashMap<>();

  private final ConfidenceScorer scorer;
  private final ImprovementCategoryTracker categoryTracker;
  private final RollbackHistory rollbackHistory;
  private final HealthScoreTracker healthTracker;
  private final Event<RegressionDetectedEvent> regressionDetectedEvent;

  @Inject
  public RegressionDetector(
      ConfidenceScorer scorer,
      ImprovementCategoryTracker categoryTracker,
      RollbackHistory rollbackHistory,
      HealthScoreTracker healthTracker,
      Event<RegressionDetectedEvent> regressionDetectedEvent) {
    this.scorer = scorer;
    this.categoryTracker = categoryTracker;
    this.rollbackHistory = rollbackHistory;
    this.healthTracker = healthTracker;
    this.regressionDetectedEvent = regressionDetectedEvent;
  }

  public void onOutcome(UUID caseId, ImprovementOutcome outcome) {
    if (outcome.status() != ImprovementOutcome.OutcomeStatus.MERGED) {
      return;
    }
    var baseline = healthTracker.latestSnapshot(caseId);
    if (baseline == null) {
      return;
    }

    monitors
        .computeIfAbsent(caseId, k -> new CopyOnWriteArrayList<>())
        .add(
            new MonitoredImprovement(
                outcome.improvementCaseId(),
                outcome.category(),
                outcome.target(),
                baseline,
                Instant.now(),
                2));
  }

  public void checkActiveMonitors(
      UUID caseId, HealthScoreTracker healthTracker, RollbackPolicy policy) {
    var caseMonitors = monitors.get(caseId);
    if (caseMonitors == null || caseMonitors.isEmpty()) {
      return;
    }

    var current = healthTracker.latestSnapshot(caseId);
    if (current == null) {
      return;
    }

    var windowDuration = Duration.ofMinutes(policy.effectiveRegressionWindowMinutes());
    var expired = new java.util.ArrayList<MonitoredImprovement>();
    for (var monitor : caseMonitors) {
      if (Duration.between(monitor.mergedAt(), Instant.now()).compareTo(windowDuration) > 0) {
        expired.add(monitor);
        continue;
      }

      if (current.score() < monitor.baseline().score()) {
        onMetricsDegraded(caseId, monitor, policy, monitor.baseline(), current);
      }
    }
    caseMonitors.removeAll(expired);
  }

  private void onMetricsDegraded(
      UUID caseId,
      MonitoredImprovement monitor,
      RollbackPolicy policy,
      HealthScoreSnapshot before,
      HealthScoreSnapshot after) {
    double confidence = scorer.score(caseId, monitor.improvementCaseId(), before, after);

    regressionDetectedEvent.fireAsync(
        new RegressionDetectedEvent(
            caseId, monitor.improvementCaseId(), confidence, monitor.category()));

    if (confidence >= policy.effectiveAutoRevertThreshold()) {
      rollbackHistory.record(
          caseId, monitor.improvementCaseId(), monitor.category(), monitor.target());
      categoryTracker.pauseCategory(
          caseId,
          monitor.category(),
          Duration.ofMinutes(policy.effectiveRegressionWindowMinutes()));
    } else if (confidence >= policy.effectivePauseThreshold()) {
      categoryTracker.pauseCategory(
          caseId,
          monitor.category(),
          Duration.ofMinutes(policy.effectiveRegressionWindowMinutes()));
    }
  }

  public int activeMonitorCount(UUID caseId) {
    var caseMonitors = monitors.get(caseId);
    return caseMonitors != null ? caseMonitors.size() : 0;
  }

  @Override
  public void reset() {
    monitors.clear();
  }
}
