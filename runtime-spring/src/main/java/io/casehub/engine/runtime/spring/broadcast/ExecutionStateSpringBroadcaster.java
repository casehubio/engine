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
package io.casehub.engine.runtime.spring.broadcast;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.model.CaseDefinition;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.engine.common.spi.CaseInstanceRepository;
import io.casehub.engine.common.spi.event.CaseContextUpdatedEvent;
import io.casehub.engine.common.spi.event.PlanItemStateChangedEvent;
import io.casehub.engine.common.spi.recovery.ExecutionSnapshotStore;
import io.casehub.engine.plan.execution.CasePlanModelSnapshotProvider;
import io.casehub.engine.plan.execution.ExecutionStateSnapshot;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ExecutionStateSpringBroadcaster {

  private static final Logger LOG = LoggerFactory.getLogger(ExecutionStateSpringBroadcaster.class);

  private record ActiveStream(UUID caseId, SubmissionPublisher<JsonNode> publisher) {}

  private final CopyOnWriteArrayList<ActiveStream> streams = new CopyOnWriteArrayList<>();
  private final CasePlanModelSnapshotProvider planModelProvider;
  private final ExecutionSnapshotStore snapshotStore;
  private final CaseDefinitionRegistry definitionRegistry;
  private final CaseInstanceRepository caseInstanceRepository;
  private final ObjectMapper objectMapper;

  public ExecutionStateSpringBroadcaster(
      CasePlanModelSnapshotProvider planModelProvider,
      ExecutionSnapshotStore snapshotStore,
      CaseDefinitionRegistry definitionRegistry,
      CaseInstanceRepository caseInstanceRepository,
      ObjectMapper objectMapper) {
    this.planModelProvider = planModelProvider;
    this.snapshotStore = snapshotStore;
    this.definitionRegistry = definitionRegistry;
    this.caseInstanceRepository = caseInstanceRepository;
    this.objectMapper = objectMapper;
  }

  @EventListener
  public void onPlanItemChanged(PlanItemStateChangedEvent event) {
    compose(event.caseId(), event.tenancyId());
  }

  @EventListener
  public void onContextUpdated(CaseContextUpdatedEvent event) {
    compose(event.caseId(), event.tenancyId());
  }

  public Flow.Publisher<JsonNode> stream(UUID caseId) {
    var publisher = new SubmissionPublisher<JsonNode>();
    streams.add(new ActiveStream(caseId, publisher));
    return publisher;
  }

  public ExecutionStateSnapshot composeInitial(UUID caseId, String tenancyId) {
    var planModel = planModelProvider.getSnapshot(caseId, tenancyId).orElse(null);
    var dagPlan = snapshotStore.getDagPlan(caseId, tenancyId).orElse(null);
    var dagResult = snapshotStore.getDagResult(caseId, tenancyId).orElse(null);
    if (planModel == null && dagPlan == null && dagResult == null) {
      return null;
    }
    CaseDefinition definition = resolveDefinition(caseId, tenancyId);
    return ExecutionStateSnapshot.compose(caseId, planModel, dagPlan, dagResult, definition);
  }

  private void compose(UUID caseId, String tenancyId) {
    try {
      var planModel = planModelProvider.getSnapshot(caseId, tenancyId).orElse(null);
      var dagPlan = snapshotStore.getDagPlan(caseId, tenancyId).orElse(null);
      var dagResult = snapshotStore.getDagResult(caseId, tenancyId).orElse(null);
      CaseDefinition definition = resolveDefinition(caseId, tenancyId);
      var snapshot =
          ExecutionStateSnapshot.compose(caseId, planModel, dagPlan, dagResult, definition);
      JsonNode json = objectMapper.valueToTree(snapshot);
      dispatch(caseId, json);
    } catch (Exception e) {
      LOG.debug("Failed to compose execution state for case {}: {}", caseId, e.getMessage());
    }
  }

  private void dispatch(UUID caseId, JsonNode json) {
    streams.removeIf(s -> s.publisher().getNumberOfSubscribers() == 0 && s.publisher().isClosed());
    for (var active : streams) {
      if (caseId.equals(active.caseId())) {
        if (active.publisher().getNumberOfSubscribers() == 0) {
          streams.remove(active);
        } else {
          active.publisher().offer(json, (subscriber, dropped) -> false);
        }
      }
    }
  }

  private CaseDefinition resolveDefinition(UUID caseId, String tenancyId) {
    try {
      return caseInstanceRepository
          .findByUuid(caseId, tenancyId)
          .filter(instance -> instance.getCaseMetaModel() != null)
          .map(instance -> definitionRegistry.getCaseDefinition(instance.getCaseMetaModel()))
          .orElse(null);
    } catch (Exception ignored) {
    }
    return null;
  }
}
