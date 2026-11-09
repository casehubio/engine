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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.casehub.api.spi.observation.Observation;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AgentWriteBackTest {

  @Test
  void depositSignal_returnsBoolean() {
    var deposited = new AtomicBoolean(false);
    AgentWriteBack writer = testWriter(deposited, null, null);

    boolean result =
        writer.depositSignal(UUID.randomUUID(), "agent-a", "trail", 0.5, Duration.ofMinutes(5));
    assertTrue(result);
    assertTrue(deposited.get());
  }

  @Test
  void depositSignal_nullHalfLifeAllowed() {
    var deposited = new AtomicBoolean(false);
    AgentWriteBack writer = testWriter(deposited, null, null);

    boolean result = writer.depositSignal(UUID.randomUUID(), "agent-a", "trail", 0.5, null);
    assertTrue(result);
  }

  @Test
  void writeObservation_acceptsSingleObservation() {
    var captured = new AtomicReference<Observation>();
    AgentWriteBack writer = testWriter(null, captured, null);

    var obs = new Observation("pattern-1", 0.9, Map.of(), Instant.now());
    writer.writeObservation(UUID.randomUUID(), "agent-a", obs);
    assertEquals("pattern-1", captured.get().patternId());
  }

  @Test
  void writeContext_acceptsJsonNode() {
    var captured = new AtomicReference<JsonNode>();
    AgentWriteBack writer = testWriter(null, null, captured);

    writer.writeContext(UUID.randomUUID(), "agent-a", "status", new TextNode("active"));
    assertEquals("active", captured.get().asText());
  }

  private AgentWriteBack testWriter(
      AtomicBoolean deposited,
      AtomicReference<Observation> obsRef,
      AtomicReference<JsonNode> ctxRef) {
    return new AgentWriteBack() {
      @Override
      public boolean depositSignal(
          UUID caseId, String agentId, String name, double strength, Duration halfLife) {
        if (deposited != null) deposited.set(true);
        return true;
      }

      @Override
      public void writeObservation(UUID caseId, String agentId, Observation observation) {
        if (obsRef != null) obsRef.set(observation);
      }

      @Override
      public void writeContext(UUID caseId, String agentId, String key, JsonNode value) {
        if (ctxRef != null) ctxRef.set(value);
      }
    };
  }
}
