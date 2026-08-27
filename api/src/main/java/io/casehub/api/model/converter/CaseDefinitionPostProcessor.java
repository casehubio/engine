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
package io.casehub.api.model.converter;

import com.fasterxml.jackson.databind.JsonNode;
import io.casehub.api.context.JacksonPojoBridge;
import io.casehub.api.model.AgentWorkerFunction;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.WorkerFunctions;
import io.casehub.api.spi.DiscoveredWorker;
import io.casehub.api.spi.WorkerFunctionProviderRegistry;
import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentConstraint;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentDisposition;
import io.casehub.eidos.api.AgentGoal;
import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.GoalPriority;
import io.casehub.eidos.api.Visibility;
import io.casehub.engine.plan.goap.GoapAction;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerFunction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jboss.logging.Logger;

/**
 * Applies runtime concerns to a {@link CaseDefinition} that Jackson deserialization cannot handle:
 * worker function wiring, GOAP shorthand, contextType bridge setup, and agentDescriptor parsing.
 *
 * <p>Called after {@link CaseDefinitionModule} deserializes the structural fields. Requires access
 * to the original raw {@link JsonNode} for spec-level YAML fields not captured in the domain model.
 */
public final class CaseDefinitionPostProcessor {

  private static final Logger LOG = Logger.getLogger(CaseDefinitionPostProcessor.class);

  private final WorkerFunctionProviderRegistry providerRegistry;

  public CaseDefinitionPostProcessor(WorkerFunctionProviderRegistry providerRegistry) {
    this.providerRegistry = providerRegistry;
  }

  /**
   * Applies all runtime concerns to the given definition using the original raw YAML node.
   *
   * @param def the partially-built definition (workers have {@link WorkerFunction#NONE})
   * @param rawNode the root YAML node (must be the same node the mapper read from)
   */
  public void apply(CaseDefinition def, JsonNode rawNode) {
    applyContextTypeBridge(def);
    applyWorkerFunctions(def, rawNode);
    applyGoapShorthand(def, rawNode);
    applyAgentDescriptors(def, rawNode);
  }

  // ---- contextType --------------------------------------------------------

  private static void applyContextTypeBridge(CaseDefinition def) {
    String contextType = def.getContextType();
    if (contextType == null || contextType.isBlank()) {
      return;
    }
    try {
      Class<?> contextClass = Class.forName(contextType);
      def.setDefaultWorkerBridge(new JacksonPojoBridge<>(contextClass));
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(
          "CaseDefinition '" + def.getName() + "' contextType class not found: " + contextType, e);
    }
  }

  // ---- worker function wiring ---------------------------------------------

