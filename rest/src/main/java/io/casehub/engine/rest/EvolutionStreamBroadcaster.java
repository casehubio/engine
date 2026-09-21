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
package io.casehub.engine.rest;

import io.casehub.api.view.EvolutionEvent;
import io.casehub.engine.common.spi.event.CircuitBreakerStateChangedEvent;
import io.casehub.engine.common.spi.event.ComplianceLevelChangedEvent;
import io.casehub.engine.common.spi.event.RegressionDetectedEvent;
import io.casehub.engine.common.spi.event.TickEvaluatedEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.smallrye.mutiny.subscription.BackPressureFailure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EvolutionStreamBroadcaster {

  private static final Logger LOG = Logger.getLogger(EvolutionStreamBroadcaster.class);

  private final BroadcastProcessor<EvolutionEvent> processor = BroadcastProcessor.create();

  void onCircuitBreakerChanged(@ObservesAsync CircuitBreakerStateChangedEvent event) {
    emit(
        event.caseId(),
        "circuit-breaker",
        Map.of(
            "oldState", event.oldState().name(),
            "newState", event.newState().name()));
  }

  void onComplianceLevelChanged(@ObservesAsync ComplianceLevelChangedEvent event) {
    emit(
        event.caseId(),
        "compliance",
        Map.of(
            "oldLevel", event.oldLevel().name(),
            "newLevel", event.newLevel().name()));
  }

  void onRegressionDetected(@ObservesAsync RegressionDetectedEvent event) {
    emit(
        event.caseId(),
        "regression",
        Map.of(
            "improvementCaseId", event.improvementCaseId().toString(),
            "confidence", String.valueOf(event.confidence()),
            "category", event.category()));
  }

  void onTickEvaluated(@ObservesAsync TickEvaluatedEvent event) {
    emit(
        event.caseId(),
        "tick",
        Map.of(
            "outcome",
            event.trace().outcome() != null
                ? event.trace().outcome().getClass().getSimpleName()
                : "none"));
  }

  public Multi<EvolutionEvent> stream(UUID caseId) {
    return processor.toHotStream().filter(e -> caseId.equals(e.caseId()));
  }

  private void emit(UUID caseId, String type, Map<String, String> data) {
    try {
      processor.onNext(new EvolutionEvent(caseId, type, data, Instant.now()));
    } catch (BackPressureFailure e) {
      LOG.debugf("Evolution event dropped (back pressure): %s/%s for case %s", type, data, caseId);
    }
  }
}
