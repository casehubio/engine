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
package io.casehub.engine.runtime.recovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.PlaybookExecutionResult;
import io.casehub.api.model.RecoveryLevel;
import io.casehub.api.model.RecoveryOverride;
import io.casehub.api.model.RecoveryPolicy;
import io.casehub.api.model.StepResult;
import io.casehub.api.model.StepStatus;
import io.casehub.api.spi.recovery.ErrorClassifier;
import io.casehub.engine.common.internal.event.EventBusAddresses;
import io.casehub.engine.common.internal.model.CaseInstance;
import io.casehub.engine.common.internal.model.CaseMetaModel;
import io.casehub.engine.common.internal.store.InMemoryPlanVersionStore;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.engine.common.spi.EventLogRepository;
import io.casehub.engine.common.spi.GoalDecomposer;
import io.casehub.engine.common.spi.PlanAdaptationEvaluator;
import io.casehub.engine.common.spi.cache.CaseInstanceCache;
import io.casehub.engine.common.spi.recovery.CompoundLockRegistry;
import io.casehub.engine.common.spi.recovery.RecoveryContext;
import io.casehub.platform.api.governance.TriageContext;
import io.casehub.platform.api.governance.TriageDecision;
import io.casehub.platform.api.governance.TriageHandler;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.WorkerOutcome;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.inject.Instance;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultRecoveryCoordinatorTriageTest {

  private DefaultRecoveryCoordinator coordinator;
  private TriageHandler triageHandler;
  private ErrorClassifier classifier;
  private CaseRecoveryStateRegistry stateRegistry;
  private CaseDefinitionRegistry definitionRegistry;
  private CaseInstanceCache caseInstanceCache;
  private EventBus eventBus;

  @BeforeEach
  void setUp() {
    classifier = mock(ErrorClassifier.class);
    stateRegistry = new CaseRecoveryStateRegistry();
    triageHandler = mock(TriageHandler.class);
    eventBus = mock(EventBus.class);
    caseInstanceCache = mock(CaseInstanceCache.class);
    definitionRegistry = mock(CaseDefinitionRegistry.class);

    @SuppressWarnings("unchecked")
    Instance<TriageHandler> triageInstance = mock(Instance.class);
    when(triageInstance.isResolvable()).thenReturn(true);
    when(triageInstance.get()).thenReturn(triageHandler);

    @SuppressWarnings("unchecked")
    Instance<io.casehub.engine.common.spi.PlanItemStore> planItemStoreInstance =
        mock(Instance.class);
    when(planItemStoreInstance.isResolvable()).thenReturn(false);

    coordinator =
        new DefaultRecoveryCoordinator(
            classifier,
            stateRegistry,
            new InMemoryPlanVersionStore(),
            mock(PlanAdaptationEvaluator.class),
            mock(GoalDecomposer.class),
            definitionRegistry,
            caseInstanceCache,
            mock(EventLogRepository.class),
            eventBus,
            new CompoundLockRegistry(),
            planItemStoreInstance,
            triageInstance);
  }

  @Test
  void triageFiresAfterStructuralRecoveryExhaustion() {
    when(classifier.classify(any())).thenReturn(RecoveryLevel.FUNDAMENTAL);
    RecoveryContext ctx = buildContextWithFailure();
    setupRecoveryEnabled(ctx);
    stateRegistry.getOrCreate(ctx.caseId()).markReplanAttempted();

    when(triageHandler.evaluate(any()))
        .thenReturn(new TriageDecision.Retry(Map.of("playbook.skipSteps", List.of("s2"))));

    boolean handled = coordinator.handleFailure(ctx);

    assertThat(handled).isTrue();
    verify(triageHandler).evaluate(any(TriageContext.class));
    verify(eventBus).publish(eq(EventBusAddresses.CONTEXT_CHANGED), any());
  }

  @Test
  void triageReturnsFalseOnAbandon() {
    when(classifier.classify(any())).thenReturn(RecoveryLevel.FUNDAMENTAL);
    RecoveryContext ctx = buildContextWithFailure();
    setupRecoveryEnabled(ctx);
    stateRegistry.getOrCreate(ctx.caseId()).markReplanAttempted();

    when(triageHandler.evaluate(any()))
        .thenReturn(new TriageDecision.Abandon("no viable modification"));

    boolean handled = coordinator.handleFailure(ctx);

    assertThat(handled).isFalse();
    verify(triageHandler).evaluate(any(TriageContext.class));
    verify(eventBus).publish(eq(EventBusAddresses.COORDINATION_EXHAUSTED), any());
  }

  @Test
  void triageRespectsMaxAttempts() {
    when(classifier.classify(any())).thenReturn(RecoveryLevel.FUNDAMENTAL);
    RecoveryContext ctx = buildContextWithFailure();
    setupRecoveryEnabled(ctx);
    stateRegistry.getOrCreate(ctx.caseId()).markReplanAttempted();
    var bindingState = stateRegistry.getOrCreate(ctx.caseId()).getOrCreate(ctx.bindingName());
    bindingState.incrementTriageAttemptCount();
    bindingState.incrementTriageAttemptCount();

    boolean handled = coordinator.handleFailure(ctx);

    assertThat(handled).isFalse();
    verify(triageHandler, never()).evaluate(any());
  }

  @Test
  void skipTriagePreventsTriage() {
    when(classifier.classify(any())).thenReturn(RecoveryLevel.FUNDAMENTAL);
    RecoveryContext ctx = buildContextWithFailure();
    setupRecoveryEnabledWithSkipTriage(ctx);
    stateRegistry.getOrCreate(ctx.caseId()).markReplanAttempted();

    boolean handled = coordinator.handleFailure(ctx);

    assertThat(handled).isFalse();
    verify(triageHandler, never()).evaluate(any());
  }

  @Test
  void triageRecordsModificationsInBindingState() {
    when(classifier.classify(any())).thenReturn(RecoveryLevel.FUNDAMENTAL);
    RecoveryContext ctx = buildContextWithFailure();
    setupRecoveryEnabled(ctx);
    stateRegistry.getOrCreate(ctx.caseId()).markReplanAttempted();

    Map<String, Object> mods = Map.of("playbook.skipSteps", List.of("s2"));
    when(triageHandler.evaluate(any())).thenReturn(new TriageDecision.Retry(mods));

    coordinator.handleFailure(ctx);

    var bindingState = stateRegistry.getOrCreate(ctx.caseId()).getOrCreate(ctx.bindingName());
    assertThat(bindingState.triageAttemptCount()).isEqualTo(1);
    assertThat(bindingState.previousTriageModifications()).hasSize(1);
  }

  @Test
  void skipRecoveryFallsThroughToTriage() {
    when(classifier.classify(any())).thenReturn(RecoveryLevel.FUNDAMENTAL);
    RecoveryContext ctx = buildContextWithFailure();
    setupRecoveryEnabledWithSkipRecoveryButNotTriage(ctx);

    when(triageHandler.evaluate(any()))
        .thenReturn(new TriageDecision.Retry(Map.of("playbook.skipSteps", List.of("s1"))));

    boolean handled = coordinator.handleFailure(ctx);

    assertThat(handled).isTrue();
    verify(triageHandler).evaluate(any(TriageContext.class));
  }

  private RecoveryContext buildContextWithFailure() {
    var step1 = new StepResult("s1", "fetch", StepStatus.SUCCEEDED, Duration.ofMillis(100), null);
    var step2 = new StepResult("s2", "transform", StepStatus.FAILED, Duration.ofMillis(10), "NPE");
    var execResult = new PlaybookExecutionResult.Failure("NPE in transform", List.of(step1, step2));
    return new RecoveryContext(
        UUID.randomUUID(),
        "tenant-1",
        "binding-1",
        "worker-1",
        "capability-1",
        new WorkerOutcome.Failed<>("failed"),
        3,
        UUID.randomUUID(),
        execResult);
  }

  private void setupRecoveryEnabled(RecoveryContext ctx) {
    CaseDefinition def =
        CaseDefinition.builder()
            .namespace("test")
            .name("triage")
            .version("1.0")
            .recoveryPolicy(RecoveryPolicy.DEFAULT)
            .capabilities(Capability.of("capability-1", ".", "."))
            .bindings(
                Binding.builder()
                    .name("binding-1")
                    .capability(Capability.of("capability-1", ".", "."))
                    .on(new ContextChangeTrigger(".ready"))
                    .build())
            .build();
    setupInstance(ctx, def);
  }

  private void setupRecoveryEnabledWithSkipTriage(RecoveryContext ctx) {
    CaseDefinition def =
        CaseDefinition.builder()
            .namespace("test")
            .name("triage")
            .version("1.0")
            .recoveryPolicy(RecoveryPolicy.DEFAULT)
            .capabilities(Capability.of("capability-1", ".", "."))
            .bindings(
                Binding.builder()
                    .name("binding-1")
                    .capability(Capability.of("capability-1", ".", "."))
                    .on(new ContextChangeTrigger(".ready"))
                    .recoveryOverride(
                        new RecoveryOverride(null, null, null, false, Set.of(), true, null))
                    .build())
            .build();
    setupInstance(ctx, def);
  }

  private void setupRecoveryEnabledWithSkipRecoveryButNotTriage(RecoveryContext ctx) {
    CaseDefinition def =
        CaseDefinition.builder()
            .namespace("test")
            .name("triage")
            .version("1.0")
            .recoveryPolicy(RecoveryPolicy.DEFAULT)
            .capabilities(Capability.of("capability-1", ".", "."))
            .bindings(
                Binding.builder()
                    .name("binding-1")
                    .capability(Capability.of("capability-1", ".", "."))
                    .on(new ContextChangeTrigger(".ready"))
                    .recoveryOverride(
                        new RecoveryOverride(null, null, null, true, Set.of(), false, null))
                    .build())
            .build();
    setupInstance(ctx, def);
  }

  private void setupInstance(RecoveryContext ctx, CaseDefinition def) {
    CaseMetaModel meta = new CaseMetaModel();
    meta.setNamespace("test");
    meta.setName("triage");
    meta.setVersion("1.0");
    CaseInstance instance = new CaseInstance();
    instance.setUuid(ctx.caseId());
    instance.tenancyId = ctx.tenancyId();
    instance.setCaseMetaModel(meta);
    var caseContext = mock(io.casehub.api.context.MutableCaseContext.class);
    when(caseContext.snapshot()).thenReturn(caseContext);
    instance.setCaseContext(caseContext);
    when(caseInstanceCache.get(ctx.caseId())).thenReturn(instance);
    when(definitionRegistry.getCaseDefinition(meta)).thenReturn(def);
  }
}