  private void applyWorkerFunctions(CaseDefinition def, JsonNode rawNode) {
    if (providerRegistry == null) {
      return;
    }
    JsonNode specNode = rawNode.has("spec") ? rawNode.get("spec") : null;
    if (specNode == null || !specNode.has("workers")) {
      return;
    }
    JsonNode rawWorkers = specNode.get("workers");
    if (rawWorkers == null || !rawWorkers.isArray()) {
      return;
    }

    // Build index of current workers by name for easy lookup
    Map<String, Worker> workerIndex = new LinkedHashMap<>();
    for (Worker w : def.getWorkers()) {
      workerIndex.put(w.name(), w);
    }

    // First pass: wire functions and handle discovery expansions
    Map<String, Worker> builtWorkers = new LinkedHashMap<>();
    List<String> sequenceWorkerNames = new ArrayList<>();

    for (int i = 0; i < rawWorkers.size(); i++) {
      JsonNode rawW = rawWorkers.get(i);
      if (rawW == null || rawW.isNull()) {
        continue;
      }

      String workerName = rawW.has("name") ? rawW.get("name").asText() : null;
      if (workerName == null) {
        continue;
      }

      // Skip sequence workers in first pass
      if (rawW.has("sequence") && rawW.get("sequence").isArray()) {
        sequenceWorkerNames.add(workerName);
        Worker existing = workerIndex.get(workerName);
        if (existing != null) {
          builtWorkers.put(workerName, existing);
        }
        continue;
      }

      // Try discovery first (multi-worker providers like MCP)
      List<DiscoveredWorker> discovered = providerRegistry.discoverWorkers(rawW);
      if (!discovered.isEmpty()) {
        for (DiscoveredWorker dw : discovered) {
          Worker discoveredWorker =
              Worker.builder()
                  .name(dw.workerName())
                  .capabilityName(dw.capability().name())
                  .function(dw.function())
                  .build();
          builtWorkers.put(dw.workerName(), discoveredWorker);
        }
        continue;
      }

      // Try single-function providers (flow, a2a)
      WorkerFunction<?, ?> function = providerRegistry.createFunction(rawW);

      // API-local construction if no provider matched
      if (function == null) {
        if (rawW.has("agent")) {
          function = buildAgentFunction(rawW, workerName);
        } else if (rawW.has("contextType")) {
          function = buildTypedSyncFunction(rawW, workerName);
        } else {
          function = WorkerFunction.NONE;
        }
      }

      // Rebuild the worker with the resolved function
      Worker existing = workerIndex.get(workerName);
      if (existing != null) {
        Worker updated =
            Worker.builder()
                .name(existing.name())
                .capabilityNames(existing.capabilities())
                .function(function)
                .executionPolicy(existing.executionPolicy())
                .description(existing.description())
                .build();
        builtWorkers.put(workerName, updated);
      }
    }

    // Second pass: resolve sequence references
    for (String seqWorkerName : sequenceWorkerNames) {
      // Find the raw node for this sequence worker
      for (int i = 0; i < rawWorkers.size(); i++) {
        JsonNode rawW = rawWorkers.get(i);
        if (rawW == null
            || !seqWorkerName.equals(rawW.has("name") ? rawW.get("name").asText() : null)) {
          continue;
        }
        JsonNode seqNode = rawW.get("sequence");
        List<WorkerFunction> stepFunctions = new ArrayList<>();
        for (JsonNode stepNode : seqNode) {
          String stepName = stepNode.asText();
          Worker stepWorker = builtWorkers.get(stepName);
          if (stepWorker == null) {
            throw new IllegalArgumentException(
                "Worker '"
                    + seqWorkerName
                    + "' sequence references unknown worker '"
                    + stepName
                    + "'");
          }
          stepFunctions.add(stepWorker.function());
        }
        WorkerFunction sequenceFunc =
            WorkerFunctions.sequence(stepFunctions.toArray(new WorkerFunction[0]));

        Worker existing = workerIndex.get(seqWorkerName);
        if (existing != null) {
          Worker updated =
              Worker.builder()
                  .name(existing.name())
                  .capabilityNames(existing.capabilities())
                  .function(sequenceFunc)
                  .executionPolicy(existing.executionPolicy())
                  .description(existing.description())
                  .build();
          builtWorkers.put(seqWorkerName, updated);
        }
        break;
      }
    }

    // Replace workers in def with the function-wired versions
    if (!builtWorkers.isEmpty()) {
      def.getWorkers().clear();
      def.getWorkers().addAll(builtWorkers.values());
    }
  }

  private static WorkerFunction<?, ?> buildAgentFunction(JsonNode rawW, String workerName) {
    JsonNode agentNode = rawW.get("agent");
    try {
      io.casehub.api.model.ai.Agent agent = AgentConverter.toApiAgent(agentNode);
      return new AgentWorkerFunction(agent);
    } catch (Exception e) {
      LOG.warnf(
          "Worker '%s': agent conversion failed, falling back to NONE — %s",
          workerName, e.getMessage());
      return WorkerFunction.NONE;
    }
  }

  private static WorkerFunction<?, ?> buildTypedSyncFunction(JsonNode rawW, String workerName) {
    String contextTypeName = rawW.get("contextType").asText();
    try {
      Class<?> contextClass = Class.forName(contextTypeName);
      Class<?> outType =
          rawW.has("outputType") ? Class.forName(rawW.get("outputType").asText()) : Map.class;
      final String capturedName = workerName;
      return new WorkerFunction.Sync<>(
          contextClass,
          outType,
          (input, scope) -> {
            throw new UnsupportedOperationException(
                "YAML-declared contextType worker '"
                    + capturedName
                    + "' has no in-process function — dispatch via external backend");
          });
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException(
          "Worker '" + workerName + "' type class not found: " + contextTypeName, e);
    }
  }

  // ---- GOAP shorthand -----------------------------------------------------

