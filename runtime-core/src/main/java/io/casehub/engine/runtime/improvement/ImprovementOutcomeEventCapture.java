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
package io.casehub.engine.runtime.improvement;

import io.casehub.api.model.improvement.ImprovementOutcome;
import io.casehub.api.spi.CaseOutcomeEvent;
import io.casehub.api.spi.CaseOutcomeObserver;
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;

public class ImprovementOutcomeEventCapture implements CaseOutcomeObserver {

  private static final Logger LOG = Logger.getLogger(ImprovementOutcomeEventCapture.class);

  private final ImprovementOutcomeRecorder outcomeRecorder;
  private final ImprovementSignalProjector signalProjector;
  private final ImprovementCbrProjector cbrProjector;
  private final ImprovementBudgetEnforcer budgetEnforcer;
  private final ImprovementCategoryTracker categoryTracker;
  private final RegressionDetector regressionDetector;

  public ImprovementOutcomeEventCapture(
      ImprovementOutcomeRecorder outcomeRecorder,
      ImprovementSignalProjector signalProjector,
      ImprovementCbrProjector cbrProjector,
      ImprovementBudgetEnforcer budgetEnforcer,
      ImprovementCategoryTracker categoryTracker,
      RegressionDetector regressionDetector) {
    this.outcomeRecorder = outcomeRecorder;
    this.signalProjector = signalProjector;
    this.cbrProjector = cbrProjector;
    this.budgetEnforcer = budgetEnforcer;
    this.categoryTracker = categoryTracker;
    this.regressionDetector = regressionDetector;
  }

  public ImprovementOutcomeEventCapture(
      ImprovementOutcomeRecorder outcomeRecorder,
      ImprovementSignalProjector signalProjector,
      ImprovementCbrProjector cbrProjector,
      ImprovementBudgetEnforcer budgetEnforcer) {
    this(
        outcomeRecorder,
        signalProjector,
        cbrProjector,
        budgetEnforcer,
        new ImprovementCategoryTracker(),
        new RegressionDetector(
            new RegressionEvaluatorRegistry(),
            new ImprovementCategoryRegistry(),
            new ImprovementCategoryTracker(),
            new RollbackHistory(),
            new HealthScoreTracker(new CapabilityAreaRegistry()),
            e -> {}));
  }

  @Override
  public void onOutcome(CaseOutcomeEvent event) {
    if (event.caseType() == null || !event.caseType().startsWith("self-improvement")) {
      return;
    }

    @SuppressWarnings("unchecked")
    var improvementData = (Map<String, Object>) event.caseFileSnapshot().get("_improvement");
    if (improvementData == null) {
      LOG.debugf(
          "Improvement case %s completed but no _improvement context — skipping outcome capture",
          event.caseId());
      return;
    }

    var parentCaseIdStr = (String) improvementData.get("parentCaseId");
    if (parentCaseIdStr == null) {
      LOG.warnf("Improvement case %s missing parentCaseId in _improvement context", event.caseId());
      return;
    }

    var parentCaseId = UUID.fromString(parentCaseIdStr);
    var category = (String) improvementData.getOrDefault("category", "unknown");
    var target = (String) improvementData.getOrDefault("target", "unknown");
    var statusStr = (String) improvementData.get("status");
    var prUrl = (String) improvementData.get("prUrl");

    ImprovementOutcome.OutcomeStatus status;
    if (statusStr != null) {
      status = ImprovementOutcome.OutcomeStatus.valueOf(statusStr);
    } else {
      status =
          switch (event.outcomeLabel()) {
            case "COMPLETED" -> ImprovementOutcome.OutcomeStatus.MERGED;
            case "FAULTED" -> ImprovementOutcome.OutcomeStatus.FAILED;
            case "CANCELLED" -> ImprovementOutcome.OutcomeStatus.ABANDONED;
            default -> ImprovementOutcome.OutcomeStatus.FAILED;
          };
    }

    var outcome =
        new ImprovementOutcome(
            parentCaseId,
            event.caseId(),
            category,
            target,
            status,
            prUrl,
            null,
            null,
            null,
            event.closedAt(),
            Map.of());

    onImprovementComplete(new ImprovementCaseCompleted(parentCaseId, event.tenancyId(), outcome));
  }

  public void onImprovementComplete(ImprovementCaseCompleted event) {
    var outcome = event.outcome();
    outcomeRecorder.record(event.caseId(), event.tenancyId(), outcome);
    signalProjector.project(event.caseId(), outcome);
    cbrProjector.project(event.tenancyId(), outcome);
    budgetEnforcer.recordCompletion(outcome.improvementCaseId());
    categoryTracker.recordOutcome(event.caseId(), outcome.category(), outcome.status());
    regressionDetector.onOutcome(event.caseId(), outcome);
  }
}
