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
package io.casehub.api.model.converter.deser;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.casehub.api.model.AdaptationConfig;
import io.casehub.api.model.Binding;
import io.casehub.api.model.CapabilityTarget;
import io.casehub.api.model.CaseCompletion;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CaseDefinitionSpec;
import io.casehub.api.model.Goal;
import io.casehub.api.model.MemoryRetrievalConfig;
import io.casehub.api.model.Milestone;
import io.casehub.api.model.RecoveryPolicy;
import io.casehub.api.model.ReflectionTriggerConfig;
import io.casehub.api.model.cbr.CbrConfig;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.casehub.api.spi.QuorumConfig;
import io.casehub.engine.plan.monitoring.MonitoringConfig;
import io.casehub.platform.api.expression.ExpressionEvaluator;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Worker;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CaseDefinitionDeserializer extends StdDeserializer<CaseDefinition> {

  public CaseDefinitionDeserializer() {
    super(CaseDefinition.class);
  }

  @Override
  public CaseDefinition deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    ObjectCodec codec = p.getCodec();
    JsonNode root = p.readValueAsTree();
    if (root == null || root.isNull()) {
      return null;
    }

    String namespace = textOrNull(root, "namespace");
    String name = textOrNull(root, "name");
    String version = textOrNull(root, "version");
    CaseDefinition def = new CaseDefinition(namespace, name, version);

    def.setDsl(textOrNull(root, "dsl"));
    def.setTitle(textOrNull(root, "title"));
    def.setSummary(textOrNull(root, "summary"));

    String expressionLang = textOrNull(root, "expressionLang");
    if (expressionLang != null) {
      def.setExpressionLang(expressionLang);
      ctxt.setAttribute(ExpressionEvaluatorDeserializer.EXPRESSION_LANG_KEY, expressionLang);
    }

    if (root.has("context") && root.get("context").has("storeFactory")) {
      def.setContextStoreFactory(root.get("context").get("storeFactory").asText());
    }
    if (root.has("contextType")) {
      def.setContextType(root.get("contextType").asText());
      if (expressionLang == null) {
        def.setExpressionLang("mvel");
        ctxt.setAttribute(ExpressionEvaluatorDeserializer.EXPRESSION_LANG_KEY, "mvel");
      }
    }

    if (root.has("semanticData") && root.get("semanticData").isObject()) {
      @SuppressWarnings("unchecked")
      Map<String, Object> sd =
          ((ObjectMapper) codec).convertValue(root.get("semanticData"), Map.class);
      def.setSemanticData(sd);
    }

    if (root.has("types") && root.get("types").isArray()) {
      java.util.Set<io.casehub.platform.api.path.Path> types = new java.util.LinkedHashSet<>();
      for (JsonNode tn : root.get("types")) {
        types.add(io.casehub.platform.api.path.Path.parse(tn.asText()));
      }
      def.setTypes(types);
    }
    if (root.has("labels") && root.get("labels").isArray()) {
      java.util.Set<io.casehub.platform.api.path.Path> labels = new java.util.LinkedHashSet<>();
      for (JsonNode ln : root.get("labels")) {
        labels.add(io.casehub.platform.api.path.Path.parse(ln.asText()));
      }
      def.setLabels(labels);
    }
    if (root.has("layers") && root.get("layers").isArray()) {
      java.util.List<String> layerNames = new java.util.ArrayList<>();
      for (JsonNode ln : root.get("layers")) {
        if (ln.isObject() && ln.has("name")) {
          layerNames.add(ln.get("name").asText());
        } else if (ln.isTextual()) {
          layerNames.add(ln.asText());
        }
      }
      def.setLayerNames(layerNames);
    }
    if (root.has("episodic") && root.get("episodic").has("memory")) {
      JsonNode memNode = root.get("episodic").get("memory");
      String domain = textOrNull(memNode, "domain");
      String entityId = textOrNull(memNode, "entityId");
      int recent = memNode.has("recent") ? memNode.get("recent").asInt() : 10;
      def.setEpisodicMemoryConfig(
          io.casehub.api.model.EpisodicMemoryConfig.of(domain, entityId, recent));
    }
    if (root.has("signals") && root.get("signals").isArray()) {
      java.util.List<io.casehub.api.model.SignalType<?>> signals = new java.util.ArrayList<>();
      for (JsonNode sn : root.get("signals")) {
        String sigName = textOrNull(sn, "name");
        String ctxType = textOrNull(sn, "contextType");
        Class<?> payloadType = java.util.Map.class;
        if (ctxType != null) {
          try {
            payloadType = Class.forName(ctxType);
          } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                "Signal '" + sigName + "' contextType class not found: " + ctxType, e);
          }
        }
        signals.add(io.casehub.api.model.SignalType.of(sigName, payloadType));
      }
      def.setSignals(signals);
    }

    JsonNode specNode = root.has("spec") ? root.get("spec") : root;

    Map<String, CapabilityTarget> capTargetMap = parseCapabilities(specNode, def, codec, ctxt);
    ctxt.setAttribute(BindingDeserializer.CAPABILITY_TARGET_MAP_KEY, capTargetMap);

    parseArrayInto(specNode, "workers", Worker.class, def.getWorkers()::add, codec, ctxt);
    parseArrayInto(specNode, "bindings", Binding.class, def.getBindings()::add, codec, ctxt);

    if (specNode.has("goals") && specNode.get("goals").isArray()) {
      for (JsonNode gn : specNode.get("goals")) {
        def.getGoals().add(deserializeGoal(gn, codec, ctxt));
      }
    }

    if (specNode.has("milestones") && specNode.get("milestones").isArray()) {
      for (JsonNode mn : specNode.get("milestones")) {
        def.getMilestones().add(deserializeMilestone(mn, codec, ctxt));
      }
    }

    if (specNode.has("completion")) {
      def.setCompletion(readValue(specNode.get("completion"), CaseCompletion.class, codec, ctxt));
    }

    if (def.getCompletion() instanceof io.casehub.api.model.GoalBasedCompletion<?> gbc) {
      for (var entry : gbc.getGoals().entrySet()) {
        String kindName = entry.getKey().value();
        java.util.Set<String> goalNames = entry.getValue().goalNames();
        for (int i = 0; i < def.getGoals().size(); i++) {
          Goal g = def.getGoals().get(i);
          if (goalNames.contains(g.getName()) && g.getKind() == null) {
            Goal updated = new Goal(g.getName(), g.getCondition(), kindName);
            if (g.getDescription() != null) updated.setDescription(g.getDescription());
            def.getGoals().set(i, updated);
          }
        }
      }
    }

    CaseDefinitionSpec spec = def.getSpec();
    setTextIfPresent(specNode, "planningStrategy", spec::setPlanningStrategy);
    setTextIfPresent(specNode, "agentRouting", spec::setAgentRouting);
    setTextIfPresent(specNode, "implementationRouting", spec::setImplementationRouting);
    setTextIfPresent(specNode, "humanTaskRouting", spec::setHumanTaskRouting);
    setTextIfPresent(specNode, "candidateMatching", spec::setCandidateMatching);
    setTextIfPresent(specNode, "decompositionStrategy", spec::setDecompositionStrategy);
    if (specNode.has("maxDecompositionDepth")) {
      spec.setMaxDecompositionDepth(specNode.get("maxDecompositionDepth").asInt());
    }
    if (specNode.has("maxAdaptations")) {
      spec.setMaxAdaptations(specNode.get("maxAdaptations").asInt());
    }

    if (specNode.has("cbr")) {
      spec.setCbrConfig(readValue(specNode.get("cbr"), CbrConfig.class, codec, ctxt));
    }
    if (specNode.has("adaptation")) {
      spec.setAdaptationConfig(
          readValue(specNode.get("adaptation"), AdaptationConfig.class, codec, ctxt));
    }
    if (specNode.has("recoveryPolicy")) {
      spec.setRecoveryPolicy(
          readValue(specNode.get("recoveryPolicy"), RecoveryPolicy.class, codec, ctxt));
    }
    if (specNode.has("monitoring")) {
      spec.setMonitoringConfig(deserializeMonitoringConfig(specNode.get("monitoring")));
    }
    if (specNode.has("reflection")) {
      spec.setReflectionTrigger(deserializeReflectionTriggerConfig(specNode.get("reflection")));
    }
    if (specNode.has("memoryRetrieval")) {
      spec.setMemoryRetrieval(
          readValue(specNode.get("memoryRetrieval"), MemoryRetrievalConfig.class, codec, ctxt));
    }
    if (specNode.has("quorum")) {
      spec.setDefaultQuorum(deserializeQuorumConfig(specNode.get("quorum")));
    }
    if (specNode.has("planningConstraints")) {
      spec.setPlanningConstraints(
          deserializePlanningConstraints(specNode.get("planningConstraints")));
    }
    JsonNode goapNode =
        specNode.has("goapActions")
            ? specNode.get("goapActions")
            : specNode.has("actions") ? specNode.get("actions") : null;
    if (goapNode != null && goapNode.isArray()) {
      java.util.List<io.casehub.engine.plan.goap.GoapAction> actions = new java.util.ArrayList<>();
      for (JsonNode an : goapNode) {
        actions.add(deserializeGoapAction(an));
      }
      spec.setGoapActions(actions);
    }
    if (specNode.has("workerServiceAccountIds")
        && specNode.get("workerServiceAccountIds").isObject()) {
      @SuppressWarnings("unchecked")
      Map<String, String> ids =
          ((ObjectMapper) codec).convertValue(specNode.get("workerServiceAccountIds"), Map.class);
      spec.setWorkerServiceAccountIds(ids);
    }
    if (specNode.has("humanTaskWorkloadConstraint")) {
      spec.setHumanTaskWorkloadConstraint(
          deserializeWorkloadConstraint(specNode.get("humanTaskWorkloadConstraint")));
    }
    if (specNode.has("humanTaskContextConstraints")
        && specNode.get("humanTaskContextConstraints").isArray()) {
      java.util.List<io.casehub.api.model.routing.ContextConstraint> constraints =
          new java.util.ArrayList<>();
      for (JsonNode cn : specNode.get("humanTaskContextConstraints")) {
        constraints.add(deserializeContextConstraint(cn, codec, ctxt));
      }
      spec.setHumanTaskContextConstraints(constraints);
    }
    if (specNode.has("routingSignalWeights") && specNode.get("routingSignalWeights").isObject()) {
      Map<String, Double> weights = new LinkedHashMap<>();
      specNode
          .get("routingSignalWeights")
          .fields()
          .forEachRemaining(e -> weights.put(e.getKey(), e.getValue().asDouble()));
      spec.setRoutingSignalWeights(weights);
    }
    if (specNode.has("authorization") && specNode.get("authorization").isObject()) {
      spec.setAuthorization(deserializeAuthorization(specNode.get("authorization")));
    }
    if (specNode.has("portfolioConfig")) {
      spec.setPortfolioConfig(
          readValue(
              specNode.get("portfolioConfig"),
              io.casehub.engine.plan.PortfolioConfig.class,
              codec,
              ctxt));
    }
    if (specNode.has("cognitiveDemands") && specNode.get("cognitiveDemands").isObject()) {
      Map<String, io.casehub.api.model.CognitiveDemand> demands = new LinkedHashMap<>();
      specNode
          .get("cognitiveDemands")
          .fields()
          .forEachRemaining(
              e -> {
                Map<String, Double> weights = new LinkedHashMap<>();
                e.getValue()
                    .fields()
                    .forEachRemaining(w -> weights.put(w.getKey(), w.getValue().asDouble()));
                demands.put(e.getKey(), new io.casehub.api.model.CognitiveDemand(weights));
              });
      spec.setCognitiveDemands(demands);
    }
    if (specNode.has("compounds") && specNode.get("compounds").isArray()) {
      java.util.List<io.casehub.api.model.CompoundDeclaration> compounds =
          new java.util.ArrayList<>();
      for (JsonNode cn : specNode.get("compounds")) {
        compounds.add(deserializeCompound(cn, codec, ctxt));
      }
      def.setCompounds(compounds);
    }
    if (specNode.has("channels") && specNode.get("channels").isArray()) {
      java.util.List<io.casehub.api.model.ChannelDeclaration> channels =
          new java.util.ArrayList<>();
      for (JsonNode cn : specNode.get("channels")) {
        String chName = textOrNull(cn, "name");
        String recordType = textOrNull(cn, "recordType");
        String transport = textOrNull(cn, "transport");
        String scopeStr = textOrNull(cn, "scope");
        io.casehub.api.model.LifecycleScope scope =
            scopeStr != null ? io.casehub.api.model.LifecycleScope.valueOf(scopeStr) : null;
        Class<?> rt = java.util.Map.class;
        if (recordType != null) {
          try {
            rt = Class.forName(recordType);
          } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                "Channel '" + chName + "' recordType not found: " + recordType, e);
          }
        }
        channels.add(new io.casehub.api.model.ChannelDeclaration(chName, rt, transport, scope));
      }
      spec.setChannels(channels);
    }

    if (root.has("use") && root.get("use").isObject()) {
      def.setUse(readValue(root.get("use"), io.casehub.api.model.Use.class, codec, ctxt));
    }
    if (root.has("labelRules") && root.get("labelRules").isArray()) {
      java.util.List<io.casehub.platform.api.label.LabelRule> rules = new java.util.ArrayList<>();
      for (JsonNode rn : root.get("labelRules")) {
        rules.add(deserializeLabelRule(rn, codec, ctxt));
      }
      def.setLabelRules(java.util.Collections.unmodifiableList(rules));
    }
    if (root.has("inboundMappings") && root.get("inboundMappings").isArray()) {
      java.util.List<io.casehub.api.model.InboundSignalMapping> mappings =
          new java.util.ArrayList<>();
      for (JsonNode mn : root.get("inboundMappings")) {
        mappings.add(deserializeInboundMapping(mn, codec, ctxt));
      }
      def.setInboundMappings(mappings);
    }

    return def;
  }

  private Map<String, CapabilityTarget> parseCapabilities(
      JsonNode specNode, CaseDefinition def, ObjectCodec codec, DeserializationContext ctxt) {
    Map<String, CapabilityTarget> capTargetMap = new LinkedHashMap<>();
    if (!specNode.has("capabilities") || !specNode.get("capabilities").isArray()) {
      return capTargetMap;
    }
    Map<String, io.casehub.api.model.CognitiveDemand> cognitiveDemands = new LinkedHashMap<>();
    for (JsonNode capNode : specNode.get("capabilities")) {
      String capName = textOrNull(capNode, "name");
      String inputProj = ".";
      if (capNode.has("inputProjection")) {
        inputProj = capNode.get("inputProjection").asText();
      } else if (capNode.has("inputSchema")) {
        inputProj = capNode.get("inputSchema").asText();
      }
      String outputProj = ".";
      if (capNode.has("outputProjection")) {
        outputProj = capNode.get("outputProjection").asText();
      } else if (capNode.has("outputSchema")) {
        outputProj = capNode.get("outputSchema").asText();
      }
      String desc = textOrNull(capNode, "description");

      Capability cap =
          Capability.builder()
              .name(capName)
              .inputSchema(inputProj)
              .outputSchema(outputProj)
              .description(desc)
              .build();
      def.getCapabilities().add(cap);
      capTargetMap.put(
          capName,
          new CapabilityTarget(
              cap, new JQExpressionEvaluator(inputProj), new JQExpressionEvaluator(outputProj)));
      if (capNode.has("cognitiveDemand") && capNode.get("cognitiveDemand").isObject()) {
        Map<String, Double> weights = new LinkedHashMap<>();
        capNode
            .get("cognitiveDemand")
            .fields()
            .forEachRemaining(e -> weights.put(e.getKey(), e.getValue().asDouble()));
        cognitiveDemands.put(capName, new io.casehub.api.model.CognitiveDemand(weights));
      }
    }
    if (!cognitiveDemands.isEmpty()) {
      def.getSpec().setCognitiveDemands(cognitiveDemands);
    }
    return capTargetMap;
  }

  private Goal deserializeGoal(JsonNode node, ObjectCodec codec, DeserializationContext ctxt)
      throws IOException {
    String goalName = textOrNull(node, "name");
    ExpressionEvaluator condition = null;
    if (node.has("condition")) {
      condition = readValue(node.get("condition"), ExpressionEvaluator.class, codec, ctxt);
    }
    String kind = textOrNull(node, "kind");
    Goal goal = new Goal(goalName, condition, kind);
    if (node.has("description")) {
      goal.setDescription(node.get("description").asText());
    }
    return goal;
  }

  private Milestone deserializeMilestone(
      JsonNode node, ObjectCodec codec, DeserializationContext ctxt) throws IOException {
    Milestone.Builder b = Milestone.builder();
    if (node.has("name")) b.name(node.get("name").asText());
    if (node.has("entryCriteria")) {
      b.entryCriteria(readValue(node.get("entryCriteria"), ExpressionEvaluator.class, codec, ctxt));
    }
    if (node.has("completionCriteria")) {
      b.completionCriteria(
          readValue(node.get("completionCriteria"), ExpressionEvaluator.class, codec, ctxt));
    }
    if (node.has("condition")) {
      b.completionCriteria(
          readValue(node.get("condition"), ExpressionEvaluator.class, codec, ctxt));
    }
    if (node.has("slaDuration")) {
      String raw = node.get("slaDuration").asText();
      String msName = textOrNull(node, "name");
      try {
        java.time.Duration d = java.time.Duration.parse(raw);
        if (d.isZero() || d.isNegative()) {
          throw new IllegalArgumentException(
              "Milestone '" + msName + "' slaDuration must be positive, got: " + raw);
        }
        b.slaDuration(d);
      } catch (java.time.format.DateTimeParseException e) {
        throw new IllegalArgumentException(
            "Milestone '" + msName + "' has invalid slaDuration: " + raw, e);
      }
    }
    if (node.has("slaStartFrom")) {
      b.slaStartFrom(io.casehub.api.model.SlaStartFrom.valueOf(node.get("slaStartFrom").asText()));
    }
    if (node.has("description")) {
      b.description(node.get("description").asText());
    }
    return b.build();
  }

  private <T> void parseArrayInto(
      JsonNode parent,
      String field,
      Class<T> type,
      Consumer<T> adder,
      ObjectCodec codec,
      DeserializationContext ctxt)
      throws IOException {
    if (!parent.has(field) || !parent.get(field).isArray()) {
      return;
    }
    for (JsonNode elem : parent.get(field)) {
      adder.accept(readValue(elem, type, codec, ctxt));
    }
  }

  private <T> T readValue(
      JsonNode node, Class<T> type, ObjectCodec codec, DeserializationContext ctxt)
      throws IOException {
    JsonParser nested = node.traverse(codec);
    nested.nextToken();
    return ctxt.readValue(nested, type);
  }

  private static String textOrNull(JsonNode node, String field) {
    return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
  }

  private static void setTextIfPresent(JsonNode node, String field, Consumer<String> setter) {
    if (node.has(field) && !node.get(field).isNull()) {
      setter.accept(node.get(field).asText());
    }
  }

  private io.casehub.engine.plan.goap.GoapAction deserializeGoapAction(JsonNode node) {
    String actionName = textOrNull(node, "name");
    Map<String, Boolean> preconditions = parseBooleanMap(node.get("preconditions"));
    Map<String, Boolean> effects = parseBooleanMap(node.get("effects"));
    double cost = node.has("cost") ? node.get("cost").asDouble() : 1.0;
    double benefit = node.has("benefit") ? node.get("benefit").asDouble() : 0.0;
    Map<String, Boolean> softPreconditions = parseBooleanMap(node.get("softPreconditions"));
    return new io.casehub.engine.plan.goap.GoapAction(
        actionName, preconditions, effects, cost, benefit, softPreconditions);
  }

  private static Map<String, Boolean> parseBooleanMap(JsonNode node) {
    if (node == null || !node.isObject()) {
      return Map.of();
    }
    var map = new LinkedHashMap<String, Boolean>();
    node.fields().forEachRemaining(e -> map.put(e.getKey(), e.getValue().asBoolean()));
    return Map.copyOf(map);
  }

  private io.casehub.engine.plan.PlanningConstraints deserializePlanningConstraints(JsonNode node) {
    java.time.Duration timeBudget =
        node.has("timeBudget") ? java.time.Duration.parse(node.get("timeBudget").asText()) : null;
    Integer resourceLimit = node.has("resourceLimit") ? node.get("resourceLimit").asInt() : null;
    Map<String, Double> weights = new LinkedHashMap<>();
    if (node.has("weights") && node.get("weights").isObject()) {
      node.get("weights")
          .fields()
          .forEachRemaining(e -> weights.put(e.getKey(), e.getValue().asDouble()));
    }
    Map<String, Integer> costBudgets = new LinkedHashMap<>();
    if (node.has("costBudgets") && node.get("costBudgets").isObject()) {
      node.get("costBudgets")
          .fields()
          .forEachRemaining(e -> costBudgets.put(e.getKey(), e.getValue().asInt()));
    }
    return new io.casehub.engine.plan.PlanningConstraints(
        timeBudget, resourceLimit, weights, costBudgets);
  }

  private io.casehub.api.model.routing.WorkloadConstraint deserializeWorkloadConstraint(
      JsonNode node) {
    var b = io.casehub.api.model.routing.WorkloadConstraint.builder();
    if (node.has("maxActiveTaskCount")) {
      b.maxActiveTaskCount(node.get("maxActiveTaskCount").asInt());
    }
    if (node.has("loadBalanceWeight")) {
      b.loadBalanceWeight(node.get("loadBalanceWeight").asDouble());
    }
    return b.build();
  }

  private io.casehub.api.model.routing.ContextConstraint deserializeContextConstraint(
      JsonNode node, ObjectCodec codec, DeserializationContext ctxt) throws IOException {
    var b = io.casehub.api.model.routing.ContextConstraint.builder();
    if (node.has("when")) {
      b.when(readValue(node.get("when"), ExpressionEvaluator.class, codec, ctxt));
    }
    if (node.has("weight")) {
      b.weight(node.get("weight").asDouble());
    }
    if (node.has("effect") && node.get("effect").isObject()) {
      JsonNode effect = node.get("effect");
      if (effect.has("preferGroups") || effect.has("preferUsers")) {
        java.util.Set<String> groups = new java.util.LinkedHashSet<>();
        java.util.Set<String> users = new java.util.LinkedHashSet<>();
        if (effect.has("preferGroups")) {
          effect.get("preferGroups").forEach(n -> groups.add(n.asText()));
        }
        if (effect.has("preferUsers")) {
          effect.get("preferUsers").forEach(n -> users.add(n.asText()));
        }
        b.prefer(groups, users);
      } else if (effect.has("excludeGroups") || effect.has("excludeUsers")) {
        java.util.Set<String> groups = new java.util.LinkedHashSet<>();
        java.util.Set<String> users = new java.util.LinkedHashSet<>();
        if (effect.has("excludeGroups")) {
          effect.get("excludeGroups").forEach(n -> groups.add(n.asText()));
        }
        if (effect.has("excludeUsers")) {
          effect.get("excludeUsers").forEach(n -> users.add(n.asText()));
        }
        b.exclude(groups, users);
      }
    }
    return b.build();
  }

  private QuorumConfig deserializeQuorumConfig(JsonNode node) {
    int instances = node.has("instances") ? node.get("instances").asInt() : 3;
    int required = node.has("required") ? node.get("required").asInt() : 2;
    io.casehub.api.model.OnThresholdReached otr = null;
    if (node.has("onThresholdReached")) {
      String otrStr = node.get("onThresholdReached").asText();
      try {
        otr = io.casehub.api.model.OnThresholdReached.valueOf(otrStr);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Invalid onThresholdReached value: "
                + otrStr
                + ". Valid values: "
                + java.util.Arrays.toString(io.casehub.api.model.OnThresholdReached.values()),
            e);
      }
    }
    boolean allowSameAssignee =
        node.has("allowSameAssignee") && node.get("allowSameAssignee").asBoolean();
    return new QuorumConfig(instances, required, otr, allowSameAssignee);
  }

  private MonitoringConfig deserializeMonitoringConfig(JsonNode node) {
    boolean enabled = node.has("enabled") ? node.get("enabled").asBoolean() : true;
    double threshold =
        node.has("perCompletionThreshold")
            ? node.get("perCompletionThreshold").asDouble()
            : MonitoringConfig.DEFAULT_THRESHOLD;
    int windowSize =
        node.has("windowSize")
            ? node.get("windowSize").asInt()
            : MonitoringConfig.DEFAULT_WINDOW_SIZE;
    return new MonitoringConfig(enabled, threshold, windowSize);
  }

  private ReflectionTriggerConfig deserializeReflectionTriggerConfig(JsonNode node) {
    boolean enabled = node.has("enabled") ? node.get("enabled").asBoolean() : false;
    double threshold =
        node.has("importanceThreshold") ? node.get("importanceThreshold").asDouble() : 3.0;
    int maxUnreflected =
        node.has("maxUnreflectedOutcomes") ? node.get("maxUnreflectedOutcomes").asInt() : 10;
    int maxSource = node.has("maxSourceMemories") ? node.get("maxSourceMemories").asInt() : 50;
    Map<String, Double> weights = ReflectionTriggerConfig.DEFAULT_IMPORTANCE_WEIGHTS;
    if (node.has("importanceWeights") && node.get("importanceWeights").isObject()) {
      weights = new LinkedHashMap<>();
      Map<String, Double> finalWeights = weights;
      node.get("importanceWeights")
          .fields()
          .forEachRemaining(e -> finalWeights.put(e.getKey(), e.getValue().asDouble()));
    }
    return new ReflectionTriggerConfig(enabled, threshold, maxUnreflected, maxSource, weights);
  }

  private io.casehub.api.model.CompoundDeclaration deserializeCompound(
      JsonNode node, ObjectCodec codec, DeserializationContext ctxt) throws IOException {
    String compName = textOrNull(node, "name");
    String completionSemantics = textOrNull(node, "completionSemantics");
    String dispatchMode = textOrNull(node, "dispatchMode");
    boolean repeatable = node.has("repeatable") && node.get("repeatable").asBoolean();
    String planningStrategy = textOrNull(node, "planningStrategy");
    Map<String, io.casehub.api.model.Participation> scopedBindings = new LinkedHashMap<>();
    if (node.has("scopedBindings") && node.get("scopedBindings").isObject()) {
      node.get("scopedBindings")
          .fields()
          .forEachRemaining(
              e ->
                  scopedBindings.put(
                      e.getKey(),
                      io.casehub.api.model.Participation.valueOf(e.getValue().asText())));
    }
    io.casehub.platform.api.expression.ExpressionEvaluator entryCond = null;
    if (node.has("entryCondition")) {
      entryCond =
          readValue(
              node.get("entryCondition"),
              io.casehub.platform.api.expression.ExpressionEvaluator.class,
              codec,
              ctxt);
    }
    io.casehub.platform.api.expression.ExpressionEvaluator exitCond = null;
    if (node.has("exitCondition")) {
      exitCond =
          readValue(
              node.get("exitCondition"),
              io.casehub.platform.api.expression.ExpressionEvaluator.class,
              codec,
              ctxt);
    }
    return new io.casehub.api.model.CompoundDeclaration(
        compName,
        completionSemantics,
        dispatchMode,
        scopedBindings,
        entryCond,
        exitCond,
        repeatable,
        planningStrategy);
  }

  private io.casehub.platform.api.label.LabelRule deserializeLabelRule(
      JsonNode node, ObjectCodec codec, DeserializationContext ctxt) throws IOException {
    String ruleName = textOrNull(node, "name");
    io.casehub.platform.api.expression.CompiledExpression<java.util.Map<String, Object>, Boolean>
        condition = null;
    if (node.has("when")) {
      ExpressionEvaluator evaluator =
          readValue(node.get("when"), ExpressionEvaluator.class, codec, ctxt);
      condition = new JqLabelRuleCondition(evaluator);
    }
    java.util.List<io.casehub.platform.api.label.LabelAction> actions = new java.util.ArrayList<>();
    if (node.has("actions") && node.get("actions").isArray()) {
      for (JsonNode an : node.get("actions")) {
        if (an.has("add")) {
          actions.add(new io.casehub.platform.api.label.LabelAction.Add(an.get("add").asText()));
        } else if (an.has("remove")) {
          actions.add(
              new io.casehub.platform.api.label.LabelAction.Remove(an.get("remove").asText()));
        }
      }
    }
    return new io.casehub.platform.api.label.LabelRule(ruleName, condition, actions);
  }

  private io.casehub.api.model.InboundSignalMapping deserializeInboundMapping(
      JsonNode node, ObjectCodec codec, DeserializationContext ctxt) throws IOException {
    var b = io.casehub.api.model.InboundSignalMapping.builder();
    if (node.has("signal")) b.signalName(node.get("signal").asText());
    if (node.has("connectorType")) b.connectorType(node.get("connectorType").asText());
    if (node.has("correlation")) {
      b.correlation(
          readValue(
              node.get("correlation"),
              io.casehub.platform.api.expression.ExpressionEvaluator.class,
              codec,
              ctxt));
    }
    if (node.has("payload")) {
      b.payload(
          readValue(
              node.get("payload"),
              io.casehub.platform.api.expression.ExpressionEvaluator.class,
              codec,
              ctxt));
    }
    if (node.has("correlationResolver")) {
      b.correlationResolver(node.get("correlationResolver").asText());
    }
    return b.build();
  }

  private Map<io.casehub.platform.api.acl.AclAction, java.util.List<String>>
      deserializeAuthorization(JsonNode node) {
    Map<io.casehub.platform.api.acl.AclAction, java.util.List<String>> auth = new LinkedHashMap<>();
    node.fields()
        .forEachRemaining(
            e -> {
              io.casehub.platform.api.acl.AclAction action =
                  io.casehub.platform.api.acl.AclAction.valueOf(e.getKey().toUpperCase());
              java.util.List<String> groups = new java.util.ArrayList<>();
              e.getValue().forEach(n -> groups.add(n.asText()));
              auth.put(action, groups);
            });
    return auth;
  }

  private static final ObjectMapper LABEL_RULE_MAPPER = new ObjectMapper();

  private static final class JqLabelRuleCondition
      implements io.casehub.platform.api.expression.CompiledExpression<
          java.util.Map<String, Object>, Boolean> {
    private final ExpressionEvaluator evaluator;
    private final net.thisptr.jackson.jq.JsonQuery compiledQuery;

    JqLabelRuleCondition(ExpressionEvaluator evaluator) {
      this.evaluator = evaluator;
      if (evaluator instanceof JQExpressionEvaluator jq) {
        try {
          this.compiledQuery =
              net.thisptr.jackson.jq.JsonQuery.compile(
                  jq.expression(), net.thisptr.jackson.jq.Versions.JQ_1_6);
        } catch (net.thisptr.jackson.jq.exception.JsonQueryException e) {
          throw new IllegalArgumentException("Invalid JQ in label rule: " + jq.expression(), e);
        }
      } else {
        this.compiledQuery = null;
      }
    }

    @Override
    public String type() {
      return evaluator.type();
    }

    @Override
    public Boolean eval(java.util.Map<String, Object> context) {
      if (compiledQuery != null) {
        try {
          JsonNode input = LABEL_RULE_MAPPER.valueToTree(context);
          net.thisptr.jackson.jq.Scope scope = net.thisptr.jackson.jq.Scope.newEmptyScope();
          java.util.List<JsonNode> results = new java.util.ArrayList<>();
          compiledQuery.apply(scope, input, results::add);
          if (results.isEmpty()) return false;
          JsonNode result = results.get(0);
          return result.isBoolean()
              ? result.asBoolean()
              : !result.isNull() && !result.isMissingNode();
        } catch (Exception e) {
          return false;
        }
      }
      throw new UnsupportedOperationException(
          "LabelRule evaluation for '"
              + evaluator.type()
              + "' requires runtime ExpressionEngineRegistry");
    }
  }

  @Override
  public CaseDefinition getNullValue(DeserializationContext ctxt) {
    return null;
  }
}
