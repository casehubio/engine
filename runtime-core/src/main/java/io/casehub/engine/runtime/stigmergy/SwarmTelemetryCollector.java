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

import io.casehub.api.model.signal.Signal;
import io.casehub.api.model.stigmergy.AgentLifecycleState;
import io.casehub.api.model.stigmergy.AgentState;
import io.casehub.api.model.stigmergy.AgentWorkState;
import io.casehub.api.model.stigmergy.SwarmTelemetry;
import io.casehub.engine.common.internal.signal.SignalRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SwarmTelemetryCollector {

  private final StigmergyCoordinator coordinator;
  private final SignalRegistry signalRegistry;

  public SwarmTelemetryCollector(StigmergyCoordinator coordinator, SignalRegistry signalRegistry) {
    this.coordinator = coordinator;
    this.signalRegistry = signalRegistry;
  }

  public List<SwarmTelemetry> snapshot(UUID caseId) {
    return snapshot(caseId, Instant.now());
  }

  public List<SwarmTelemetry> snapshot(UUID caseId, Instant now) {
    var agents = coordinator.allAgents(caseId);
    if (agents.isEmpty()) return List.of();

    Map<String, Integer> signalCounts = countSignalsPerAgent(caseId);
    List<SwarmTelemetry> result = new ArrayList<>();

    for (var agent : agents) {
      var counters = coordinator.getAgentCounters(caseId, agent.agentId());
      Duration activeTime = computeActiveTime(agent, now);
      AgentWorkState workState = counters != null ? counters.workState() : AgentWorkState.IDLE;
      int messagesSent = counters != null ? counters.messagesSent() : 0;
      int tasksCompleted = counters != null ? counters.tasksCompleted() : 0;
      int signalsDeposited = signalCounts.getOrDefault(agent.agentId(), 0);

      result.add(
          new SwarmTelemetry(
              caseId,
              agent.agentId(),
              workState,
              signalsDeposited,
              messagesSent,
              tasksCompleted,
              activeTime));
    }
    return List.copyOf(result);
  }

  private Duration computeActiveTime(AgentState agent, Instant now) {
    if (agent.activatedAt() == null) return Duration.ZERO;
    if (agent.state() == AgentLifecycleState.DEPARTED && agent.departedAt() != null) {
      return Duration.between(agent.activatedAt(), agent.departedAt());
    }
    return Duration.between(agent.activatedAt(), now);
  }

  private Map<String, Integer> countSignalsPerAgent(UUID caseId) {
    Map<String, Integer> counts = new HashMap<>();
    for (Signal signal : signalRegistry.getAllSignals(caseId).values()) {
      if (signal.expired()) continue;
      for (String source : signal.sources()) {
        counts.merge(source, 1, Integer::sum);
      }
    }
    return counts;
  }
}
