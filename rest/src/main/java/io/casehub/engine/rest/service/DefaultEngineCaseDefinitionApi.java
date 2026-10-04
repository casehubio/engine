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

import io.casehub.api.engine.rest.EngineCaseDefinitionApi;
import io.casehub.api.view.CaseDefinitionPage;
import io.casehub.api.view.CaseDefinitionView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class DefaultEngineCaseDefinitionApi implements EngineCaseDefinitionApi {

  @Inject CaseDefinitionService caseDefinitionService;

  @Override
  public CaseDefinitionPage listDefinitions(String tenancyId, Integer offset, Integer limit) {
    return caseDefinitionService.listDefinitions(tenancyId, offset, limit);
  }

  @Override
  public List<CaseDefinitionView> getDefinitionsByName(
      String namespace, String name, String tenancyId) {
    return caseDefinitionService.getDefinitionsByName(namespace, name, tenancyId);
  }

  @Override
  public CaseDefinitionView getDefinitionByKey(
      String namespace, String name, String version, String tenancyId) {
    return caseDefinitionService.getDefinitionByKey(namespace, name, version, tenancyId);
  }
}
