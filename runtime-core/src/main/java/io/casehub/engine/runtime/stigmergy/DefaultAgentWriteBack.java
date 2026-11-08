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

import com.fasterxml.jackson.databind.JsonNode;
import io.casehub.api.spi.observation.Observation;
import io.casehub.api.spi.stigmergy.AgentWriteBack;
import io.casehub.engine.common.internal.observation.ObservationRegistry;
import io.casehub.engine.common.internal.signal.SignalRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.jspecify.annotations.Nullable;

public class DefaultAgentWriteBack implements AgentWriteBack {

  private static final Duration DEFAULT_HALF_LIFE = Duration.ofMinutes(5);
  private static final int DEFAULT_MAX_PER_CASE = 100;

  private final SignalRegistry signalRegistry;
  private final ObservationRegistry observationRegistry;
  private final @Nullable StigmergyCoordinator coordinator;
  private final ConcurrentHashMap<String, ReentrantLock> observationLocks =
      new ConcurrentHashMap<>();

  public DefaultAgentWriteBack(
      SignalRegistry signalRegistry,
      ObservationRegistry observationRegistry,
      @Nullable StigmergyCoordinator coordinator) {
    this.signalRegistry = signalRegistry;
    this.observationRegistry = observationRegistry;
    this.coordinator = coordinator;
  }

  @Override
  public boolean depositSignal(
      UUID caseId, String agentId, String name, double strength, @Nullable Duration halfLife) {
    if (coordinator == null || !coordinator.isStigmergyCase(caseId)) {
      return false;
    }
    Duration resolvedHalfLife = halfLife != null ? halfLife : DEFAULT_HALF_LIFE;
    return signalRegistry.deposit(
        caseId, name, strength, resolvedHalfLife, agentId, DEFAULT_MAX_PER_CASE);
  }

  @Override
  public void writeObservation(UUID caseId, String agentId, Observation observation) {
    String lockKey = caseId + ":" + agentId;
    ReentrantLock lock = observationLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
    lock.lock();
    try {
      List<Observation> existing = observationRegistry.getObservations(caseId, agentId);
      List<Observation> merged = new ArrayList<>(existing);
      merged.add(observation);
      observationRegistry.storeObservations(caseId, agentId, merged);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void writeContext(UUID caseId, String agentId, String key, JsonNode value) {
    throw new UnsupportedOperationException(
        "writeContext requires agency integration (casehubio/agency#361)");
  }
}
