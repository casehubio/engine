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
package io.casehub.blackboard.registry;

import io.casehub.blackboard.plan.CasePlanModel;
import io.casehub.blackboard.plan.DefaultCasePlanModel;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared registry of per-case {@link CasePlanModel} instances and the worker-name-to-PlanItemId
 * completion index.
 *
 * <p>Injected by both {@link io.casehub.blackboard.control.PlanningStrategyLoopControl} (which
 * writes entries on Binding selection) and {@link
 * io.casehub.blackboard.handler.PlanItemCompletionHandler} (which reads entries on worker
 * completion). See casehubio/engine#76.
 *
 * <p>State is in-memory and transient — rebuilt from EventLog on engine recovery. Persistence SPI
 * deferred to casehubio/engine#84.
 */
@ApplicationScoped
public class BlackboardRegistry {

  // caseId → CasePlanModel
  private final ConcurrentHashMap<UUID, CasePlanModel> planModels = new ConcurrentHashMap<>();

  // caseId → (workerName → planItemId)
  private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, String>> completionIndex =
      new ConcurrentHashMap<>();

  public CasePlanModel getOrCreate(UUID caseId) {
    return planModels.computeIfAbsent(caseId, DefaultCasePlanModel::new);
  }

  public Optional<CasePlanModel> get(UUID caseId) {
    return Optional.ofNullable(planModels.get(caseId));
  }

  public void indexWorkerForCompletion(UUID caseId, String workerName, String planItemId) {
    completionIndex
        .computeIfAbsent(caseId, k -> new ConcurrentHashMap<>())
        .put(workerName, planItemId);
  }

  public Optional<String> getPlanItemId(UUID caseId, String workerName) {
    ConcurrentHashMap<String, String> index = completionIndex.get(caseId);
    return index == null ? Optional.empty() : Optional.ofNullable(index.get(workerName));
  }
}
