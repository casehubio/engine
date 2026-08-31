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
package io.casehub.api.model.converter.yaml;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.casehub.api.model.AdaptationConfig;
import io.casehub.api.model.CaseCompletion;
import io.casehub.api.model.cbr.CbrConfig;
import io.casehub.api.model.converter.deser.AdaptationConfigDeserializer;
import io.casehub.api.model.converter.deser.CaseCompletionDeserializer;
import io.casehub.api.model.converter.deser.CbrConfigDeserializer;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record YamlCaseSpec(
    List<YamlCapability> capabilities,
    List<YamlGoal> goals,
    List<YamlMilestone> milestones,
    @JsonDeserialize(using = CaseCompletionDeserializer.class) CaseCompletion completion,
    String planningStrategy,
    String agentRouting,
    String implementationRouting,
    String humanTaskRouting,
    String candidateMatching,
    String decompositionStrategy,
    Integer maxDecompositionDepth,
    Integer maxAdaptations,
    Integer maxEscalations,
    @JsonAlias("cbr") @JsonDeserialize(using = CbrConfigDeserializer.class) CbrConfig cbrConfig,
    @JsonAlias("adaptation") @JsonDeserialize(using = AdaptationConfigDeserializer.class)
        AdaptationConfig adaptationConfig,
    YamlRecoveryPolicy recoveryPolicy,
    YamlMonitoringConfig monitoring,
    @JsonAlias("reflection") YamlReflectionTriggerConfig reflectionTrigger,
    YamlMemoryRetrievalConfig memoryRetrieval,
    YamlQuorumConfig quorum,
    YamlPlanningConstraints planningConstraints,
    YamlPortfolioConfig portfolioConfig,
    List<YamlContextConstraint> humanTaskContextConstraints,
    YamlWorkloadConstraint humanTaskWorkloadConstraint,
    Map<String, Double> routingSignalWeights,
    Map<String, YamlCognitiveDemand> cognitiveDemands,
    Map<String, List<String>> authorization,
    Map<String, String> workerServiceAccountIds,
    List<YamlCompound> compounds,
    List<YamlChannel> channels,
    @JsonAlias("goapActions") List<YamlGoapAction> actions,
    List<YamlContextLayer> layers,
    List<YamlWorker> workers,
    List<YamlBinding> bindings) {

  public YamlCaseSpec {
    if (capabilities == null) {
      capabilities = List.of();
    }
    if (goals == null) {
      goals = List.of();
    }
    if (milestones == null) {
      milestones = List.of();
    }
    if (humanTaskContextConstraints == null) {
      humanTaskContextConstraints = List.of();
    }
    if (routingSignalWeights == null) {
      routingSignalWeights = Map.of();
    }
    if (cognitiveDemands == null) {
      cognitiveDemands = Map.of();
    }
    if (authorization == null) {
      authorization = Map.of();
    }
    if (workerServiceAccountIds == null) {
      workerServiceAccountIds = Map.of();
    }
    if (compounds == null) {
      compounds = List.of();
    }
    if (channels == null) {
      channels = List.of();
    }
    if (actions == null) {
      actions = List.of();
    }
    if (layers == null) {
      layers = List.of();
    }
    if (workers == null) {
      workers = List.of();
    }
    if (bindings == null) {
      bindings = List.of();
    }
  }
}
