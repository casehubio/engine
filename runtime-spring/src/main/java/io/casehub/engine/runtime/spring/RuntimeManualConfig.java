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
package io.casehub.engine.runtime.spring;

import io.casehub.api.engine.ExpressionEngineRegistry;
import io.casehub.api.engine.LoopControl;
import io.casehub.api.model.cbr.CbrCaseTypeRegistration;
import io.casehub.api.spi.CaseChannelProvider;
import io.casehub.api.spi.DispatchBudget;
import io.casehub.api.spi.WorkerContextProvider;
import io.casehub.api.spi.WorkerProvisioner;
import io.casehub.api.spi.event.EventDispatcher;
import io.casehub.api.spi.routing.GoalRemovalService;
import io.casehub.eidos.api.AgentRegistry;
import io.casehub.eidos.api.CapabilityHealth;
import io.casehub.eidos.api.GoalEvolution;
import io.casehub.eidos.api.GoalSignalStore;
import io.casehub.engine.common.internal.context.BridgeResolver;
import io.casehub.engine.common.internal.jq.JQEvaluator;
import io.casehub.engine.common.internal.worker.scope.ScopedWorkerRegistry;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.engine.common.spi.EventLogRepository;
import io.casehub.engine.common.spi.PlanItemStore;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.casehub.engine.common.spi.scheduler.WorkerExecutionManager;
import io.casehub.engine.common.spi.scheduler.WorkerExecutionRoutingStrategy;
import io.casehub.engine.runtime.acl.WorkerGrantOrchestrator;
import io.casehub.engine.runtime.engine.CaseCompletionTracker;
import io.casehub.engine.runtime.engine.CaseEvaluationSerializer;
import io.casehub.engine.runtime.engine.QuiescenceTracker;
import io.casehub.engine.runtime.engine.SignalSettlementTracker;
import io.casehub.engine.runtime.engine.handler.CaseContextChangedEventHandler;
import io.casehub.engine.runtime.engine.handler.WorkerScheduleEventHandler;
import io.casehub.engine.runtime.executor.WorkerRuntimeFactory;
import io.casehub.engine.runtime.memory.AgentExperienceRecorder;
import io.casehub.engine.runtime.memory.AgentMemoryRetriever;
import io.casehub.engine.runtime.routing.AgentCandidateFactory;
import io.casehub.engine.runtime.routing.CbrRetrievalService;
import io.casehub.engine.runtime.routing.GoalAbandonmentEvaluator;
import io.casehub.engine.runtime.routing.GoalFormationEvaluator;
import io.casehub.engine.runtime.routing.GoalRevisionEvaluator;
import io.casehub.engine.runtime.routing.SelectionContextStore;
import io.casehub.engine.runtime.worker.CompositeWorkerExecutionManager;
import io.casehub.ledger.api.spi.LedgerTraceIdProvider;
import io.casehub.neocortex.memory.CaseMemoryStore;
import io.casehub.neocortex.memory.cbr.CbrPlanAdapter;
import io.casehub.neocortex.memory.cbr.CbrPlanEnsembleAnalyzer;
import io.casehub.neocortex.memory.cbr.CbrRecordStore;
import io.casehub.platform.api.routing.StrategyResolver;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class RuntimeManualConfig {

  @Bean
  public SpringEventDispatcher springEventDispatcher(ApplicationEventPublisher publisher) {
    return new SpringEventDispatcher(publisher);
  }

  @Bean
  public ExecutorService runtimeVirtualThreadExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
  }

  @Bean
  public GoalRevisionEvaluator goalRevisionEvaluator(
      ObjectProvider<GoalSignalStore> goalSignalStore,
      ObjectProvider<GoalEvolution> goalEvolution,
      ObjectProvider<AgentRegistry> agentRegistry,
      GoalRemovalService goalRemovalService,
      CaseDefinitionRegistry caseDefinitionRegistry,
      StrategyResolver strategyResolver,
      EventLogRepository eventLogRepository,
      @Value("${casehub.goal-revision.enabled:false}") boolean enabled,
      @Value("${casehub.goal-revision.strategy:default}") String strategyId,
      @Value("${casehub.goal-revision.min-outcomes:3}") int minOutcomes,
      @Value("${casehub.goal-revision.importance-threshold:0.3}") double importanceThreshold) {
    return new GoalRevisionEvaluator(
        Optional.ofNullable(goalSignalStore.getIfAvailable()),
        Optional.ofNullable(goalEvolution.getIfAvailable()),
        Optional.ofNullable(agentRegistry.getIfAvailable()),
        goalRemovalService,
        caseDefinitionRegistry,
        strategyResolver,
        eventLogRepository,
        enabled,
        strategyId,
        minOutcomes,
        importanceThreshold);
  }

  @Bean
  public GoalFormationEvaluator goalFormationEvaluator(
      ObjectProvider<AgentRegistry> agentRegistry,
      ObjectProvider<io.casehub.api.spi.routing.GoalFormationService> goalFormationService,
      ObjectProvider<CaseMemoryStore> caseMemoryStore,
      CaseDefinitionRegistry caseDefinitionRegistry,
      StrategyResolver strategyResolver,
      EventLogRepository eventLogRepository,
      @Value("${casehub.engine.goal.formation.enabled:false}") boolean enabled,
      @Value("${casehub.engine.goal.formation.auto-approve:true}") boolean autoApprove,
      @Value("${casehub.engine.goal.formation.strategy:llm}") String strategyId,
      @Value("${casehub.engine.goal.formation.max-new-per-reflection:2}") int maxNewPerReflection,
      @Value("${casehub.engine.goal.formation.cooldown-minutes:60}") long cooldownMinutes,
      @Value("${casehub.engine.goal.formation.max-memories:20}") int maxMemories) {
    return new GoalFormationEvaluator(
        Optional.ofNullable(agentRegistry.getIfAvailable()),
        Optional.ofNullable(goalFormationService.getIfAvailable()),
        Optional.ofNullable(caseMemoryStore.getIfAvailable()),
        caseDefinitionRegistry,
        strategyResolver,
        eventLogRepository,
        enabled,
        autoApprove,
        strategyId,
        maxNewPerReflection,
        cooldownMinutes,
        maxMemories);
  }

  @Bean
  public AgentExperienceRecorder agentExperienceRecorder(
      ObjectProvider<io.casehub.neocortex.memory.experience.ExperienceRecorder> experienceRecorder,
      ObjectProvider<io.casehub.neocortex.memory.reflection.ReflectionOrchestrator>
          reflectionOrchestrator,
      CaseDefinitionRegistry caseDefinitionRegistry,
      GoalFormationEvaluator goalFormationEvaluator,
      ObjectProvider<CaseMemoryStore> caseMemoryStore,
      ObjectProvider<io.micrometer.core.instrument.MeterRegistry> meterRegistry,
      @Value("${casehub.reasoning.enabled:true}") boolean reasoningEnabled) {
    return new AgentExperienceRecorder(
        Optional.ofNullable(experienceRecorder.getIfAvailable()),
        Optional.ofNullable(reflectionOrchestrator.getIfAvailable()),
        caseDefinitionRegistry,
        goalFormationEvaluator,
        Optional.ofNullable(caseMemoryStore.getIfAvailable()),
        Optional.ofNullable(meterRegistry.getIfAvailable()),
        reasoningEnabled);
  }

  @Bean
  public CbrRetrievalService cbrRetrievalService(
      JQEvaluator jqEvaluator,
      CbrRecordStore cbrStore,
      CbrPlanAdapter planAdapter,
      CbrPlanEnsembleAnalyzer ensembleAnalyzer,
      List<CbrCaseTypeRegistration> registrations,
      @Value("${casehub.engine.cbr.ensemble-timeout-ms:5000}") long ensembleTimeoutMs) {
    return new CbrRetrievalService(
        jqEvaluator, cbrStore, planAdapter, ensembleAnalyzer, registrations, ensembleTimeoutMs);
  }

  @Bean
  public WorkerScheduleEventHandler workerScheduleEventHandler(
      WorkerExecutionManager workflowExecutionManager,
      io.casehub.api.spi.WorkerExecutionGuard workerExecutionGuard,
      QuiescenceTracker quiescenceTracker,
      WorkerContextProvider workerContextProvider,
      CaseChannelProvider caseChannelProvider,
      EventDispatcher eventDispatcher,
      EventLogRepository eventLogRepository,
      ExpressionEngineRegistry expressionEngineRegistry,
      BridgeResolver bridgeResolver,
      CaseDefinitionRegistry caseDefinitionRegistry,
      AgentMemoryRetriever agentMemoryRetriever,
      @Value("${casehub.idempotency.window:#{null}}") Optional<Duration> idempotencyWindow) {
    return new WorkerScheduleEventHandler(
        workflowExecutionManager,
        workerExecutionGuard,
        quiescenceTracker,
        workerContextProvider,
        caseChannelProvider,
        eventDispatcher,
        eventLogRepository,
        expressionEngineRegistry,
        bridgeResolver,
        caseDefinitionRegistry,
        agentMemoryRetriever,
        idempotencyWindow);
  }

  @Bean
  public CaseContextChangedEventHandler caseContextChangedEventHandler(
      EventDispatcher eventDispatcher,
      JQEvaluator jqEvaluator,
      CaseDefinitionRegistry caseDefinitionRegistry,
      ExpressionEngineRegistry expressionEngineRegistry,
      LoopControl loopControl,
      StrategyResolver strategyResolver,
      AgentCandidateFactory agentCandidateFactory,
      WorkerExecutionManager executionManager,
      CapabilityHealth capabilityHealth,
      WorkerContextProvider workerContextProvider,
      WorkerProvisioner workerProvisioner,
      ApplicationEventPublisher publisher,
      LedgerTraceIdProvider traceIdProvider,
      CbrRetrievalService cbrRetrievalService,
      BridgeResolver bridgeResolver,
      SignalSettlementTracker settlementTracker,
      WorkerGrantOrchestrator workerGrantOrchestrator,
      ExecutorService runtimeVirtualThreadExecutor,
      CaseEvaluationSerializer evaluationSerializer,
      QuiescenceTracker quiescenceTracker,
      ScopedWorkerRegistry scopedWorkerRegistry,
      SelectionContextStore selectionContextStore,
      DispatchBudget dispatchBudget,
      PlanItemStore planItemStore,
      ObjectProvider<io.casehub.engine.common.spi.JudgmentScheduler> judgmentScheduler,
      io.casehub.engine.common.internal.observation.ObservationRegistry observationRegistry,
      io.casehub.engine.common.internal.observation.ContextHistoryBuffer contextHistoryBuffer,
      io.casehub.engine.common.internal.signal.SignalRegistry signalRegistry,
      io.casehub.engine.common.internal.observation.RuleRegistry ruleRegistry,
      io.casehub.engine.common.internal.convergence.ActivityTracker activityTracker,
      io.casehub.engine.runtime.convergence.ConvergenceDetector convergenceDetector,
      io.casehub.engine.runtime.convergence.BudgetEnforcer budgetEnforcer,
      ObjectProvider<io.casehub.engine.runtime.stigmergy.StigmergyCoordinator> stigmergyCoordinator,
      ObjectProvider<io.casehub.engine.runtime.stigmergy.RoleTracker> roleTracker,
      ObjectProvider<io.casehub.engine.runtime.stigmergy.TeamDetector> teamDetector,
      ObjectProvider<io.casehub.engine.runtime.stigmergy.SwarmProgressTracker> swarmProgressTracker,
      ObjectProvider<io.casehub.engine.runtime.stigmergy.SwarmProvisioner> swarmProvisioner,
      ObjectProvider<io.casehub.engine.runtime.improvement.ImprovementGoalFormationStrategy>
          improvementStrategy,
      ObjectProvider<io.casehub.api.spi.routing.GoalFormationService> goalFormationService) {
    return new CaseContextChangedEventHandler(
        eventDispatcher,
        jqEvaluator,
        caseDefinitionRegistry,
        expressionEngineRegistry,
        loopControl,
        strategyResolver,
        agentCandidateFactory,
        executionManager,
        capabilityHealth,
        workerContextProvider,
        workerProvisioner,
        event -> publisher.publishEvent(event),
        traceIdProvider,
        cbrRetrievalService,
        bridgeResolver,
        settlementTracker,
        workerGrantOrchestrator,
        runtimeVirtualThreadExecutor,
        evaluationSerializer,
        quiescenceTracker,
        scopedWorkerRegistry,
        selectionContextStore,
        dispatchBudget,
        planItemStore,
        event -> publisher.publishEvent(event),
        Optional.ofNullable(judgmentScheduler.getIfAvailable()),
        observationRegistry,
        contextHistoryBuffer,
        signalRegistry,
        ruleRegistry,
        activityTracker,
        convergenceDetector,
        budgetEnforcer,
        Optional.ofNullable(stigmergyCoordinator.getIfAvailable()),
        Optional.ofNullable(roleTracker.getIfAvailable()),
        Optional.ofNullable(teamDetector.getIfAvailable()),
        Optional.ofNullable(swarmProgressTracker.getIfAvailable()),
        Optional.ofNullable(swarmProvisioner.getIfAvailable()),
        Optional.ofNullable(improvementStrategy.getIfAvailable()),
        Optional.ofNullable(goalFormationService.getIfAvailable()));
  }

  @Bean
  public GoalAbandonmentEvaluator goalAbandonmentEvaluator(
      ObjectProvider<GoalSignalStore> signalStore,
      @Value("${casehub.engine.goal.abandonment-threshold:5}") int threshold) {
    return new GoalAbandonmentEvaluator(
        Optional.ofNullable(signalStore.getIfAvailable()), threshold);
  }

  @Bean
  public CompositeWorkerExecutionManager compositeWorkerExecutionManager(
      WorkerExecutionRoutingStrategy routingStrategy, List<WorkerExecutionManager> backends) {
    return new CompositeWorkerExecutionManager(routingStrategy, backends);
  }

  @Bean
  public WorkerRuntimeFactory workerRuntimeFactory(
      io.casehub.api.engine.CaseHubRuntime caseHubRuntime,
      CaseDefinitionRegistry definitionRegistry,
      CaseInstanceCache caseInstanceCache,
      CaseCompletionTracker caseCompletionTracker,
      io.casehub.engine.common.internal.channel.DataChannelRegistry channelRegistry,
      io.casehub.api.spi.DataChannelFactory defaultChannelFactory,
      io.casehub.engine.common.internal.observation.ObservationRegistry observationRegistry,
      io.casehub.engine.common.internal.signal.SignalRegistry signalRegistry,
      PlanItemStore planItemStore,
      io.casehub.engine.common.internal.observation.RuleRegistry ruleRegistry,
      io.casehub.engine.common.internal.convergence.ActivityTracker activityTracker,
      ObjectProvider<io.casehub.engine.runtime.stigmergy.StigmergyCoordinator> stigmergyCoordinator,
      ObjectProvider<io.casehub.engine.runtime.stigmergy.RoleTracker> roleTracker,
      ObjectProvider<io.casehub.engine.runtime.stigmergy.TeamDetector> teamDetector,
      ObjectProvider<io.casehub.engine.runtime.stigmergy.SwarmProgressTracker>
          swarmProgressTracker) {
    var factory =
        new WorkerRuntimeFactory(
            caseHubRuntime,
            definitionRegistry,
            caseInstanceCache,
            caseCompletionTracker,
            channelRegistry,
            defaultChannelFactory,
            observationRegistry,
            signalRegistry,
            planItemStore,
            ruleRegistry);
    if (stigmergyCoordinator.getIfAvailable() != null) {
      factory.setStigmergyCoordinator(stigmergyCoordinator.getIfAvailable());
    }
    if (roleTracker.getIfAvailable() != null) {
      factory.setSwarmTrackers(
          activityTracker,
          roleTracker.getIfAvailable(),
          teamDetector.getIfAvailable(),
          swarmProgressTracker.getIfAvailable());
    }
    return factory;
  }

  @Bean
  @ConditionalOnBean(io.casehub.engine.runtime.stigmergy.StigmergyCoordinator.class)
  public io.casehub.engine.runtime.stigmergy.SwarmProvisioner swarmProvisioner(
      WorkerProvisioner workerProvisioner,
      io.casehub.engine.runtime.stigmergy.StigmergyCoordinator coordinator,
      io.casehub.engine.common.internal.signal.SignalRegistry signalRegistry,
      io.casehub.engine.common.internal.convergence.ActivityTracker activityTracker,
      io.casehub.engine.runtime.stigmergy.RoleTracker roleTracker,
      io.casehub.engine.runtime.stigmergy.TeamDetector teamDetector,
      io.casehub.engine.runtime.stigmergy.SwarmProgressTracker progressTracker,
      DispatchBudget dispatchBudget,
      ObjectProvider<io.casehub.api.spi.stigmergy.SwarmProvisioningAdvisor> advisorProvider) {
    return new io.casehub.engine.runtime.stigmergy.SwarmProvisioner(
        workerProvisioner,
        coordinator,
        signalRegistry,
        activityTracker,
        roleTracker,
        teamDetector,
        progressTracker,
        dispatchBudget,
        advisorProvider.getIfAvailable());
  }

  @Bean
  @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
  public io.casehub.api.spi.ContextDiffStrategy contextDiffStrategy(
      @Value("${casehub.engine.diff-strategy:none}") String strategy) {
    return switch (strategy) {
      case "none" -> new io.casehub.engine.runtime.diff.NoOpContextDiffStrategy();
      case "top-level" -> new io.casehub.engine.runtime.diff.TopLevelContextDiffStrategy();
      case "json-patch" -> new io.casehub.engine.runtime.diff.JsonPatchContextDiffStrategy();
      default ->
          throw new IllegalStateException(
              "Unknown casehub.engine.diff-strategy: '"
                  + strategy
                  + "'. Valid values: none, top-level, json-patch");
    };
  }

  @Bean
  @org.springframework.beans.factory.annotation.Qualifier("yamlMapper")
  public com.fasterxml.jackson.databind.ObjectMapper yamlObjectMapper(
      io.casehub.engine.common.internal.config.ConfigContext configContext) {
    com.fasterxml.jackson.databind.ObjectMapper mapper =
        io.casehub.yaml.jackson.YamlMappers.create();
    com.fasterxml.jackson.databind.module.SimpleModule module =
        new com.fasterxml.jackson.databind.module.SimpleModule("ConfigSecretResolvingModule");
    module.addDeserializer(
        String.class,
        new io.casehub.engine.runtime.marshaller.ConfigSecretResolvingDeserializer(configContext));
    mapper.registerModule(module);
    return mapper;
  }

  @Bean
  public io.casehub.engine.common.spi.CrossTenantEventLogRepository crossTenantEventLogRepository(
      io.casehub.engine.common.spi.CrossTenantEventLogRepository repo) {
    return repo;
  }

  @Bean
  public io.casehub.engine.common.spi.CrossTenantCaseInstanceRepository
      crossTenantCaseInstanceRepository(
          io.casehub.engine.common.spi.CrossTenantCaseInstanceRepository repo) {
    return repo;
  }
}