  private static void applyGoapShorthand(CaseDefinition def, JsonNode rawNode) {
    JsonNode specNode = rawNode.has("spec") ? rawNode.get("spec") : null;
    if (specNode == null || !specNode.has("workers")) {
      return;
    }
    JsonNode rawWorkers = specNode.get("workers");
    if (rawWorkers == null || !rawWorkers.isArray()) {
      return;
    }

    List<GoapAction> workerGoapActions = new ArrayList<>();

    for (int i = 0; i < rawWorkers.size(); i++) {
      JsonNode rawW = rawWorkers.get(i);
      if (rawW == null) {
        continue;
      }
      boolean hasEffect = rawW.has("effect") && rawW.get("effect").isObject();
      boolean hasCost = rawW.has("cost");
      if (!hasEffect && !hasCost) {
        continue;
      }

      // Read capName from the raw YAML node — avoids fragile index alignment with def.getWorkers()
      String capName;
      if (rawW.has("capabilities")
          && rawW.get("capabilities").isArray()
          && rawW.get("capabilities").size() > 0) {
        capName = rawW.get("capabilities").get(0).asText();
      } else {
        capName = rawW.has("name") ? rawW.get("name").asText() : null;
      }
      if (capName == null) {
        continue;
      }

      Map<String, Boolean> effects = parseBooleanMap(rawW.get("effect"));
      double cost = rawW.has("cost") ? rawW.get("cost").asDouble() : 1.0;

      Map<String, Boolean> softPrec = Map.of();
      if (rawW.has("softDependency") && rawW.get("softDependency").isArray()) {
        var sp = new LinkedHashMap<String, Boolean>();
        rawW.get("softDependency").forEach(e -> sp.put(e.asText(), true));
        softPrec = Map.copyOf(sp);
      }

      workerGoapActions.add(new GoapAction(capName, Map.of(), effects, cost, 0, softPrec));
    }

    if (!workerGoapActions.isEmpty()) {
      List<GoapAction> existing =
          def.getGoapActions() != null ? new ArrayList<>(def.getGoapActions()) : new ArrayList<>();
      existing.addAll(workerGoapActions);
      def.setGoapActions(existing);
    }
  }

  // ---- agentDescriptor ----------------------------------------------------

  private static void applyAgentDescriptors(CaseDefinition def, JsonNode rawNode) {
    JsonNode specNode = rawNode.has("spec") ? rawNode.get("spec") : null;
    if (specNode == null || !specNode.has("workers")) {
      return;
    }
    JsonNode rawWorkers = specNode.get("workers");
    if (rawWorkers == null || !rawWorkers.isArray()) {
      return;
    }

    Map<String, AgentDescriptor> descriptors = new LinkedHashMap<>();
    for (int i = 0; i < rawWorkers.size(); i++) {
      JsonNode rawW = rawWorkers.get(i);
      if (rawW == null || !rawW.has("agentDescriptor")) {
        continue;
      }
      String workerName = rawW.has("name") ? rawW.get("name").asText() : null;
      if (workerName == null) {
        continue;
      }
      descriptors.put(workerName, buildAgentDescriptor(rawW.get("agentDescriptor"), workerName));
    }

    if (!descriptors.isEmpty()) {
      def.setAgentDescriptors(descriptors);
    }
  }

