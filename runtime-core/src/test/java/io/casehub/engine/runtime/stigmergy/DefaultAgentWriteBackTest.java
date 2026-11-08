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

class DefaultAgentWriteBackTest {

  private SignalRegistry signalRegistry;
  private ObservationRegistry observationRegistry;
  private StigmergyCoordinator coordinator;
  private DefaultAgentWriteBack writer;

  @BeforeEach
  void setUp() {
    signalRegistry = new SignalRegistry();
    observationRegistry = new ObservationRegistry();
    coordinator =
        new StigmergyCoordinator(signalRegistry, observationRegistry, new ActivityTracker());
    writer = new DefaultAgentWriteBack(signalRegistry, observationRegistry, coordinator);
  }

  private UUID initCase() {
    UUID caseId = UUID.randomUUID();
    var defaults =
        new StigmergyDefaults(Duration.ofMinutes(5), 0.01, 100, 50, 50, null, null, null, null);
    var config = new StigmergyConfig(defaults, null, null, null);
    coordinator.initializeCase(caseId, List.of("agent-a", "agent-b"), config, "tenant-1");
    return caseId;
  }

  @Test
  void depositSignal_delegatesToSignalRegistry() {
    UUID caseId = initCase();
    boolean result = writer.depositSignal(caseId, "agent-a", "trail", 0.5, Duration.ofMinutes(5));

    assertTrue(result);
    var signals = signalRegistry.perceive(caseId, 0.01);
    assertEquals(1, signals.size());
    assertTrue(signals.get("trail").sources().contains("agent-a"));
  }

  @Test
  void depositSignal_nullHalfLifeUsesDefault() {
    UUID caseId = initCase();
    boolean result = writer.depositSignal(caseId, "agent-a", "trail", 0.5, null);

    assertTrue(result);
    var signals = signalRegistry.getAllSignals(caseId);
    assertEquals(Duration.ofMinutes(5), signals.get("trail").halfLife());
  }

  @Test
  void depositSignal_uninitializedCase_returnsFalse() {
    boolean result = writer.depositSignal(UUID.randomUUID(), "agent-a", "trail", 0.5, null);
    assertFalse(result);
  }

  @Test
  void depositSignal_nullCoordinator_returnsFalse() {
    var writerNoCoord = new DefaultAgentWriteBack(signalRegistry, observationRegistry, null);
    boolean result = writerNoCoord.depositSignal(UUID.randomUUID(), "agent-a", "trail", 0.5, null);
    assertFalse(result);
  }

  @Test
  void writeObservation_appendsToExisting() {
    UUID caseId = UUID.randomUUID();
    var obs1 = new Observation("pattern-1", 0.9, Map.of(), Instant.now());
    var obs2 = new Observation("pattern-2", 0.7, Map.of(), Instant.now());

    writer.writeObservation(caseId, "agent-a", obs1);
    writer.writeObservation(caseId, "agent-a", obs2);

    var stored = observationRegistry.getObservations(caseId, "agent-a");
    assertEquals(2, stored.size());
    assertEquals("pattern-1", stored.get(0).patternId());
    assertEquals("pattern-2", stored.get(1).patternId());
  }

  @Test
  void writeObservation_differentAgentsIndependent() {
    UUID caseId = UUID.randomUUID();
    var obs1 = new Observation("pattern-1", 0.9, Map.of(), Instant.now());
    var obs2 = new Observation("pattern-2", 0.7, Map.of(), Instant.now());

    writer.writeObservation(caseId, "agent-a", obs1);
    writer.writeObservation(caseId, "agent-b", obs2);

    assertEquals(1, observationRegistry.getObservations(caseId, "agent-a").size());
    assertEquals(1, observationRegistry.getObservations(caseId, "agent-b").size());
  }

  @Test
  void writeContext_throwsUnsupported() {
    assertThrows(
        UnsupportedOperationException.class,
        () ->
            writer.writeContext(
                UUID.randomUUID(),
                "agent-a",
                "key",
                com.fasterxml.jackson.databind.node.TextNode.valueOf("value")));
  }
}
