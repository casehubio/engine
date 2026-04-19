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
package io.casehub.blackboard.plan;

import io.casehub.blackboard.stage.Stage;
import io.casehub.blackboard.stage.StageStatus;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Collectors;

/**
 * Thread-safe in-memory {@link CasePlanModel} implementation. Plan state is transient — rebuilt
 * from EventLog on engine recovery. See casehubio/engine#76. Persistence SPI deferred — see
 * casehubio/engine#84.
 */
public class DefaultCasePlanModel implements CasePlanModel {

  private final UUID caseId;
  private final PriorityBlockingQueue<PlanItem> agenda = new PriorityBlockingQueue<>();
  private final ConcurrentHashMap<String, PlanItem> itemsById = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Stage> stages = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Boolean> milestones = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Object> state = new ConcurrentHashMap<>();
  private volatile Map<String, Object> resourceBudget = Map.of();
  private volatile String focus;
  private volatile String focusRationale;

  public DefaultCasePlanModel(UUID caseId) {
    this.caseId = caseId;
  }

  @Override
  public UUID getCaseId() {
    return caseId;
  }

  @Override
  public void addPlanItem(PlanItem item) {
    agenda.add(item);
    itemsById.put(item.getPlanItemId(), item);
  }

  @Override
  public void removePlanItem(String planItemId) {
    PlanItem item = itemsById.remove(planItemId);
    if (item != null) agenda.remove(item);
  }

  @Override
  public Optional<PlanItem> getPlanItem(String planItemId) {
    return Optional.ofNullable(itemsById.get(planItemId));
  }

  @Override
  public List<PlanItem> getAgenda() {
    return agenda.stream()
        .filter(p -> p.getStatus() == PlanItem.PlanItemStatus.PENDING)
        .sorted()
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<PlanItem> getTopPlanItems(int maxCount) {
    List<PlanItem> all = getAgenda();
    return Collections.unmodifiableList(all.size() <= maxCount ? all : all.subList(0, maxCount));
  }

  @Override
  public void addStage(Stage stage) {
    stages.put(stage.getStageId(), stage);
  }

  @Override
  public Optional<Stage> getStage(String stageId) {
    return Optional.ofNullable(stages.get(stageId));
  }

  @Override
  public List<Stage> getPendingStages() {
    return stages.values().stream()
        .filter(s -> s.getStatus() == StageStatus.PENDING)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Stage> getActiveStages() {
    return stages.values().stream()
        .filter(s -> s.getStatus() == StageStatus.ACTIVE)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Stage> getAllStages() {
    return List.copyOf(stages.values());
  }

  @Override
  public void trackMilestone(String name) {
    milestones.putIfAbsent(name, Boolean.FALSE);
  }

  @Override
  public void achieveMilestone(String name) {
    milestones.computeIfPresent(name, (k, v) -> Boolean.TRUE);
  }

  @Override
  public boolean isMilestoneAchieved(String name) {
    return Boolean.TRUE.equals(milestones.get(name));
  }

  @Override
  public void setFocus(String f) {
    this.focus = f;
  }

  @Override
  public Optional<String> getFocus() {
    return Optional.ofNullable(focus);
  }

  @Override
  public void setFocusRationale(String r) {
    this.focusRationale = r;
  }

  @Override
  public void setResourceBudget(Map<String, Object> budget) {
    this.resourceBudget = Map.copyOf(budget);
  }

  @Override
  public Map<String, Object> getResourceBudget() {
    return resourceBudget;
  }

  @Override
  public void put(String key, Object value) {
    state.put(key, value);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Optional<T> get(String key, Class<T> type) {
    Object v = state.get(key);
    return (v != null && type.isInstance(v)) ? Optional.of((T) v) : Optional.empty();
  }
}
