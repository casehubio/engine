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
package io.casehub.engine.runtime.stigmergy;

import io.casehub.api.model.signal.PerceivedSignal;
import io.casehub.api.spi.observation.Observation;
import io.casehub.api.spi.stigmergy.AgentObservationProvider;
import io.casehub.engine.common.internal.observation.ObservationRegistry;
import io.casehub.engine.common.internal.signal.SignalRegistry;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public class DefaultAgentObservationProvider implements AgentObservationProvider {

  private static final double DEFAULT_THRESHOLD = 0.01;

  private final SignalRegistry signalRegistry;
  private final ObservationRegistry observationRegistry;
  private final @Nullable StigmergyCoordinator coordinator;

  public DefaultAgentObservationProvider(
      SignalRegistry signalRegistry,
      ObservationRegistry observationRegistry,
      @Nullable StigmergyCoordinator coordinator) {
    this.signalRegistry = signalRegistry;
    this.observationRegistry = observationRegistry;
    this.coordinator = coordinator;
  }

  @Override
  public Map<String, PerceivedSignal> perceiveSignals(UUID caseId) {
    if (coordinator == null || !coordinator.isStigmergyCase(caseId)) {
      return Map.of();
    }
    return signalRegistry.perceive(caseId, DEFAULT_THRESHOLD);
  }

  @Override
  public List<Observation> observations(UUID caseId, String agentId) {
    return observationRegistry.getObservations(caseId, agentId);
  }
}
