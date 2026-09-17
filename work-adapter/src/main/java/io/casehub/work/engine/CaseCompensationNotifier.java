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
package io.casehub.work.engine;

import static io.casehub.platform.api.identity.TenancyConstants.PLATFORM_TENANT_ID;
import static io.casehub.platform.api.subscription.SubscriptionConstants.NOTIFICATION_DATASOURCE_PATH;

import io.casehub.engine.common.spi.event.CaseLifecycleEvent;
import io.casehub.platform.api.datasource.DataSource;
import io.casehub.platform.api.datasource.DataSourceRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.Set;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CaseCompensationNotifier {

  private static final Logger LOG = Logger.getLogger(CaseCompensationNotifier.class);

  private static final Set<String> COMPENSATION_EVENT_TYPES =
      Set.of("CaseCompensating", "CaseCompensated", "CaseCompensationFaulted");

  @Inject Instance<DataSourceRegistry> dataSourceRegistryInstance;

  void onCaseLifecycle(@ObservesAsync CaseLifecycleEvent event) {
    if (!COMPENSATION_EVENT_TYPES.contains(event.eventType())) {
      return;
    }
    if (dataSourceRegistryInstance.isUnsatisfied()) {
      return;
    }
    CaseCompensationEvent.Kind kind =
        switch (event.eventType()) {
          case "CaseCompensating" -> CaseCompensationEvent.Kind.STARTED;
          case "CaseCompensated" -> CaseCompensationEvent.Kind.COMPLETED;
          case "CaseCompensationFaulted" -> CaseCompensationEvent.Kind.FAULTED;
          default -> null;
        };
    if (kind == null) {
      return;
    }
    fire(
        new CaseCompensationEvent(
            kind,
            event.tenancyId(),
            event.caseId(),
            event.caseDefinitionName(),
            event.caseStatus(),
            event.actorId()));
  }

  private void fire(CaseCompensationEvent event) {
    try {
      Optional<DataSource<?>> ds =
          dataSourceRegistryInstance
              .get()
              .resolveSource(NOTIFICATION_DATASOURCE_PATH, PLATFORM_TENANT_ID);
      if (ds.isEmpty()) {
        LOG.warnf("Notification DataSource not available — dropping %s event", event.kind());
        return;
      }
      @SuppressWarnings("unchecked")
      DataSource<Object> source = (DataSource<Object>) ds.get();
      source.add(event);
    } catch (Exception e) {
      LOG.warnf("Failed to fire compensation event %s: %s", event.kind(), e.getMessage());
    }
  }
}
