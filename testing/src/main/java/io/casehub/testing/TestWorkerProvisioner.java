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
package io.casehub.testing;

import io.casehub.api.model.ProvisionContext;
import io.casehub.api.spi.ProvisionResult;
import io.casehub.api.spi.WorkerProvisioner;
import io.casehub.engine.common.internal.event.EventBusAddresses;
import io.casehub.engine.common.internal.event.WorkflowExecutionCompleted;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.engine.common.spi.CrossTenantCaseInstanceRepository;
import io.casehub.worker.api.Worker;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jboss.logging.Logger;

@Alternative
@Priority(1)
@ApplicationScoped
public class TestWorkerProvisioner implements WorkerProvisioner {

  private static final Logger LOG = Logger.getLogger(TestWorkerProvisioner.class);

  @Inject CrossTenantCaseInstanceRepository caseInstanceRepository;
  @Inject CaseDefinitionRegistry caseDefinitionRegistry;
  @Inject EventBus eventBus;

  @Override
  public ProvisionResult provision(Set<String> capabilities, ProvisionContext context) {
    String taskType = context.taskType();
    LOG.infof(
        "TestWorkerProvisioner: auto-completing capability '%s' for case %s",
        taskType, context.caseId());

    io.vertx.mutiny.core.Vertx.currentContext()
        .owner()
        .setTimer(
            50,
            id -> {
              try {
                CaseInstance instance =
                    caseInstanceRepository.findByUuid(context.caseId()).orElse(null);
                if (instance == null) return;
                var definition =
                    caseDefinitionRegistry.getCaseDefinition(instance.getCaseMetaModel());
                Worker worker =
                    definition.getWorkers().stream()
                        .filter(w -> w.capabilities().contains(taskType))
                        .findFirst()
                        .orElse(null);
                if (worker == null) {
                  LOG.warnf(
                      "TestWorkerProvisioner: no worker for capability '%s' in case definition",
                      taskType);
                  return;
                }
                eventBus.publish(
                    EventBusAddresses.WORKER_EXECUTION_FINISHED,
                    WorkflowExecutionCompleted.approved(
                        instance,
                        worker,
                        UUID.randomUUID().toString(),
                        Map.of("autoCompleted", true, "capability", taskType),
                        null));
                LOG.infof(
                    "TestWorkerProvisioner: auto-completed worker '%s' for case %s",
                    worker.name(), context.caseId());
              } catch (Exception e) {
                LOG.debugf(
                    e,
                    "TestWorkerProvisioner: auto-completion failed for case %s",
                    context.caseId());
              }
            });

    return ProvisionResult.empty();
  }

  @Override
  public void terminate(String workerId, String tenancyId) {}

  @Override
  public Set<String> getCapabilities() {
    return Set.of();
  }
}
