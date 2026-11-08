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

import com.fasterxml.jackson.databind.JsonNode;
import io.casehub.api.spi.observation.Observation;
import java.time.Duration;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Accepts agent decision write-backs into the engine's stigmergy environment.
 *
 * <p>Processes the three write types from {@code DecisionOutcome.WriteBack}: signal deposits,
 * observations, and context mutations. Named {@code AgentWriteBack} (not {@code StigmergyWriter})
 * because context writes target the WORKING context layer, not the stigmergy subsystem.
 *
 * <p>Implementations must be thread-safe. {@link #writeObservation} uses append semantics —
 * concurrent calls for the same agent must not overwrite each other.
 */
public interface AgentWriteBack {

  boolean depositSignal(
      UUID caseId, String agentId, String name, double strength, @Nullable Duration halfLife);

  void writeObservation(UUID caseId, String agentId, Observation observation);

  void writeContext(UUID caseId, String agentId, String key, JsonNode value);
}
