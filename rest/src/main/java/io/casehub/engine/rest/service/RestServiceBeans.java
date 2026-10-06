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
package io.casehub.engine.rest.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.engine.CaseHubRuntime;
import io.casehub.api.engine.ExpressionEngineRegistry;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.engine.common.spi.CaseInstanceRepository;
import io.casehub.engine.common.spi.CaseMetaModelRepository;
import io.casehub.engine.common.spi.EventLogRepository;
import io.casehub.engine.common.spi.PlanItemStore;
import io.casehub.engine.common.spi.recovery.ExecutionSnapshotStore;
import io.casehub.engine.plan.execution.CasePlanModelSnapshotProvider;
import io.casehub.platform.api.acl.AccessControlProvider;
import io.casehub.platform.api.identity.CurrentPrincipal;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

@ApplicationScoped
public class RestServiceBeans {

  @Inject AccessControlProvider accessControlProvider;
  @Inject CurrentPrincipal currentPrincipal;
  @Inject CaseDefinitionRegistry definitionRegistry;
  @Inject CaseHubRuntime runtime;
  @Inject CaseInstanceRepository instanceRepository;
  @Inject ExpressionEngineRegistry expressionEngineRegistry;
  @Inject PlanItemStore planItemStore;
  @Inject ObjectMapper objectMapper;
  @Inject CaseMetaModelRepository metaModelRepository;
  @Inject EventLogRepository eventLogRepository;
  @Inject CasePlanModelSnapshotProvider planModelProvider;
  @Inject ExecutionSnapshotStore snapshotStore;

  @Produces
  @ApplicationScoped
  public CaseService caseService() {
    return new CaseService(
        accessControlProvider,
        currentPrincipal,
        definitionRegistry,
        runtime,
        instanceRepository,
        expressionEngineRegistry,
        planItemStore,
        objectMapper);
  }

  @Produces
  @ApplicationScoped
  public CaseDefinitionService caseDefinitionService() {
    return new CaseDefinitionService(
        metaModelRepository, definitionRegistry, currentPrincipal, accessControlProvider);
  }

  @Produces
  @ApplicationScoped
  public EventLogService eventLogService(CaseService caseService) {
    return new EventLogService(caseService, eventLogRepository, currentPrincipal, objectMapper);
  }

  @Produces
  @ApplicationScoped
  public PlanService planService(CaseService caseService) {
    return new PlanService(
        caseService, planModelProvider, snapshotStore, currentPrincipal, objectMapper);
  }
}
