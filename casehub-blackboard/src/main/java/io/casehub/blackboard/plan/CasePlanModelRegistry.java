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

import io.casehub.api.model.CaseDefinition;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CDI singleton registry mapping CaseDefinition identity (namespace+name+version) to CasePlanModel.
 * Register plans at application startup via {@code @PostConstruct}.
 */
@ApplicationScoped
public class CasePlanModelRegistry {

  private final Map<String, CasePlanModel> registry = new ConcurrentHashMap<>();

  public void register(CaseDefinition definition, CasePlanModel model) {
    registry.put(key(definition), model);
  }

  public Optional<CasePlanModel> find(CaseDefinition definition) {
    return Optional.ofNullable(registry.get(key(definition)));
  }

  public Optional<CasePlanModel> findByKey(String namespace, String name, String version) {
    return Optional.ofNullable(registry.get(namespace + ":" + name + ":" + version));
  }

  private static String key(CaseDefinition definition) {
    return definition.getNamespace() + ":" + definition.getName() + ":" + definition.getVersion();
  }
}
