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
import io.casehub.api.model.Binding;
import io.casehub.api.model.CapabilityTarget;
import io.casehub.api.model.ExecutionMode;
import io.casehub.api.model.HumanRoutingConfig;
import io.casehub.api.model.HumanTaskTarget;
import io.casehub.api.model.JudgmentTarget;
import io.casehub.api.model.LifecycleScope;
import io.casehub.api.model.OnThresholdReached;
import io.casehub.api.model.OutcomeAction;
import io.casehub.api.model.OutcomePolicy;
import io.casehub.api.model.OutcomeType;
import io.casehub.api.model.Participation;
import io.casehub.api.model.RecoveryLevel;
import io.casehub.api.model.RecoveryOverride;
import io.casehub.api.model.ReplanHint;
import io.casehub.api.model.SideEffectClassification;
import io.casehub.api.model.SubCase;
import io.casehub.api.model.SubCaseMapping;
import io.casehub.api.model.Trigger;
import io.casehub.platform.api.expression.ExpressionEvaluator;
import io.casehub.worker.api.Capability;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BindingDeserializer extends StdDeserializer<Binding> {

  static final String CAPABILITY_TARGET_MAP_KEY = "casehub.capabilityTargetMap";

  public BindingDeserializer() {
    super(Binding.class);
  }

  @Override
  public Binding deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    ObjectCodec codec = p.getCodec();
    JsonNode node = p.readValueAsTree();
    if (node == null || node.isNull()) {
      return null;
    }

    String name = textOrNull(node, "name");

    Trigger trigger = null;
    if (node.has("on")) {
      trigger = readValue(node.get("on"), Trigger.class, codec, ctxt);
    }

    Binding.Builder builder = Binding.builder().name(name).on(trigger);

    resolveTarget(node, builder, codec, ctxt);

    if (node.has("when")) {
      builder.when(readValue(node.get("when"), ExpressionEvaluator.class, codec, ctxt));
    }
    if (node.has("inputProjectionOverride")) {
      builder.inputProjectionOverride(
          readValue(node.get("inputProjectionOverride"), ExpressionEvaluator.class, codec, ctxt));
    }

    if (node.has("conflictResolverStrategy")) {
      builder.conflictResolverStrategy(node.get("conflictResolverStrategy").asText());
    }
    if (node.has("lifecycleScope")) {
      builder.lifecycleScope(LifecycleScope.valueOf(node.get("lifecycleScope").asText()));
    }
    if (node.has("participation")) {
      builder.participation(Participation.valueOf(node.get("participation").asText()));
    }
    if (node.has("executionMode")) {
      builder.executionMode(ExecutionMode.valueOf(node.get("executionMode").asText()));
    }
    if (node.has("replanHint")) {
      builder.replanHint(ReplanHint.valueOf(node.get("replanHint").asText().toUpperCase()));
    } else if (node.has("replanAfter")) {
      builder.replanHint(ReplanHint.valueOf(node.get("replanAfter").asText().toUpperCase()));
    }

    if (node.has("outcomePolicy")) {
      builder.outcomePolicy(deserializeOutcomePolicy(node.get("outcomePolicy")));
    }

    if (node.has("contextWrite")) {
      @SuppressWarnings("unchecked")
      Map<String, Object> cw =
          ((ObjectMapper) codec).convertValue(node.get("contextWrite"), Map.class);
      if (cw != null && !cw.isEmpty()) {
        builder.contextWrite(cw);
      }
    }

    if (node.has("producedKeys")) {
      Set<String> keys = new LinkedHashSet<>();
      node.get("producedKeys").forEach(n -> keys.add(n.asText()));
      builder.producedKeys(keys);
    }

    if (node.has("contingency")) {
      List<String> cont = new ArrayList<>();
      node.get("contingency").forEach(n -> cont.add(n.asText()));
      builder.contingency(cont);
    }

    if (node.has("exchangeProjectionStrategy")) {
      builder.exchangeProjectionStrategy(node.get("exchangeProjectionStrategy").asText());
    }
    String epExpression = null;
    if (node.has("exchangeProjection")) {
      JsonNode ep = node.get("exchangeProjection");
      if (ep.isTextual()) {
        builder.exchangeProjectionStrategy(ep.asText());
      } else if (ep.isObject()) {
        if (ep.has("strategy")) builder.exchangeProjectionStrategy(ep.get("strategy").asText());
        if (ep.has("expression")) epExpression = ep.get("expression").asText();
      }
    }
    if (node.has("produces")) {
      builder.produces(node.get("produces").asText());
    }
    if (node.has("consumes")) {
      builder.consumes(node.get("consumes").asText());
    }

    if (node.has("sideEffectClassification")) {
      builder.sideEffectClassification(
          SideEffectClassification.valueOf(node.get("sideEffectClassification").asText()));
    }

    if (node.has("recoveryOverride")) {
      builder.recoveryOverride(deserializeRecoveryOverride(node.get("recoveryOverride")));
    }

    if (node.has("permissionIntent") && node.get("permissionIntent").isArray()) {
      List<io.casehub.platform.api.acl.WorkerAction> actions = new ArrayList<>();
      for (JsonNode an : node.get("permissionIntent")) {
        actions.add(io.casehub.api.acl.EngineWorkerActions.fromKebabCase(an.asText()));
      }
      builder.permissionIntent(actions);
    }

    Binding result = builder.build();
    if (epExpression != null) {
      result.setExchangeProjectionExpression(epExpression);
    }
    return result;
  }

  private void resolveTarget(
      JsonNode node, Binding.Builder builder, ObjectCodec codec, DeserializationContext ctxt)
      throws IOException {
    String bindingName = textOrNull(node, "name");
    if (node.has("capability")) {
      String capName = node.get("capability").asText();
      @SuppressWarnings("unchecked")
      Map<String, CapabilityTarget> capTargetMap =
          (Map<String, CapabilityTarget>) ctxt.getAttribute(CAPABILITY_TARGET_MAP_KEY);
      if (capTargetMap != null && capTargetMap.containsKey(capName)) {
        builder.target(capTargetMap.get(capName));
      } else {
        builder.capability(Capability.of(capName, ".", "."));
      }
    } else if (node.has("subCase")) {
      builder.subCase(deserializeSubCase(node.get("subCase"), codec, ctxt));
    } else if (node.has("humanTask")) {
      builder.judgment(deserializeHumanJudgment(node.get("humanTask"), bindingName, codec, ctxt));
    } else if (node.has("judgment")) {
      builder.judgment(deserializeJudgment(node.get("judgment"), bindingName));
    } else if (node.has("signal")) {
      @SuppressWarnings("unchecked")
      Map<String, Object> payload =
          ((ObjectMapper) codec).convertValue(node.get("signal"), Map.class);
      builder.signal(payload);
    }
  }

  private SubCase deserializeSubCase(JsonNode node, ObjectCodec codec, DeserializationContext ctxt)
      throws IOException {
    SubCase.Builder b = SubCase.builder();
    if (node.has("namespace")) b.namespace(node.get("namespace").asText());
    if (node.has("name")) b.name(node.get("name").asText());
    if (node.has("version")) b.version(node.get("version").asText());
    if (node.has("waitForCompletion"))
      b.waitForCompletion(node.get("waitForCompletion").asBoolean());
    if (node.has("maxRecursionDepth")) b.maxRecursionDepth(node.get("maxRecursionDepth").asInt());
    if (node.has("inputMapping")) {
      b.inputMapping(readValue(node.get("inputMapping"), SubCaseMapping.class, codec, ctxt));
    }
    if (node.has("outputMapping")) {
      b.outputMapping(readValue(node.get("outputMapping"), SubCaseMapping.class, codec, ctxt));
    }
    if (node.has("groupId")) b.groupId(node.get("groupId").asText());
    if (node.has("totalInGroup")) b.totalInGroup(node.get("totalInGroup").asInt());
    if (node.has("requiredCount")) b.requiredCount(node.get("requiredCount").asInt());
    if (node.has("onThresholdReached")) {
      b.onThresholdReached(OnThresholdReached.valueOf(node.get("onThresholdReached").asText()));
    }
    return b.build();
  }

  private HumanTaskTarget deserializeHumanTask(
      JsonNode node, String bindingName, ObjectCodec codec, DeserializationContext ctxt)
      throws IOException {
    boolean hasTemplate = node.has("templateRef");
    boolean hasTitle = node.has("title");
    if (hasTemplate && hasTitle) {
      throw new IllegalArgumentException(
          "Binding '"
              + bindingName
              + "' cannot specify both 'title' and 'templateRef' on humanTask");
    }

    HumanTaskTarget.Builder b;
    if (hasTemplate) {
      b = HumanTaskTarget.template(node.get("templateRef").asText());
    } else {
      b = HumanTaskTarget.inline();
    }
    if (hasTitle) {
      b.title(node.get("title").asText());
    }
    if (node.has("titleExpression")) {
      b.titleExpression(
          readExpressionWithContext(
              node.get("titleExpression"), "titleExpression", bindingName, codec, ctxt));
    }
    if (node.has("candidateGroups")) {
      JsonNode cg = node.get("candidateGroups");
      if (cg.isArray()) {
        if (cg.size() > 0) {
          Set<String> groups = new LinkedHashSet<>();
          for (JsonNode n : cg) {
            if (!n.isTextual()) {
              throw new IllegalArgumentException(
                  "candidateGroups elements must be strings, got: " + n.getNodeType());
            }
            groups.add(n.asText());
          }
          b.candidateGroups(groups);
        }
      } else if (cg.isTextual()) {
        b.candidateGroupsExpression(cg.asText());
      }
    }
    if (node.has("candidateUsers")) {
      JsonNode cu = node.get("candidateUsers");
      if (cu.isArray()) {
        if (cu.size() > 0) {
          Set<String> users = new LinkedHashSet<>();
          cu.forEach(n -> users.add(n.asText()));
          b.candidateUsers(users);
        }
      } else if (cu.isTextual()) {
        b.candidateUsersExpression(cu.asText());
      }
    }
    if (node.has("expiresIn")) {
      String raw = node.get("expiresIn").asText();
      try {
        java.time.Duration d = java.time.Duration.parse(raw);
        if (d.isZero() || d.isNegative()) {
          throw new IllegalArgumentException(
              "Binding '" + bindingName + "' humanTask expiresIn must be positive, got: " + raw);
        }
        b.expiresIn(d);
      } catch (java.time.format.DateTimeParseException e) {
        throw new IllegalArgumentException(
            "Binding '" + bindingName + "' humanTask has invalid expiresIn: " + raw, e);
      }
    }
    if (node.has("expiresInExpression")) {
      String eiExpr = node.get("expiresInExpression").asText();
      if (eiExpr != null && !eiExpr.isBlank()) {
        b.expiresInExpression(
            readExpressionWithContext(
                node.get("expiresInExpression"), "expiresInExpression", bindingName, codec, ctxt));
      }
    }
    if (node.has("claimDeadlineHours")) {
      b.claimDeadlineHours(node.get("claimDeadlineHours").asInt());
    }
    if (node.has("expiresAtExpression")) {
      b.expiresAtExpression(
          readExpressionWithContext(
              node.get("expiresAtExpression"), "expiresAtExpression", bindingName, codec, ctxt));
    }
    if (node.has("priority")) {
      b.priority(node.get("priority").asText());
    }
    if (node.has("inputMapping")) {
      b.inputMapping(readValue(node.get("inputMapping"), ExpressionEvaluator.class, codec, ctxt));
    }
    if (node.has("outputMapping")) {
      b.outputMapping(readValue(node.get("outputMapping"), ExpressionEvaluator.class, codec, ctxt));
    }
    if (node.has("scope")) {
      b.scope(node.get("scope").asText());
    }
    if (node.has("scopeExpression")) {
      b.scopeExpression(
          readValue(node.get("scopeExpression"), ExpressionEvaluator.class, codec, ctxt));
    }
    if (node.has("outcomes")) {
      Set<String> outcomes = new LinkedHashSet<>();
      node.get("outcomes").forEach(n -> outcomes.add(n.asText()));
      b.outcomes(outcomes);
    }
    if (node.has("payloadType")) {
      try {
        b.payloadType(Class.forName(node.get("payloadType").asText()));
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(
            "humanTask payloadType class not found: " + node.get("payloadType").asText(), e);
      }
    }
    if (node.has("resolutionType")) {
      try {
        b.resolutionType(Class.forName(node.get("resolutionType").asText()));
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(
            "humanTask resolutionType class not found: " + node.get("resolutionType").asText(), e);
      }
    }
    try {
      return b.build();
    } catch (IllegalStateException e) {
      throw new IllegalArgumentException("Binding '" + bindingName + "' " + e.getMessage(), e);
    }
  }

  private JudgmentTarget deserializeHumanJudgment(
      JsonNode node, String bindingName, ObjectCodec codec, DeserializationContext ctxt)
      throws IOException {
    JudgmentTarget.Builder jb = JudgmentTarget.builder();

    if (node.has("title")) {
      jb.title(node.get("title").asText());
    }
    if (node.has("titleExpression")) {
      jb.titleExpression(
          readExpressionWithContext(
              node.get("titleExpression"), "titleExpression", bindingName, codec, ctxt));
    }
    if (!node.has("title") && !node.has("titleExpression") && !node.has("templateRef")) {
      jb.prompt(bindingName);
    } else if (node.has("title")) {
      jb.prompt(node.get("title").asText());
    } else {
      jb.prompt(bindingName);
    }
    if (node.has("expiresIn")) {
      String raw = node.get("expiresIn").asText();
      jb.expiresIn(java.time.Duration.parse(raw));
    }
    if (node.has("expiresInExpression")) {
      jb.expiresInExpression(
          readExpressionWithContext(
              node.get("expiresInExpression"), "expiresInExpression", bindingName, codec, ctxt));
    }
    if (node.has("expiresAtExpression")) {
      jb.expiresAtExpression(
          readExpressionWithContext(
              node.get("expiresAtExpression"), "expiresAtExpression", bindingName, codec, ctxt));
    }
    if (node.has("priority")) {
      jb.priority(node.get("priority").asText());
    }
    if (node.has("inputMapping")) {
      jb.inputMapping(readValue(node.get("inputMapping"), ExpressionEvaluator.class, codec, ctxt));
    }
    if (node.has("outputMapping")) {
      jb.outputMapping(
          readValue(node.get("outputMapping"), ExpressionEvaluator.class, codec, ctxt));
    }
    if (node.has("scope")) {
      jb.scope(node.get("scope").asText());
    }
    if (node.has("scopeExpression")) {
      jb.scopeExpression(
          readValue(node.get("scopeExpression"), ExpressionEvaluator.class, codec, ctxt));
    }
    if (node.has("outcomes")) {
      Set<String> outcomes = new LinkedHashSet<>();
      node.get("outcomes").forEach(n -> outcomes.add(n.asText()));
      jb.outcomes(outcomes);
    }
    if (node.has("resolutionType")) {
      try {
        jb.resolutionType(Class.forName(node.get("resolutionType").asText()));
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(
            "humanTask resolutionType class not found: " + node.get("resolutionType").asText(), e);
      }
    }

    io.casehub.api.spi.routing.CandidateSetSpec candidateGroups = null;
    io.casehub.api.spi.routing.CandidateSetSpec candidateUsers = null;
    if (node.has("candidateGroups")) {
      JsonNode cg = node.get("candidateGroups");
      if (cg.isArray()) {
        Set<String> groups = new LinkedHashSet<>();
        cg.forEach(n -> groups.add(n.asText()));
        candidateGroups =
            new io.casehub.api.spi.routing.CandidateSetSpec.Inline(
                io.casehub.api.spi.routing.StaticSetStrategy.of(groups));
      } else if (cg.isTextual()) {
        candidateGroups =
            new io.casehub.api.spi.routing.CandidateSetSpec.Inline(
                new io.casehub.api.spi.routing.JqCandidateSetStrategy(cg.asText()));
      }
    }
    if (node.has("candidateUsers")) {
      JsonNode cu = node.get("candidateUsers");
      if (cu.isArray()) {
        Set<String> users = new LinkedHashSet<>();
        cu.forEach(n -> users.add(n.asText()));
        candidateUsers =
            new io.casehub.api.spi.routing.CandidateSetSpec.Inline(
                io.casehub.api.spi.routing.StaticSetStrategy.of(users));
      } else if (cu.isTextual()) {
        candidateUsers =
            new io.casehub.api.spi.routing.CandidateSetSpec.Inline(
                new io.casehub.api.spi.routing.JqCandidateSetStrategy(cu.asText()));
      }
    }
    String templateRef = node.has("templateRef") ? node.get("templateRef").asText() : null;
    Integer claimDeadlineHours =
        node.has("claimDeadlineHours") ? node.get("claimDeadlineHours").asInt() : null;
    Class<?> payloadType = null;
    if (node.has("payloadType")) {
      try {
        payloadType = Class.forName(node.get("payloadType").asText());
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(
            "humanTask payloadType class not found: " + node.get("payloadType").asText(), e);
      }
    }

    jb.human(
        new HumanRoutingConfig(
            templateRef, candidateGroups, candidateUsers, claimDeadlineHours, payloadType));
    return jb.build();
  }

  private JudgmentTarget deserializeJudgment(JsonNode node, String bindingName) {
    JudgmentTarget.Builder b = JudgmentTarget.builder();
    if (node.has("prompt")) {
      b.prompt(node.get("prompt").asText());
    }
    if (node.has("promptExpression")) {
      b.promptExpression(node.get("promptExpression").asText());
    }
    if (node.has("inputMapping")) {
      b.inputMapping(node.get("inputMapping").asText());
    }
    if (node.has("outputMapping")) {
      b.outputMapping(node.get("outputMapping").asText());
    }
    if (node.has("resolutionType")) {
      try {
        b.resolutionType(Class.forName(node.get("resolutionType").asText()));
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(
            "Binding '"
                + bindingName
                + "' judgment has unknown resolutionType: "
                + node.get("resolutionType").asText(),
            e);
      }
    }
    if (node.has("expiresIn")) {
      String raw = node.get("expiresIn").asText();
      try {
        java.time.Duration d = java.time.Duration.parse(raw);
        if (d.isZero() || d.isNegative()) {
          throw new IllegalArgumentException(
              "Binding '" + bindingName + "' judgment expiresIn must be positive, got: " + raw);
        }
        b.expiresIn(d);
      } catch (java.time.format.DateTimeParseException e) {
        throw new IllegalArgumentException(
            "Binding '" + bindingName + "' judgment has invalid expiresIn: " + raw, e);
      }
    }
    if (node.has("expiresInExpression")) {
      b.expiresInExpression(node.get("expiresInExpression").asText());
    }
    if (node.has("evidenceRequirements")) {
      java.util.List<String> reqs = new java.util.ArrayList<>();
      node.get("evidenceRequirements").forEach(n -> reqs.add(n.asText()));
      b.evidenceRequirements(reqs);
    }
    if (node.has("verifierStrategy")) {
      b.verifierStrategy(node.get("verifierStrategy").asText());
    }
    if (node.has("escalatorStrategy")) {
      b.escalatorStrategy(node.get("escalatorStrategy").asText());
    }
    if (node.has("trustThreshold")) {
      b.trustThreshold(node.get("trustThreshold").asText());
    }
    if (node.has("title")) {
      b.title(node.get("title").asText());
    }
    if (node.has("titleExpression")) {
      b.titleExpression(node.get("titleExpression").asText());
    }
    if (node.has("outcomes")) {
      java.util.Set<String> outcomes = new java.util.LinkedHashSet<>();
      node.get("outcomes").forEach(n -> outcomes.add(n.asText()));
      b.outcomes(outcomes);
    }
    if (node.has("scope")) {
      b.scope(node.get("scope").asText());
    }
    if (node.has("scopeExpression")) {
      b.scopeExpression(node.get("scopeExpression").asText());
    }
    if (node.has("priority")) {
      b.priority(node.get("priority").asText());
    }
    if (node.has("expiresAtExpression")) {
      b.expiresAtExpression(node.get("expiresAtExpression").asText());
    }
    if (node.has("human")) {
      JsonNode humanNode = node.get("human");
      io.casehub.api.spi.routing.CandidateSetSpec cg = null;
      io.casehub.api.spi.routing.CandidateSetSpec cu = null;
      if (humanNode.has("candidateGroups")) {
        JsonNode cgn = humanNode.get("candidateGroups");
        if (cgn.isArray()) {
          java.util.Set<String> groups = new java.util.LinkedHashSet<>();
          cgn.forEach(n -> groups.add(n.asText()));
          cg =
              new io.casehub.api.spi.routing.CandidateSetSpec.Inline(
                  io.casehub.api.spi.routing.StaticSetStrategy.of(groups));
        } else if (cgn.isTextual()) {
          cg =
              new io.casehub.api.spi.routing.CandidateSetSpec.Inline(
                  new io.casehub.api.spi.routing.JqCandidateSetStrategy(cgn.asText()));
        }
      }
      if (humanNode.has("candidateUsers")) {
        JsonNode cun = humanNode.get("candidateUsers");
        if (cun.isArray()) {
          java.util.Set<String> users = new java.util.LinkedHashSet<>();
          cun.forEach(n -> users.add(n.asText()));
          cu =
              new io.casehub.api.spi.routing.CandidateSetSpec.Inline(
                  io.casehub.api.spi.routing.StaticSetStrategy.of(users));
        } else if (cun.isTextual()) {
          cu =
              new io.casehub.api.spi.routing.CandidateSetSpec.Inline(
                  new io.casehub.api.spi.routing.JqCandidateSetStrategy(cun.asText()));
        }
      }
      String templateRef =
          humanNode.has("templateRef") ? humanNode.get("templateRef").asText() : null;
      Integer claimDeadlineHours =
          humanNode.has("claimDeadlineHours") ? humanNode.get("claimDeadlineHours").asInt() : null;
      Class<?> payloadType = null;
      if (humanNode.has("payloadType")) {
        try {
          payloadType = Class.forName(humanNode.get("payloadType").asText());
        } catch (ClassNotFoundException e) {
          throw new IllegalArgumentException(
              "Binding '"
                  + bindingName
                  + "' judgment human payloadType not found: "
                  + humanNode.get("payloadType").asText(),
              e);
        }
      }
      b.human(new HumanRoutingConfig(templateRef, cg, cu, claimDeadlineHours, payloadType));
    }
    return b.build();
  }

  private ExpressionEvaluator readExpressionWithContext(
      JsonNode node,
      String fieldName,
      String bindingName,
      ObjectCodec codec,
      DeserializationContext ctxt)
      throws IOException {
    try {
      ExpressionEvaluator result = readValue(node, ExpressionEvaluator.class, codec, ctxt);
      if (result instanceof io.casehub.api.model.evaluator.JQExpressionEvaluator jq) {
        io.casehub.api.model.evaluator.JQExpressionEvaluator.validate(jq.expression());
      }
      return result;
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Binding '" + bindingName + "' has invalid " + fieldName + ": " + e.getMessage(), e);
    }
  }

  private OutcomePolicy deserializeOutcomePolicy(JsonNode node) {
    OutcomeAction onDecline =
        node.has("onDecline")
            ? OutcomeAction.valueOf(node.get("onDecline").asText())
            : OutcomeAction.REROUTE;
    OutcomeAction onFailure =
        node.has("onFailure")
            ? OutcomeAction.valueOf(node.get("onFailure").asText())
            : OutcomeAction.REROUTE;
    OutcomeAction onExpired =
        node.has("onExpired")
            ? OutcomeAction.valueOf(node.get("onExpired").asText())
            : OutcomeAction.REROUTE;
    int maxAttempts = node.has("maxRerouteAttempts") ? node.get("maxRerouteAttempts").asInt() : 3;
    return new OutcomePolicy(onDecline, onFailure, onExpired, maxAttempts);
  }

  private RecoveryOverride deserializeRecoveryOverride(JsonNode node) {
    Set<OutcomeType> skipFor = new HashSet<>();
    if (node.has("skipRecoveryFor")) {
      node.get("skipRecoveryFor").forEach(n -> skipFor.add(OutcomeType.valueOf(n.asText())));
    }
    return new RecoveryOverride(
        node.has("maxRetries") ? node.get("maxRetries").asInt() : null,
        node.has("maxRerouteAttempts") ? node.get("maxRerouteAttempts").asInt() : null,
        node.has("maxLevel") ? RecoveryLevel.valueOf(node.get("maxLevel").asText()) : null,
        node.has("skipRecovery") && node.get("skipRecovery").asBoolean(),
        skipFor);
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

  @Override
  public Binding getNullValue(DeserializationContext ctxt) {
    return null;
  }
}
