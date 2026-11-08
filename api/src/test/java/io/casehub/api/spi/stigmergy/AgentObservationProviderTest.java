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
package io.casehub.api.spi.stigmergy;

import static org.junit.jupiter.api.Assertions.*;

import io.casehub.api.model.signal.PerceivedSignal;
import io.casehub.api.spi.observation.Observation;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AgentObservationProviderTest {

  @Test
  void perceiveSignals_returnsMapOfPerceivedSignals() {
    AgentObservationProvider provider =
        new AgentObservationProvider() {
          @Override
          public Map<String, PerceivedSignal> perceiveSignals(UUID caseId) {
            return Map.of(
                "trail",
                new PerceivedSignal(
                    "trail", 0.8, 3, "agent-a", Duration.ofSeconds(30), Set.of("agent-a")));
          }

          @Override
          public List<Observation> observations(UUID caseId, String agentId) {
            return List.of();
          }
        };

    var signals = provider.perceiveSignals(UUID.randomUUID());
    assertEquals(1, signals.size());
    assertEquals(0.8, signals.get("trail").effectiveStrength());
  }

  @Test
  void observations_returnsAgentScopedObservations() {
    var obs = new Observation("pattern-1", 0.9, Map.of(), Instant.now());
    AgentObservationProvider provider =
        new AgentObservationProvider() {
          @Override
          public Map<String, PerceivedSignal> perceiveSignals(UUID caseId) {
            return Map.of();
          }

          @Override
          public List<Observation> observations(UUID caseId, String agentId) {
            return "agent-a".equals(agentId) ? List.of(obs) : List.of();
          }
        };

    assertEquals(1, provider.observations(UUID.randomUUID(), "agent-a").size());
    assertEquals(0, provider.observations(UUID.randomUUID(), "agent-b").size());
  }
}
