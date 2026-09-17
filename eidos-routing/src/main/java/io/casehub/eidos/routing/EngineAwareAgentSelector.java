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
package io.casehub.eidos.routing;

import io.casehub.api.spi.routing.AgentCandidate;
import io.casehub.api.spi.routing.AgentHealth;
import io.casehub.api.spi.routing.AgentRoutingContext;
import io.casehub.api.spi.routing.AgentRoutingStrategy;
import io.casehub.api.spi.routing.EscalationReason;
import io.casehub.api.spi.routing.RoutingResult;
import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentMatch;
import io.casehub.eidos.api.AgentSelection;
import io.casehub.eidos.api.AgentSelector;
import io.casehub.eidos.api.CapabilityHealth;
import io.casehub.eidos.api.CapabilityHealth.CapabilityStatus;
import io.casehub.eidos.api.CapabilityHealth.ProbeContext;
import io.casehub.eidos.api.EscalationKind;
import io.casehub.eidos.api.SelectionContext;
import io.casehub.ledger.api.spi.TrustScoreSource;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Alternative
@Priority(1)
@ApplicationScoped
public class EngineAwareAgentSelector implements AgentSelector {

  private final CapabilityHealth capabilityHealth;
  private final Instance<AgentRoutingStrategy> routingStrategies;
  private final Instance<TrustScoreSource> trustSourceInstance;

  @Inject
  public EngineAwareAgentSelector(
      CapabilityHealth capabilityHealth,
      Instance<AgentRoutingStrategy> routingStrategies,
      Instance<TrustScoreSource> trustSourceInstance) {
    this.capabilityHealth = capabilityHealth;
    this.routingStrategies = routingStrategies;
    this.trustSourceInstance = trustSourceInstance;
  }

  @Override
  public AgentSelection select(List<AgentMatch> candidates, SelectionContext context) {
    if (candidates.isEmpty()) {
      return new AgentSelection.NoneQualified("no candidates");
    }

    var converted = convertAndFilter(candidates, context);
    if (converted.isEmpty()) {
      return new AgentSelection.NoneQualified(
          "all %d candidates unhealthy".formatted(candidates.size()));
    }

    var strategy = routingStrategies.get();
    var routingContext = toRoutingContext(context);
    var result =
        strategy.select(
            routingContext, converted.stream().map(CandidateMapping::candidate).toList());

    return toAgentSelection(result, converted, context);
  }

  private List<CandidateMapping> convertAndFilter(
      List<AgentMatch> matches, SelectionContext context) {
    var result = new ArrayList<CandidateMapping>(matches.size());
    for (var match : matches) {
      var descriptor = match.descriptor();
      var capTag =
          match.resolvedCapability() != null
              ? match.resolvedCapability().capability().name()
              : context.capabilityName();
      var status =
          capTag != null
              ? capabilityHealth.probe(descriptor, capTag, ProbeContext.of(context.taskDomain()))
              : new CapabilityStatus.Ready();
      var health = mapHealth(status);
      if (health == null) {
        continue;
      }
      Map<String, Integer> violations =
          status instanceof CapabilityStatus.BehavioralViolation bv ? bv.violations() : null;
      var candidate =
          new AgentCandidate(
              descriptor.agentId(),
              descriptor.capabilities().stream()
                  .map(AgentCapability::name)
                  .collect(Collectors.toSet()),
              0,
              health,
              descriptor,
              match.resolvedCapability() != null ? match.resolvedCapability().degree() : null,
              violations);
      result.add(new CandidateMapping(candidate, match));
    }
    return result;
  }

  private AgentHealth mapHealth(CapabilityStatus status) {
    return switch (status) {
      case CapabilityStatus.Ready r -> AgentHealth.READY;
      case CapabilityStatus.Degraded d -> AgentHealth.DEGRADED;
      case CapabilityStatus.EpistemicallyWeak ew -> AgentHealth.EPISTEMICALLY_WEAK;
      case CapabilityStatus.BehavioralViolation bv -> AgentHealth.BEHAVIORAL_VIOLATION;
      case CapabilityStatus.Overloaded o -> null;
      case CapabilityStatus.Unavailable u -> null;
      case CapabilityStatus.Excluded ex -> null;
    };
  }

  private AgentRoutingContext toRoutingContext(SelectionContext context) {
    return new AgentRoutingContext(
        null, context.capabilityName(), null, context.tenancyId(), List.of(), null, null);
  }

  private AgentSelection toAgentSelection(
      RoutingResult result, List<CandidateMapping> mappings, SelectionContext context) {
    return switch (result) {
      case RoutingResult.Selected sel -> {
        var assignment = sel.single();
        var mapping =
            mappings.stream()
                .filter(m -> m.candidate().workerId().equals(assignment.executorId()))
                .findFirst()
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "Strategy returned unknown executorId: " + assignment.executorId()));
        var trustScore = lookupTrustScore(assignment.executorId(), context.capabilityName());
        yield new AgentSelection.Selected(
            mapping.match().descriptor(),
            mapping.match().resolvedCapability(),
            trustScore,
            assignment.reason() != null ? assignment.reason() : "engine selected");
      }
      case RoutingResult.Unresolvable u -> new AgentSelection.NoneQualified(u.reason());
      case RoutingResult.Escalated e ->
          new AgentSelection.Escalated(
              e.capabilityName() != null ? e.capabilityName() : context.capabilityName(),
              mapEscalationKind(e.escalationReason()),
              e.reason());
    };
  }

  private EscalationKind mapEscalationKind(EscalationReason reason) {
    return switch (reason) {
      case BORDERLINE_STALEMATE -> EscalationKind.BORDERLINE_STALEMATE;
      case NO_QUALIFIED_AGENT -> EscalationKind.NO_QUALIFIED_AGENT;
    };
  }

  private double lookupTrustScore(String agentId, String capabilityName) {
    if (!trustSourceInstance.isResolvable()) {
      return 0.0;
    }
    var source = trustSourceInstance.get();
    var capScore =
        capabilityName != null
            ? source.capabilityScore(agentId, capabilityName)
            : OptionalDouble.empty();
    if (capScore.isPresent()) {
      return capScore.getAsDouble();
    }
    return source.globalScore(agentId).orElse(0.0);
  }

  private record CandidateMapping(AgentCandidate candidate, AgentMatch match) {}
}
