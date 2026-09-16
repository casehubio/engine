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
package io.casehub.engine.internal.quarkus;

import io.casehub.api.spi.ActionRiskClassifier;
import io.casehub.api.spi.CaseChannelProvider;
import io.casehub.api.spi.ChainedActionRiskClassifier;
import io.casehub.api.spi.DispatchBudget;
import io.casehub.api.spi.FailureClassifier;
import io.casehub.api.spi.RiskClassifier;
import io.casehub.api.spi.WorkerContextProvider;
import io.casehub.api.spi.WorkerProvisioner;
import io.casehub.api.spi.WorkerStatusListener;
import io.casehub.api.spi.judgment.JudgmentEscalator;
import io.casehub.api.spi.recovery.ErrorClassifier;
import io.casehub.api.spi.routing.HumanTaskRoutingStrategy;
import io.casehub.api.spi.routing.ImplementationRoutingStrategy;
import io.casehub.api.spi.routing.WorkloadDataProvider;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.CapabilityHealth;
import io.casehub.eidos.api.VocabularyRegistry;
import io.casehub.engine.common.spi.PlanItemStore;
import io.casehub.engine.common.spi.recovery.RecoveryCoordinator;
import io.casehub.engine.common.spi.scheduler.WorkerExecutionRoutingStrategy;
import io.casehub.engine.internal.routing.FirstSupportedRoutingStrategy;
import io.casehub.engine.internal.routing.NoOpHumanTaskRoutingStrategy;
import io.casehub.engine.internal.routing.NoOpImplementationRoutingStrategy;
import io.casehub.engine.internal.routing.NoOpWorkloadDataProvider;
import io.casehub.engine.internal.worker.DefaultErrorClassifier;
import io.casehub.engine.internal.worker.DefaultFailureClassifier;
import io.casehub.engine.internal.worker.DefaultJudgmentEscalator;
import io.casehub.engine.internal.worker.EmptyWorkerContextProvider;
import io.casehub.engine.internal.worker.NoOpAgentRegistry;
import io.casehub.engine.internal.worker.NoOpCapabilityHealth;
import io.casehub.engine.internal.worker.NoOpCaseChannelProvider;
import io.casehub.engine.internal.worker.NoOpDispatchBudget;
import io.casehub.engine.internal.worker.NoOpPlanItemStore;
import io.casehub.engine.internal.worker.NoOpRecoveryCoordinator;
import io.casehub.engine.internal.worker.NoOpVocabularyRegistry;
import io.casehub.engine.internal.worker.NoOpWorkerProvisioner;
import io.casehub.engine.internal.worker.NoOpWorkerStatusListener;
import io.casehub.platform.api.identity.CurrentPrincipal;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;

/**
 * {@link DefaultBean} producers for all SPI types consumed by {@link RuntimeBeans} and other engine
 * producer classes. Each method wraps a plain POJO from {@code runtime-core} so that CDI can
 * discover a fallback bean when no consumer-provided implementation is on the classpath.
 *
 * <p>Refs engine#1119.
 */
@ApplicationScoped
public class SpiDefaultBeans {

  // --- Worker lifecycle SPIs ---

  @Produces
  @DefaultBean
  @ApplicationScoped
  CaseChannelProvider noOpCaseChannelProvider() {
    return new NoOpCaseChannelProvider();
  }

  @Produces
  @DefaultBean
  @ApplicationScoped
  WorkerStatusListener noOpWorkerStatusListener() {
    return new NoOpWorkerStatusListener();
  }

  @Produces
  @DefaultBean
  @ApplicationScoped
  WorkerProvisioner noOpWorkerProvisioner() {
    return new NoOpWorkerProvisioner();
  }

  @Produces
  @DefaultBean
  @ApplicationScoped
  WorkerContextProvider emptyWorkerContextProvider(
      CaseChannelProvider caseChannelProvider, CurrentPrincipal currentPrincipal) {
    return new EmptyWorkerContextProvider(caseChannelProvider, currentPrincipal);
  }

  // --- Routing and dispatch SPIs ---

  @Produces
  @DefaultBean
  @ApplicationScoped
  DispatchBudget noOpDispatchBudget() {
    return new NoOpDispatchBudget();
  }

  @Produces
  @DefaultBean
  @ApplicationScoped
  WorkerExecutionRoutingStrategy firstSupportedRoutingStrategy() {
    return new FirstSupportedRoutingStrategy();
  }

  @Produces
  @DefaultBean
  @ApplicationScoped
  WorkloadDataProvider noOpWorkloadDataProvider() {
    return new NoOpWorkloadDataProvider();
  }

  @Produces
  @DefaultBean
  @ApplicationScoped
  HumanTaskRoutingStrategy noOpHumanTaskRoutingStrategy() {
    return new NoOpHumanTaskRoutingStrategy();
  }

  @Produces
  @DefaultBean
  @ApplicationScoped
  ImplementationRoutingStrategy noOpImplementationRoutingStrategy() {
    return new NoOpImplementationRoutingStrategy();
  }

  // --- Failure handling and recovery SPIs ---

  @Produces
  @DefaultBean
  @ApplicationScoped
  ActionRiskClassifier chainedActionRiskClassifier(
      @RiskClassifier Instance<ActionRiskClassifier> classifiers) {
    return new ChainedActionRiskClassifier(classifiers.stream().toList());
  }

  @Produces
  @DefaultBean
  @ApplicationScoped
  FailureClassifier defaultFailureClassifier() {
    return new DefaultFailureClassifier();
  }

  @Produces
  @DefaultBean
  @ApplicationScoped
  ErrorClassifier defaultErrorClassifier() {
    return new DefaultErrorClassifier();
  }

  @Produces
  @DefaultBean
  @ApplicationScoped
  RecoveryCoordinator noOpRecoveryCoordinator() {
    return new NoOpRecoveryCoordinator();
  }

  // --- Eidos SPIs ---

  @Produces
  @DefaultBean
  @ApplicationScoped
  CapabilityHealth noOpCapabilityHealth() {
    return new NoOpCapabilityHealth();
  }

  @Produces
  @DefaultBean
  @ApplicationScoped
  VocabularyRegistry noOpVocabularyRegistry() {
    return new NoOpVocabularyRegistry();
  }

  @Produces
  @DefaultBean
  @ApplicationScoped
  AgentRegistry noOpAgentRegistry() {
    return new NoOpAgentRegistry();
  }

  // --- Planning SPIs ---

  @Produces
  @DefaultBean
  @ApplicationScoped
  PlanItemStore noOpPlanItemStore() {
    return new NoOpPlanItemStore();
  }

  // --- Judgment SPIs ---

  @Produces
  @DefaultBean
  @ApplicationScoped
  JudgmentEscalator defaultJudgmentEscalator() {
    return new DefaultJudgmentEscalator();
  }
}