  static AgentDescriptor buildAgentDescriptor(JsonNode node, String workerName) {
    var builder = AgentDescriptor.builder();
    builder.agentId(node.has("agentId") ? node.get("agentId").asText() : workerName);
    builder.name(node.has("name") ? node.get("name").asText() : workerName);
    builder.slot(node.has("slot") ? node.get("slot").asText() : workerName);
    builder.tenancyId(node.has("tenancyId") ? node.get("tenancyId").asText() : "default");

    if (node.has("briefing")) builder.briefing(node.get("briefing").asText());
    if (node.has("version")) builder.version(node.get("version").asText());
    if (node.has("provider")) builder.provider(node.get("provider").asText());
    if (node.has("modelFamily")) builder.modelFamily(node.get("modelFamily").asText());
    if (node.has("modelVersion")) builder.modelVersion(node.get("modelVersion").asText());
    if (node.has("jurisdiction")) builder.jurisdiction(node.get("jurisdiction").asText());
    if (node.has("dataHandlingPolicy"))
      builder.dataHandlingPolicy(node.get("dataHandlingPolicy").asText());

    if (node.has("goals") && node.get("goals").isArray()) {
      List<AgentGoal> goals = new ArrayList<>();
      for (JsonNode gn : node.get("goals")) {
        String priority = gn.has("priority") ? gn.get("priority").asText() : "PRIMARY";
        String visibility = gn.has("visibility") ? gn.get("visibility").asText() : "PUBLIC";
        List<String> capRefs = new ArrayList<>();
        if (gn.has("capabilities") && gn.get("capabilities").isArray()) {
          gn.get("capabilities").forEach(c -> capRefs.add(c.asText()));
        }
        Map<String, String> attrs = null;
        if (gn.has("attributes") && gn.get("attributes").isObject()) {
          attrs = new LinkedHashMap<>();
          var it = gn.get("attributes").fields();
          while (it.hasNext()) {
            var entry = it.next();
            attrs.put(entry.getKey(), entry.getValue().asText());
          }
        }
        goals.add(
            new AgentGoal(
                gn.get("name").asText(),
                gn.has("description") ? gn.get("description").asText() : gn.get("name").asText(),
                GoalPriority.valueOf(priority),
                Visibility.valueOf(visibility),
                capRefs,
                attrs));
      }
      builder.goals(goals);
    }

    if (node.has("constraints") && node.get("constraints").isArray()) {
      List<AgentConstraint> constraints = new ArrayList<>();
      for (JsonNode cn : node.get("constraints")) {
        constraints.add(
            new AgentConstraint(
                cn.get("name").asText(),
                cn.has("description") ? cn.get("description").asText() : cn.get("name").asText(),
                cn.has("visibility")
                    ? Visibility.valueOf(cn.get("visibility").asText())
                    : Visibility.PUBLIC,
                cn.has("severity")
                    ? ConstraintSeverity.valueOf(cn.get("severity").asText())
                    : ConstraintSeverity.HARD));
      }
      builder.constraints(constraints);
    }

    if (node.has("disposition") && node.get("disposition").isObject()) {
      JsonNode dn = node.get("disposition");
      var db = AgentDisposition.builder();
      if (dn.has("socialOrient")) db.socialOrient(dn.get("socialOrient").asText());
      if (dn.has("ruleFollowing")) db.ruleFollowing(dn.get("ruleFollowing").asText());
      if (dn.has("riskAppetite")) db.riskAppetite(dn.get("riskAppetite").asText());
      if (dn.has("autonomy")) db.autonomy(dn.get("autonomy").asText());
      if (dn.has("conflictMode")) db.conflictMode(dn.get("conflictMode").asText());
      if (dn.has("delegation")) db.delegation(dn.get("delegation").asBoolean());
      builder.disposition(db.build());
    }

    if (node.has("capabilities") && node.get("capabilities").isArray()) {
      List<AgentCapability> caps = new ArrayList<>();
      for (JsonNode cn : node.get("capabilities")) {
        var cb = AgentCapability.builder();
        cb.name(cn.get("name").asText());
        if (cn.has("description")) cb.description(cn.get("description").asText());
        if (cn.has("qualityHint")) cb.qualityHint(cn.get("qualityHint").asDouble());
        if (cn.has("latencyHintP50Ms")) cb.latencyHintP50Ms(cn.get("latencyHintP50Ms").asLong());
        if (cn.has("costHint")) cb.costHint(cn.get("costHint").asText());
        if (cn.has("tags") && cn.get("tags").isArray()) {
          List<String> tags = new ArrayList<>();
          cn.get("tags").forEach(t -> tags.add(t.asText()));
          cb.tags(tags);
        }
        caps.add(cb.build());
      }
      builder.capabilities(caps);
    }

    return builder.build();
  }

  // ---- helpers ------------------------------------------------------------

  private static Map<String, Boolean> parseBooleanMap(JsonNode node) {
    if (node == null || !node.isObject()) {
      return Map.of();
    }
    var map = new LinkedHashMap<String, Boolean>();
    node.fields().forEachRemaining(e -> map.put(e.getKey(), e.getValue().asBoolean()));
    return Map.copyOf(map);
  }
}
