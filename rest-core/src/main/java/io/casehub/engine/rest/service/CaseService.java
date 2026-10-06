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
import io.casehub.api.acl.EngineResourceTypes;
import io.casehub.api.context.CaseContext;
import io.casehub.api.engine.CaseHubRuntime;
import io.casehub.api.engine.EntityNotFoundException;
import io.casehub.api.engine.ExpressionEngineRegistry;
import io.casehub.api.model.CaseCompletion;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseStatus;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalBasedCompletion;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.PredicateBasedCompletion;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.casehub.api.view.CaseContextPathView;
import io.casehub.api.view.CaseContextView;
import io.casehub.api.view.CaseInstanceView;
import io.casehub.api.view.CasePage;
import io.casehub.api.view.CompletionSummaryView;
import io.casehub.api.view.GoalEvaluationView;
import io.casehub.api.view.GoalStatusView;
import io.casehub.api.view.PlanItemView;
import io.casehub.api.view.StartCaseRequest;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.internal.model.CaseMetaModel;
import io.casehub.engine.common.internal.model.PlanItemRecord;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.engine.common.spi.CaseInstanceRepository;
import io.casehub.engine.common.spi.PlanItemStore;
import io.casehub.engine.common.spi.query.CaseInstanceQuery;
import io.casehub.platform.api.acl.AccessControlProvider;
import io.casehub.platform.api.acl.AccessDeniedException;
import io.casehub.platform.api.acl.AclAction;
import io.casehub.platform.api.acl.ResourceId;
import io.casehub.platform.api.identity.CurrentPrincipal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseService {

  private static final Logger LOG = LoggerFactory.getLogger(CaseService.class);

  private final AccessControlProvider accessControlProvider;
  private final CurrentPrincipal currentPrincipal;
  private final CaseDefinitionRegistry definitionRegistry;
  private final CaseHubRuntime runtime;
  private final CaseInstanceRepository instanceRepository;
  private final ExpressionEngineRegistry expressionEngineRegistry;
  private final PlanItemStore planItemStore;
  private final ObjectMapper objectMapper;

  public CaseService(
      AccessControlProvider accessControlProvider,
      CurrentPrincipal currentPrincipal,
      CaseDefinitionRegistry definitionRegistry,
      CaseHubRuntime runtime,
      CaseInstanceRepository instanceRepository,
      ExpressionEngineRegistry expressionEngineRegistry,
      PlanItemStore planItemStore,
      ObjectMapper objectMapper) {
    this.accessControlProvider = accessControlProvider;
    this.currentPrincipal = currentPrincipal;
    this.definitionRegistry = definitionRegistry;
    this.runtime = runtime;
    this.instanceRepository = instanceRepository;
    this.expressionEngineRegistry = expressionEngineRegistry;
    this.planItemStore = planItemStore;
    this.objectMapper = objectMapper;
  }

  public CaseInstance startCase(
      String namespace,
      String name,
      String version,
      Map<String, Object> context,
      String tenancyId) {
    var metaModel =
        definitionRegistry
            .findByIdentity(namespace, name, version)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        String.format("No definition for %s/%s/%s", namespace, name, version)));

    var definition = definitionRegistry.getCaseDefinition(metaModel);
    if (definition == null) {
      throw new EntityNotFoundException(
          String.format(
              "Definition metadata exists but body not found for %s/%s/%s",
              namespace, name, version));
    }

    UUID caseId = runtime.startCase(definition, context);

    CaseInstance instance =
        instanceRepository
            .findByUuid(caseId, tenancyId)
            .orElseThrow(
                () ->
                    new RuntimeException(
                        "Case created (id=" + caseId + ") but not found in repository"));
    return instance;
  }

  public CaseInstance requireCase(UUID caseId, String tenancyId) {
    return instanceRepository
        .findByUuid(caseId, tenancyId)
        .orElseThrow(() -> new EntityNotFoundException("Case not found: " + caseId));
  }

  public CaseInstance requireCaseAccess(UUID caseId, AclAction action) {
    String tenancyId = currentPrincipal.tenancyId();
    CaseInstance instance =
        instanceRepository
            .findByUuid(caseId, tenancyId)
            .orElseThrow(() -> new EntityNotFoundException("Case not found: " + caseId));
    String actorId = currentPrincipal.actorId();
    ResourceId resourceId = new ResourceId(EngineResourceTypes.CASE, caseId.toString());
    if (!accessControlProvider.canAccess(actorId, resourceId, action)) {
      LOG.warn("ACL denied: actor={} resource={} action={}", actorId, resourceId, action);
      throw new AccessDeniedException(actorId, resourceId, action);
    }
    return instance;
  }

  public CasePage listCases(
      CaseStatus status,
      String namespace,
      String name,
      String tenancyId,
      Integer offset,
      Integer limit) {
    int page = offset != null ? offset : 0;
    int size = limit != null ? limit : 20;
    var query =
        CaseInstanceQuery.builder()
            .status(status)
            .namespace(namespace)
            .name(name)
            .page(page)
            .size(size)
            .build();
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    String actorId = currentPrincipal.actorId();
    var items =
        instanceRepository.query(query, resolvedTenancyId).stream()
            .filter(
                ci ->
                    accessControlProvider.canAccess(
                        actorId,
                        new ResourceId(EngineResourceTypes.CASE, ci.getUuid().toString()),
                        AclAction.READ))
            .map(this::mapInstance)
            .toList();
    long total = items.size();
    return new CasePage(items, total, total > (long) page * size + size);
  }

  public CaseInstanceView startCaseAndMap(StartCaseRequest request, String tenancyId) {
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    var instance =
        startCase(
            request.namespace(),
            request.name(),
            request.version(),
            request.context(),
            resolvedTenancyId);
    return mapInstance(instance);
  }

  public CaseContextView getCaseContext(UUID caseId, String tenancyId) {
    requireCaseAccess(caseId, AclAction.READ);
    Object context = runtime.query(caseId, ".");
    return new CaseContextView(objectMapper.valueToTree(context));
  }

  public CaseContextPathView getCaseContextPath(UUID caseId, String path, String tenancyId) {
    requireCaseAccess(caseId, AclAction.READ);
    Object value = runtime.query(caseId, path);
    return new CaseContextPathView(path, objectMapper.valueToTree(value));
  }

  public List<PlanItemView> getPlanItems(UUID caseId, String tenancyId) {
    requireCaseAccess(caseId, AclAction.READ);
    String resolvedTenancyId = tenancyId != null ? tenancyId : currentPrincipal.tenancyId();
    return planItemStore.findByCaseId(caseId, resolvedTenancyId).stream()
        .map(this::mapPlanItem)
        .toList();
  }

  public CaseInstanceView mapInstance(CaseInstance instance) {
    CaseMetaModel meta = instance.getCaseMetaModel();
    return new CaseInstanceView(
        instance.getUuid(),
        instance.getState(),
        meta.getNamespace(),
        meta.getName(),
        meta.getVersion(),
        instance.getCreatedAt(),
        instance.getActorId());
  }

  public PlanItemView mapPlanItem(PlanItemRecord record) {
    return new PlanItemView(
        UUID.fromString(record.planItemId()),
        record.bindingName(),
        record.status(),
        record.targetType().name().toLowerCase().replace('_', '-'),
        record.parentCompoundId() != null ? UUID.fromString(record.parentCompoundId()) : null);
  }

  public GoalEvaluationView evaluateGoals(UUID caseId, String tenancyId) {
    CaseInstance instance = requireCaseAccess(caseId, AclAction.READ);

    CaseMetaModel meta = instance.getCaseMetaModel();
    CaseDefinition definition = definitionRegistry.getCaseDefinition(meta);
    if (definition == null) {
      throw new EntityNotFoundException("Case definition not found for case: " + caseId);
    }

    CaseContext caseContext = (CaseContext) runtime.query(caseId, ".");

    List<GoalStatusView> goals = new ArrayList<>();
    Set<String> reachedGoalNames = new HashSet<>();

    for (Goal goal : definition.getGoals()) {
      String conditionStr = null;
      if (goal.getCondition() instanceof JQExpressionEvaluator jq) {
        conditionStr = jq.expression();
      }

      boolean satisfied = false;
      try {
        satisfied = expressionEngineRegistry.evaluate(goal.getCondition(), caseContext);
      } catch (Exception ignored) {
      }

      if (satisfied) {
        reachedGoalNames.add(goal.getName());
      }

      goals.add(new GoalStatusView(goal.getName(), goal.getKind(), satisfied, conditionStr));
    }

    CompletionSummaryView completion =
        buildCompletionSummary(definition.getCompletion(), reachedGoalNames, caseContext);

    return new GoalEvaluationView(goals, completion);
  }

  private CompletionSummaryView buildCompletionSummary(
      CaseCompletion caseCompletion, Set<String> reachedGoalNames, CaseContext caseContext) {
    if (caseCompletion == null) {
      return null;
    }

    if (caseCompletion instanceof GoalBasedCompletion<?> goalBased) {
      int total = goalBased.getGoals().size();
      int satisfied = 0;
      for (var entry : goalBased.getGoals().entrySet()) {
        GoalExpression expr = entry.getValue();
        if (expr.isSatisfiedBy(reachedGoalNames)) {
          satisfied++;
        }
      }
      return new CompletionSummaryView(satisfied == total, satisfied, total, "goal-based");
    }

    if (caseCompletion instanceof PredicateBasedCompletion predBased) {
      boolean sat = false;
      try {
        sat = expressionEngineRegistry.evaluate(predBased.getDoneWhen(), caseContext);
      } catch (Exception ignored) {
      }
      return new CompletionSummaryView(sat, sat ? 1 : 0, 1, "predicate-based");
    }

    return null;
  }
}
