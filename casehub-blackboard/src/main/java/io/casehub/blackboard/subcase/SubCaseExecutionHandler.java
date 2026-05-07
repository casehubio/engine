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
package io.casehub.blackboard.subcase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.casehub.api.engine.CaseHubRuntime;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.SubCase;
import io.casehub.api.model.event.CaseHubEventType;
import io.casehub.api.model.event.EventStreamType;
import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.SubCaseScheduleEvent;
import io.casehub.engine.internal.history.EventLog;
import io.casehub.engine.internal.model.CaseInstance;
import io.casehub.engine.internal.model.CaseMetaModel;
import io.casehub.engine.internal.work.PendingWorkRegistry;
import io.casehub.engine.spi.CaseDefinitionRegistry;
import io.casehub.engine.spi.CaseInstanceRepository;
import io.casehub.engine.spi.EventLogRepository;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import org.jboss.logging.Logger;

/**
 * Consumes {@link EventBusAddresses#SUBCASE_SCHEDULE} events and spawns a child CaseInstance. When
 * {@code waitForCompletion=true}, transitions the parent case to WAITING.
 *
 * <p>See casehubio/engine#195.
 */
@ApplicationScoped
public class SubCaseExecutionHandler {

  private static final Logger LOG = Logger.getLogger(SubCaseExecutionHandler.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Inject CaseHubRuntime caseHubRuntime;
  @Inject CaseDefinitionRegistry caseDefinitionRegistry;
  @Inject CaseInstanceRepository caseInstanceRepository;
  @Inject EventLogRepository eventLogRepository;
  @Inject PendingWorkRegistry pendingWorkRegistry;

  @ConsumeEvent(value = EventBusAddresses.SUBCASE_SCHEDULE, blocking = true)
  public Uni<Void> onSubCaseSchedule(SubCaseScheduleEvent event) {
    CaseInstance parent = event.parentInstance();
    SubCase subCase = event.subCase();

    // Circular detection: reject if child definition matches parent
    CaseMetaModel parentMeta = parent.getCaseMetaModel();
    if (parentMeta != null
        && subCase.namespace().equals(parentMeta.getNamespace())
        && subCase.name().equals(parentMeta.getName())
        && subCase.version().equals(parentMeta.getVersion())) {
      LOG.errorf(
          "SubCase circular dependency detected: case %s cannot spawn itself (%s/%s/%s)",
          parent.getUuid(), subCase.namespace(), subCase.name(), subCase.version());
      return Uni.createFrom().voidItem();
    }

    // Resolve child CaseDefinition
    CaseMetaModel childMeta = new CaseMetaModel();
    childMeta.setNamespace(subCase.namespace());
    childMeta.setName(subCase.name());
    childMeta.setVersion(subCase.version());

    var childDefinition = caseDefinitionRegistry.getCaseDefinition(childMeta);
    if (childDefinition == null) {
      LOG.errorf(
          "SubCaseExecutionHandler: no CaseDefinition for %s/%s/%s",
          subCase.namespace(), subCase.name(), subCase.version());
      return Uni.createFrom().voidItem();
    }

    // Start child case
    CompletionStage<UUID> childFuture =
        caseHubRuntime.startCase(childDefinition, event.childInitialContext());
    UUID childCaseId = childFuture.toCompletableFuture().join();

    LOG.infof(
        "SubCase spawned: parentCaseId=%s childCaseId=%s waitForCompletion=%s",
        parent.getUuid(), childCaseId, subCase.waitForCompletion());

    // Write SUBCASE_STARTED EventLog on parent
    EventLog startedLog = new EventLog();
    startedLog.setCaseId(parent.getUuid());
    startedLog.setWorkerId(childCaseId.toString());
    startedLog.setEventType(CaseHubEventType.SUBCASE_STARTED);
    startedLog.setStreamType(EventStreamType.CASE);
    startedLog.setTimestamp(Instant.now());
    ObjectNode meta = OBJECT_MAPPER.createObjectNode();
    meta.put("childCaseId", childCaseId.toString());
    meta.put("waitForCompletion", subCase.waitForCompletion());
    if (subCase.outputMapping() != null) {
      meta.put("outputMapping", subCase.outputMapping());
    }
    startedLog.setMetadata(meta);

    if (subCase.waitForCompletion()) {
      pendingWorkRegistry.register(childCaseId.toString());
      parent.setState(CaseStatus.WAITING);
      parent.setWaitingForWorkId(childCaseId.toString());
      // updateStateAndAppendEvent atomically persists state + appends the EventLog — no separate
      // append needed
      return caseInstanceRepository.updateStateAndAppendEvent(parent, startedLog).replaceWithVoid();
    } else {
      return eventLogRepository.append(startedLog).replaceWithVoid();
    }
  }
}
