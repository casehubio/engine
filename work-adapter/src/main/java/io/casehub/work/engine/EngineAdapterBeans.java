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
package io.casehub.work.engine;

import io.casehub.engine.common.internal.context.BridgeResolver;
import io.casehub.engine.common.internal.event.ActionGateCancelledEvent;
import io.casehub.engine.common.internal.event.EventBusAddresses;
import io.casehub.engine.common.internal.jq.JQEvaluator;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.engine.common.spi.CrossTenantCaseInstanceRepository;
import io.casehub.engine.common.spi.CrossTenantPlanItemStore;
import io.casehub.engine.common.spi.PlanItemStore;
import io.casehub.engine.common.spi.event.CaseLifecycleEvent;
import io.casehub.engine.common.spi.event.PlanItemObsoleteEvent;
import io.casehub.engine.common.spi.event.PlanItemStateChangedEvent;
import io.casehub.engine.planning.registry.BlackboardRegistry;
import io.casehub.engine.runtime.routing.EngineStrategyResolver;
import io.casehub.platform.api.datasource.DataSourceRegistry;
import io.casehub.platform.api.subscription.EventTypeRegistry;
import io.casehub.platform.api.subscription.SubscriptionStore;
import io.casehub.work.api.WorkItemEvent;
import io.casehub.work.api.WorkItemGroupLifecycleEvent;
import io.casehub.work.api.spi.ClaimSlaPolicy;
import io.casehub.work.api.spi.InstanceAssignmentStrategy;
import io.casehub.work.api.spi.SlaBreachPolicy;
import io.casehub.work.api.spi.WorkItemCreator;
import io.casehub.work.api.spi.WorkItemLifecycle;
import io.casehub.work.api.spi.WorkItemStore;
import io.casehub.work.api.spi.WorkerSelectionStrategy;
import io.casehub.work.engine.recovery.HumanTaskRecoveryService;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class EngineAdapterBeans {

    @Inject EventBus eventBus;
    @Inject ActionGateCancelledHandler actionGateCancelledHandlerRef;

    // --- Producers: already-cleaned POJOs from prior session ---

    @Produces
    public ActionGateWorkItemHandler actionGateWorkItemHandler(
            final WorkItemCreator workItemCreator) {
        return new ActionGateWorkItemHandler(workItemCreator);
    }

    @Produces
    public WorkActorStateContributor workActorStateContributor(final WorkItemStore workItemStore) {
        return new WorkActorStateContributor(workItemStore);
    }

    @Produces
    @ApplicationScoped
    public HumanTaskScheduleHandler humanTaskScheduleHandler(
            BlackboardRegistry registry,
            WorkItemCreator workItemCreator,
            PlanItemStore planItemStore) {
        return new HumanTaskScheduleHandler(registry, workItemCreator, planItemStore);
    }

    @Produces
    @ApplicationScoped
    public JudgmentWorkItemScheduler judgmentWorkItemScheduler(
            BlackboardRegistry registry,
            WorkItemCreator workItemCreator,
            PlanItemStore planItemStore) {
        return new JudgmentWorkItemScheduler(registry, workItemCreator, planItemStore);
    }

    // --- Producers: newly-cleaned POJOs ---

    @Produces
    @ApplicationScoped
    public CaseCompensationNotifier caseCompensationNotifier(
            Instance<DataSourceRegistry> dataSourceRegistryInstance) {
        return new CaseCompensationNotifier(
                dataSourceRegistryInstance.isResolvable()
                ? Optional.of(dataSourceRegistryInstance.get())
                : Optional.empty());
    }

    @Produces
    @ApplicationScoped
    public CompensationSubscriptionBootstrap compensationSubscriptionBootstrap(
            Instance<SubscriptionStore> subscriptionStoreInstance,
            Instance<EventTypeRegistry> eventTypeRegistryInstance) {
        return new CompensationSubscriptionBootstrap(
                subscriptionStoreInstance.isResolvable()
                ? Optional.of(subscriptionStoreInstance.get())
                : Optional.empty(),
                eventTypeRegistryInstance.isResolvable()
                ? Optional.of(eventTypeRegistryInstance.get())
                : Optional.empty());
    }

    @Produces
    @ApplicationScoped
    public ActionGateCancelledHandler actionGateCancelledHandler(
            WorkItemCreator workItemCreator,
            WorkItemLifecycle workItemLifecycle) {
        return new ActionGateCancelledHandler(workItemCreator, workItemLifecycle);
    }

    @Produces
    @ApplicationScoped
    public ActionGateCompletionApplier actionGateCompletionApplier() {
        return new ActionGateCompletionApplier(
                e -> eventBus.publish(EventBusAddresses.ACTION_GATE_APPROVED, e),
                e -> eventBus.publish(EventBusAddresses.ACTION_GATE_REJECTED, e),
                e -> eventBus.publish(EventBusAddresses.ACTION_GATE_EXPIRED, e));
    }

    @Produces
    @ApplicationScoped
    public WorkStrategyContributor workStrategyContributor(
            EngineStrategyResolver resolver,
            @Any Instance<WorkerSelectionStrategy> workerStrategies,
            @Any Instance<ClaimSlaPolicy> claimPolicies,
            @Any Instance<SlaBreachPolicy> breachPolicies,
            @Any Instance<InstanceAssignmentStrategy> assignmentStrategies) {
        return new WorkStrategyContributor(
                resolver,
                workerStrategies.stream().toList(),
                claimPolicies.stream().toList(),
                breachPolicies.stream().toList(),
                assignmentStrategies.stream().toList());
    }

    @Produces
    @ApplicationScoped
    public PlanItemCompletionApplier planItemCompletionApplier(
            BlackboardRegistry registry,
            CaseDefinitionRegistry caseDefinitionRegistry,
            CrossTenantCaseInstanceRepository caseInstanceRepository,
            JQEvaluator jqEvaluator,
            BridgeResolver bridgeResolver,
            Event<PlanItemStateChangedEvent> planItemStateChangedEvents,
            Event<PlanItemObsoleteEvent> planItemObsoleteEvents) {
        return new PlanItemCompletionApplier(
                registry,
                caseDefinitionRegistry,
                caseInstanceRepository,
                e -> eventBus.publish(EventBusAddresses.CONTEXT_CHANGED, e),
                jqEvaluator,
                bridgeResolver,
                planItemStateChangedEvents::fireAsync,
                planItemObsoleteEvents::fireAsync);
    }

    @Produces
    @ApplicationScoped
    public WorkItemLifecycleAdapter workItemLifecycleAdapter(
            BlackboardRegistry registry,
            CrossTenantCaseInstanceRepository caseInstanceRepository,
            PlanItemCompletionApplier applier,
            ActionGateCompletionApplier gateApplier) {
        return new WorkItemLifecycleAdapter(
                registry,
                caseInstanceRepository,
                e -> eventBus.publish(EventBusAddresses.CONTEXT_CHANGED, e),
                applier,
                gateApplier);
    }

    @Produces
    @ApplicationScoped
    public HumanTaskRecoveryService humanTaskRecoveryService(
            CrossTenantPlanItemStore planItemStore,
            WorkItemCreator workItemCreator,
            PlanItemCompletionApplier applier) {
        return new HumanTaskRecoveryService(planItemStore, workItemCreator, applier);
    }

    // --- CDI observers bridging to POJOs ---

    @Transactional
    void onWorkItemLifecycle(
            @ObservesAsync WorkItemEvent event,
            WorkItemLifecycleAdapter adapter) {
        adapter.onWorkItemLifecycle(event);
    }

    void onWorkItemGroupLifecycle(
            @ObservesAsync WorkItemGroupLifecycleEvent event,
            WorkItemLifecycleAdapter adapter) {
        adapter.onWorkItemGroupLifecycle(event);
    }

    void onCaseLifecycle(
            @ObservesAsync CaseLifecycleEvent event,
            CaseCompensationNotifier notifier) {
        notifier.onCaseLifecycle(event);
    }

    // --- EventBus consumer bridging to POJO ---

    @ConsumeEvent(value = EventBusAddresses.ACTION_GATE_CANCELLED)
    @RunOnVirtualThread
    @Transactional
    void onActionGateCancelled(ActionGateCancelledEvent event) {
        actionGateCancelledHandlerRef.onActionGateCancelled(event);
    }

    // --- Startup initialization ---

    void onStartup(@Observes StartupEvent ev,
                   CompensationSubscriptionBootstrap compensationBootstrap,
                   WorkStrategyContributor strategyContributor) {
        compensationBootstrap.init();
        strategyContributor.init();
    }

    @Transactional
    void onStartupRecovery(@Observes @Priority(25) StartupEvent ev,
                           HumanTaskRecoveryService recoveryService) {
        recoveryService.init();
    }
}
