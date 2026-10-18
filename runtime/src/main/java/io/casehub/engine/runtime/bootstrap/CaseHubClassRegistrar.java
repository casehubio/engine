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
package io.casehub.engine.runtime.bootstrap;

import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

// Scans for CaseHub subclasses on the classpath that are NOT CDI-managed beans.
// CDI-managed CaseHub beans are handled by DefaultCaseDefinitionRegistry.
@ApplicationScoped
public class CaseHubClassRegistrar {

  private static final Logger LOG = Logger.getLogger(CaseHubClassRegistrar.class);

  @Inject CaseDefinitionRegistry caseDefinitionRegistry;

  void onStart(@Observes @Priority(150) StartupEvent event) {
    // TODO: use Jandex CompositeIndex to scan for concrete CaseHub subclasses
    //  that are not CDI-managed, instantiate via reflection, and register via
    //  caseDefinitionRegistry.registerCaseDefinition(). This replaces scaffold's
    //  CaseHubClassPathLoader.
    LOG.debug("CaseHubClassRegistrar: Jandex classpath scan not yet implemented");
  }
}
