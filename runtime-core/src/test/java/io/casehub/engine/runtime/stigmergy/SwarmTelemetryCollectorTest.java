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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.casehub.api.model.stigmergy.AgentWorkState;
import io.casehub.api.model.stigmergy.CoordinationConfig;
import io.casehub.api.model.stigmergy.StigmergyConfig;
import io.casehub.api.model.stigmergy.StigmergyDefaults;
import io.casehub.api.model.stigmergy.SwarmConfig;
import io.casehub.engine.common.internal.convergence.ActivityTracker;
import io.casehub.engine.common.internal.observation.ObservationRegistry;
import io.casehub.engine.common.internal.signal.SignalRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SwarmTelemetryCollectorTest {

  private StigmergyCoordinator coordinator;
  private SignalRegistry signalRegistry;
  private SwarmTelemetryCollector collector;
  private UUID caseId;
  private StigmergyConfig config;

  @BeforeEach
  void setUp() {
    signalRegistry = new SignalRegistry();
    var observationRegistry = new ObservationRegistry();
    var activityTracker = new ActivityTracker();
    coordinator = new StigmergyCoordinator(signalRegistry, observationRegistry, activityTracker);
    collector = new SwarmTelemetryCollector(coordinator, signalRegistry);
    caseId = UUID.randomUUID();
    config =
        new StigmergyConfig(
            new StigmergyDefaults(null, 0.01, null, null, null, null, null, null, null),
            new CoordinationConfig(2, 10.0, 0.6),
            new SwarmConfig(null, 0.5, 2, 20, 1, null, 0.3, 2, 0.05));
  }

  @Test
  void snapshot_emptyCase_returnsEmptyList() {
    var telemetry = collector.snapshot(caseId);
    assertTrue(telemetry.isEmpty());
  }

  @Test
  void snapshot_activeAgent_computesActiveTime() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test");
    coordinator.agentJoined(caseId, "agent-1", "b-1");
    coordinator.agentActivated(caseId, "agent-1");

    var now = Instant.now().plusSeconds(60);
    var telemetry = collector.snapshot(caseId, now);

    assertEquals(1, telemetry.size());
    var t = telemetry.get(0);
    assertEquals(caseId, t.caseId());
    assertEquals("agent-1", t.agentId());
    assertTrue(t.activeTime().getSeconds() >= 59);
  }

  @Test
  void snapshot_joiningAgent_zeroActiveTime() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test");
    coordinator.agentJoined(caseId, "agent-1", "b-1");

    var telemetry = collector.snapshot(caseId);

    assertEquals(1, telemetry.size());
    assertEquals(Duration.ZERO, telemetry.get(0).activeTime());
  }

  @Test
  void snapshot_departedAgent_fixedActiveTime() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test");
    coordinator.agentJoined(caseId, "agent-1", "b-1");
    coordinator.agentActivated(caseId, "agent-1");
    coordinator.agentDeparted(caseId, "agent-1");

    var telemetry = collector.snapshot(caseId);

    assertEquals(1, telemetry.size());
    assertNotNull(telemetry.get(0).activeTime());
    assertTrue(telemetry.get(0).activeTime().compareTo(Duration.ZERO) >= 0);
  }

  @Test
  void snapshot_reflectsCounters() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test");
    coordinator.agentJoined(caseId, "agent-1", "b-1");
    coordinator.agentActivated(caseId, "agent-1");

    coordinator.recordAgentMessage(caseId, "agent-1");
    coordinator.recordAgentMessage(caseId, "agent-1");
    coordinator.recordAgentTaskCompleted(caseId, "agent-1");
    coordinator.updateAgentWorkState(caseId, "agent-1", AgentWorkState.EXECUTING);

    var telemetry = collector.snapshot(caseId);

    assertEquals(1, telemetry.size());
    var t = telemetry.get(0);
    assertEquals(2, t.messagesSent());
    assertEquals(1, t.tasksCompleted());
    assertEquals(AgentWorkState.EXECUTING, t.workState());
  }

  @Test
  void snapshot_countsSignalsPerAgent() {
    coordinator.initializeCase(caseId, List.of("agent-1", "agent-2"), config, "test");
    coordinator.agentJoined(caseId, "agent-1", "b-1");
    coordinator.agentJoined(caseId, "agent-2", "b-2");
    coordinator.agentActivated(caseId, "agent-1");
    coordinator.agentActivated(caseId, "agent-2");

    signalRegistry.deposit(caseId, "sig-a", 0.8, Duration.ofMinutes(5), "agent-1", 100);
    signalRegistry.deposit(caseId, "sig-b", 0.7, Duration.ofMinutes(5), "agent-1", 100);
    signalRegistry.deposit(caseId, "sig-c", 0.9, Duration.ofMinutes(5), "agent-2", 100);

    var telemetry = collector.snapshot(caseId);

    assertEquals(2, telemetry.size());
    var t1 =
        telemetry.stream().filter(t -> t.agentId().equals("agent-1")).findFirst().orElseThrow();
    var t2 =
        telemetry.stream().filter(t -> t.agentId().equals("agent-2")).findFirst().orElseThrow();
    assertEquals(2, t1.signalsDeposited());
    assertEquals(1, t2.signalsDeposited());
  }

  @Test
  void snapshot_multipleAgents_allIncluded() {
    coordinator.initializeCase(caseId, List.of("a", "b", "c"), config, "test");
    for (String id : List.of("a", "b", "c")) {
      coordinator.agentJoined(caseId, id, "b-" + id);
      coordinator.agentActivated(caseId, id);
    }

    var telemetry = collector.snapshot(caseId);

    assertEquals(3, telemetry.size());
  }
}
