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

import static org.junit.jupiter.api.Assertions.*;

import io.casehub.api.model.stigmergy.StigmergyConfig;
import io.casehub.api.model.stigmergy.StigmergyDefaults;
import io.casehub.api.spi.observation.Observation;
import io.casehub.engine.common.internal.convergence.ActivityTracker;
import io.casehub.engine.common.internal.observation.ObservationRegistry;
import io.casehub.engine.common.internal.signal.SignalRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultAgentObservationProviderTest {

  private SignalRegistry signalRegistry;
  private ObservationRegistry observationRegistry;
  private StigmergyCoordinator coordinator;
  private DefaultAgentObservationProvider provider;

  @BeforeEach
  void setUp() {
    signalRegistry = new SignalRegistry();
    observationRegistry = new ObservationRegistry();
    coordinator =
        new StigmergyCoordinator(signalRegistry, observationRegistry, new ActivityTracker());
    provider =
        new DefaultAgentObservationProvider(signalRegistry, observationRegistry, coordinator);
  }

  @Test
  void perceiveSignals_returnsActiveSignals() {
    UUID caseId = UUID.randomUUID();
    var defaults =
        new StigmergyDefaults(Duration.ofMinutes(5), 0.01, 100, 50, 50, null, null, null, null);
    var config = new StigmergyConfig(defaults, null, null, null);
    coordinator.initializeCase(caseId, List.of("agent-a"), config, "tenant-1");

    signalRegistry.deposit(caseId, "trail", 0.8, Duration.ofMinutes(5), "agent-a", 100);

    var signals = provider.perceiveSignals(caseId);
    assertEquals(1, signals.size());
    assertTrue(signals.containsKey("trail"));
    assertTrue(signals.get("trail").sources().contains("agent-a"));
  }

  @Test
  void perceiveSignals_emptyForUnknownCase() {
    var signals = provider.perceiveSignals(UUID.randomUUID());
    assertTrue(signals.isEmpty());
  }

  @Test
  void perceiveSignals_emptyWhenCoordinatorNull() {
    var providerNoCoord =
        new DefaultAgentObservationProvider(signalRegistry, observationRegistry, null);
    var signals = providerNoCoord.perceiveSignals(UUID.randomUUID());
    assertTrue(signals.isEmpty());
  }

  @Test
  void observations_returnsAgentObservations() {
    UUID caseId = UUID.randomUUID();
    var obs = new Observation("pattern-1", 0.9, Map.of(), Instant.now());
    observationRegistry.storeObservations(caseId, "agent-a", List.of(obs));

    var result = provider.observations(caseId, "agent-a");
    assertEquals(1, result.size());
    assertEquals("pattern-1", result.get(0).patternId());
  }

  @Test
  void observations_emptyForDifferentAgent() {
    UUID caseId = UUID.randomUUID();
    var obs = new Observation("pattern-1", 0.9, Map.of(), Instant.now());
    observationRegistry.storeObservations(caseId, "agent-a", List.of(obs));

    var result = provider.observations(caseId, "agent-b");
    assertTrue(result.isEmpty());
  }
}
