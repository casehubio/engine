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
package io.casehub.engine.yamlcbr.quarkus;

import io.casehub.api.spi.YamlStepExecutionObserver;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.engine.flow.CallableDispatchRegistry;
import io.casehub.engine.yamlcbr.PlaybookContextExtractor;
import io.casehub.engine.yamlcbr.StepExecutionCbrBridge;
import io.casehub.engine.yamlcbr.StepFileCallableDispatcher;
import io.casehub.neocortex.memory.cbr.CbrRecordStore;
import io.casehub.yaml.plugin.api.PluginRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class YamlCbrBeans {

  @Produces
  @ApplicationScoped
  YamlStepExecutionObserver stepExecutionCbrBridge(
      CbrRecordStore cbrStore,
      CaseDefinitionRegistry registry,
      Instance<PlaybookContextExtractor> extractors) {
    return new StepExecutionCbrBridge(cbrStore, registry, extractors.stream().toList());
  }

  @Produces
  @ApplicationScoped
  StepFileCallableDispatcher stepFileCallableDispatcher(
      PluginRegistry pluginRegistry, CallableDispatchRegistry dispatchRegistry) {
    return new StepFileCallableDispatcher(pluginRegistry, dispatchRegistry);
  }
}
