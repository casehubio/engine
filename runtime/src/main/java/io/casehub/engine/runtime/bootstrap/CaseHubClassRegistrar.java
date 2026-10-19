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
    var indexes = new java.util.ArrayList<org.jboss.jandex.IndexView>();
    try {
      var resources =
          Thread.currentThread().getContextClassLoader().getResources("META-INF/jandex.idx");
      while (resources.hasMoreElements()) {
        try (var is = resources.nextElement().openStream()) {
          indexes.add(new org.jboss.jandex.IndexReader(is).read());
        }
      }
    } catch (java.io.IOException e) {
      LOG.warn("Failed to read Jandex indexes from classpath", e);
      return;
    }

    if (indexes.isEmpty()) {
      LOG.debug("No Jandex indexes found on classpath — skipping CaseHub class scan");
      return;
    }

    var composite = org.jboss.jandex.CompositeIndex.create(indexes);
    var caseHubName =
        org.jboss.jandex.DotName.createSimple(io.casehub.api.engine.CaseHub.class.getName());
    var subclasses = composite.getAllKnownSubclasses(caseHubName);
    int registered = 0;

    for (var classInfo : subclasses) {
      if (java.lang.reflect.Modifier.isAbstract(classInfo.flags())) {
        continue;
      }

      Class<?> clazz;
      try {
        clazz =
            Thread.currentThread().getContextClassLoader().loadClass(classInfo.name().toString());
      } catch (ClassNotFoundException e) {
        LOG.debugf("CaseHub subclass %s not loadable — skipping", classInfo.name());
        continue;
      }

      if (io.quarkus.arc.Arc.container().instance(clazz).isAvailable()) {
        continue;
      }

      try {
        var constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        var hub = (io.casehub.api.engine.CaseHub) constructor.newInstance();
        var definition = hub.getDefinition();
        caseDefinitionRegistry.registerCaseDefinition(definition);
        registered++;
        LOG.debugf(
            "Registered non-CDI CaseHub definition from %s: %s",
            clazz.getName(), definition.getName());
      } catch (NoSuchMethodException e) {
        LOG.warnf("CaseHub subclass %s has no no-arg constructor — skipping", clazz.getName());
      } catch (Exception e) {
        LOG.warnf(e, "Failed to instantiate CaseHub subclass %s", clazz.getName());
      }
    }

    if (registered > 0) {
      LOG.infof("CaseHubClassRegistrar: registered %d non-CDI case definition(s)", registered);
    } else {
      LOG.debug("CaseHubClassRegistrar: no non-CDI CaseHub subclasses found");
    }
  }
}
