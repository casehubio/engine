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
package io.casehub.engine;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import io.casehub.api.engine.CaseHub;
import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.event.CaseHubEventType;
import io.casehub.engine.common.internal.event.EventBusAddresses;
import io.casehub.engine.common.internal.event.ExpectationViolationEvent;
import io.casehub.engine.common.internal.history.EventLog;
import io.casehub.engine.common.spi.EventLogRepository;
import io.casehub.engine.plan.goap.GoapAction;
import io.casehub.engine.plan.monitoring.MonitoringConfig;
import io.casehub.platform.api.governance.ExecutionPolicy;
import io.casehub.platform.api.governance.RetryPolicy;
import io.casehub.platform.api.identity.TenancyConstants;
import io.casehub.worker.api.Capability;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerFunction;
import io.casehub.worker.api.WorkerResult;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test validating EventLog metadata formatting for the plan monitoring and expectation
 * validation pipeline. Refs engine#954, deferred from engine#927.
 *
 * <p>Covers: full case lifecycle with GOAP actions and producedKeys, metadata block structure on
 * WORKER_EXECUTION_COMPLETED EventLog entries, divergence ratio computation end-to-end, and
 * ExpectationViolationEvent publication when threshold is exceeded.
 */
@QuarkusTest
@TestProfile(EventLogMetadataValidationIntegrationTest.MemoryProfile.class)
class EventLogMetadataValidationIntegrationTest {

  @Inject PartialEffectsBean partialEffectsBean;
  @Inject AllEffectsBean allEffectsBean;
  @Inject ProducedKeysBean producedKeysBean;
  @Inject HighDivergenceBean highDivergenceBean;
  @Inject EventLogRepository eventLogRepository;

  @BeforeEach
  void setUp() {
    ViolationEventRecorder.events.clear();
  }

  @Test
  void goap_effects_partial_satisfaction_writes_metadata() {
    UUID caseId = partialEffectsBean.startCase(Map.of("task", "go"));

    await()
        .atMost(30, SECONDS)
        .untilAsserted(
            () -> {
              List<EventLog> completed = findCompletedEvents(caseId);
              assertThat(completed).hasSize(1);

              JsonNode metadata = completed.get(0).getMetadata();
              JsonNode validation = metadata.get("expectationValidation");
              assertThat(validation).isNotNull();
              assertThat(validation.get("totalExpectedEffects").asInt()).isEqualTo(2);
              assertThat(validation.get("violatedEffectCount").asInt()).isEqualTo(1);
              assertThat(validation.get("divergenceRatio").asDouble())
                  .isCloseTo(0.5, within(0.001));
              assertThat(validation.get("effectSource").asText()).isEqualTo("GOAP");
              assertThat(validation.get("adaptationGeneration").asInt()).isEqualTo(0);

              JsonNode violations = validation.get("violations");
              assertThat(violations.isArray()).isTrue();
              assertThat(violations.size()).isEqualTo(1);
              assertThat(violations.get(0).get("key").asText()).isEqualTo("scored");
              assertThat(violations.get(0).get("expected").asBoolean()).isTrue();
              assertThat(violations.get(0).get("actual").asText()).isEqualTo("UNKNOWN");
            });
  }

  @Test
  void all_goap_effects_satisfied_writes_zero_divergence() {
    UUID caseId = allEffectsBean.startCase(Map.of("task", "go"));

    await()
        .atMost(30, SECONDS)
        .untilAsserted(
            () -> {
              List<EventLog> completed = findCompletedEvents(caseId);
              assertThat(completed).hasSize(1);

              JsonNode metadata = completed.get(0).getMetadata();
              JsonNode validation = metadata.get("expectationValidation");
              assertThat(validation).isNotNull();
              assertThat(validation.get("totalExpectedEffects").asInt()).isEqualTo(2);
              assertThat(validation.get("violatedEffectCount").asInt()).isEqualTo(0);
              assertThat(validation.get("divergenceRatio").asDouble())
                  .isCloseTo(0.0, within(0.001));
              assertThat(validation.get("violations").size()).isEqualTo(0);
            });
  }

  @Test
  void produced_keys_metadata_uses_correct_source() {
    UUID caseId = producedKeysBean.startCase(Map.of("task", "go"));

    await()
        .atMost(30, SECONDS)
        .untilAsserted(
            () -> {
              List<EventLog> completed = findCompletedEvents(caseId);
              assertThat(completed).hasSize(1);

              JsonNode metadata = completed.get(0).getMetadata();
              JsonNode validation = metadata.get("expectationValidation");
              assertThat(validation).isNotNull();
              assertThat(validation.get("effectSource").asText()).isEqualTo("PRODUCED_KEYS");
              assertThat(validation.get("totalExpectedEffects").asInt()).isEqualTo(2);
              assertThat(validation.get("violatedEffectCount").asInt()).isEqualTo(1);
              assertThat(validation.get("divergenceRatio").asDouble())
                  .isCloseTo(0.5, within(0.001));
            });
  }

  @Test
  void violation_event_published_when_threshold_exceeded() {
    UUID caseId = highDivergenceBean.startCase(Map.of("task", "go"));

    await()
        .atMost(30, SECONDS)
        .untilAsserted(
            () -> {
              List<EventLog> completed = findCompletedEvents(caseId);
              assertThat(completed).hasSize(1);
              assertThat(completed.get(0).getMetadata().get("expectationValidation")).isNotNull();

              ExpectationViolationEvent event =
                  ViolationEventRecorder.events.stream()
                      .filter(e -> e.caseId().equals(caseId))
                      .findFirst()
                      .orElse(null);
              assertThat(event).isNotNull();
              assertThat(event.divergenceRatio()).isCloseTo(1.0, within(0.001));
              assertThat(event.violations()).hasSize(2);
              assertThat(event.bindingName()).isEqualTo("on-diverge");
              assertThat(event.workerName()).isEqualTo("diverge-worker");
            });
  }

  private List<EventLog> findCompletedEvents(final UUID caseId) {
    return eventLogRepository.findByCaseAndTypes(
        caseId,
        List.of(CaseHubEventType.WORKER_EXECUTION_COMPLETED),
        TenancyConstants.DEFAULT_TENANT_ID);
  }

  // ── CaseHub beans ────────────────────────────────────────────────────────

  @ApplicationScoped
  public static class PartialEffectsBean extends CaseHub {
    private final Capability cap =
        Capability.builder()
            .name("verify-data")
            .inputSchema("{ task: .task }")
            .outputSchema(".")
            .build();

    @Override
    public CaseDefinition getDefinition() {
      return CaseDefinition.builder()
          .namespace("test-partial-effects")
          .name("Partial Effects Test")
          .version("1.0.0")
          .monitoring(MonitoringConfig.defaults())
          .goapActions(
              List.of(
                  new GoapAction(
                      "verify-data", Map.of(), Map.of("resolved", true, "scored", true), 1.0)))
          .capabilities(cap)
          .workers(
              Worker.builder()
                  .name("partial-worker")
                  .capabilityName("verify-data")
                  .function(
                      new WorkerFunction.Sync<>(
                          Map.class,
                          Map.class,
                          (input, scope) -> WorkerResult.of(Map.of("resolved", true))))
                  .executionPolicy(new ExecutionPolicy(60000, new RetryPolicy(1, 100)))
                  .build())
          .bindings(
              Binding.builder()
                  .name("on-verify")
                  .capability(cap)
                  .on(new ContextChangeTrigger(".task == \"go\""))
                  .build())
          .build();
    }
  }

  @ApplicationScoped
  public static class AllEffectsBean extends CaseHub {
    private final Capability cap =
        Capability.builder()
            .name("verify-all")
            .inputSchema("{ task: .task }")
            .outputSchema(".")
            .build();

    @Override
    public CaseDefinition getDefinition() {
      return CaseDefinition.builder()
          .namespace("test-all-effects")
          .name("All Effects Test")
          .version("1.0.0")
          .monitoring(MonitoringConfig.defaults())
          .goapActions(
              List.of(
                  new GoapAction(
                      "verify-all", Map.of(), Map.of("resolved", true, "scored", true), 1.0)))
          .capabilities(cap)
          .workers(
              Worker.builder()
                  .name("all-effects-worker")
                  .capabilityName("verify-all")
                  .function(
                      new WorkerFunction.Sync<>(
                          Map.class,
                          Map.class,
                          (input, scope) ->
                              WorkerResult.of(Map.of("resolved", true, "scored", true))))
                  .executionPolicy(new ExecutionPolicy(60000, new RetryPolicy(1, 100)))
                  .build())
          .bindings(
              Binding.builder()
                  .name("on-verify-all")
                  .capability(cap)
                  .on(new ContextChangeTrigger(".task == \"go\""))
                  .build())
          .build();
    }
  }

  @ApplicationScoped
  public static class ProducedKeysBean extends CaseHub {
    private final Capability cap =
        Capability.builder()
            .name("produce-keys")
            .inputSchema("{ task: .task }")
            .outputSchema(".")
            .build();

    @Override
    public CaseDefinition getDefinition() {
      return CaseDefinition.builder()
          .namespace("test-produced-keys")
          .name("Produced Keys Test")
          .version("1.0.0")
          .monitoring(MonitoringConfig.defaults())
          .capabilities(cap)
          .workers(
              Worker.builder()
                  .name("keys-worker")
                  .capabilityName("produce-keys")
                  .function(
                      new WorkerFunction.Sync<>(
                          Map.class,
                          Map.class,
                          (input, scope) -> WorkerResult.of(Map.of("dataA", "value"))))
                  .executionPolicy(new ExecutionPolicy(60000, new RetryPolicy(1, 100)))
                  .build())
          .bindings(
              Binding.builder()
                  .name("on-produce")
                  .capability(cap)
                  .producedKeys(Set.of("dataA", "dataB"))
                  .on(new ContextChangeTrigger(".task == \"go\""))
                  .build())
          .build();
    }
  }

  @ApplicationScoped
  public static class HighDivergenceBean extends CaseHub {
    private final Capability cap =
        Capability.builder()
            .name("diverge-data")
            .inputSchema("{ task: .task }")
            .outputSchema(".")
            .build();

    @Override
    public CaseDefinition getDefinition() {
      return CaseDefinition.builder()
          .namespace("test-high-divergence")
          .name("High Divergence Test")
          .version("1.0.0")
          .monitoring(MonitoringConfig.defaults())
          .goapActions(
              List.of(
                  new GoapAction(
                      "diverge-data", Map.of(), Map.of("resolved", true, "scored", true), 1.0)))
          .capabilities(cap)
          .workers(
              Worker.builder()
                  .name("diverge-worker")
                  .capabilityName("diverge-data")
                  .function(
                      new WorkerFunction.Sync<>(
                          Map.class,
                          Map.class,
                          (input, scope) -> WorkerResult.of(Map.of("other", "data"))))
                  .executionPolicy(new ExecutionPolicy(60000, new RetryPolicy(1, 100)))
                  .build())
          .bindings(
              Binding.builder()
                  .name("on-diverge")
                  .capability(cap)
                  .on(new ContextChangeTrigger(".task == \"go\""))
                  .build())
          .build();
    }
  }

  // ── Test infrastructure ──────────────────────────────────────────────────

  @ApplicationScoped
  public static class ViolationEventRecorder {
    static final List<ExpectationViolationEvent> events = new CopyOnWriteArrayList<>();

    @ConsumeEvent(EventBusAddresses.EXPECTATION_VIOLATED)
    public void onViolation(final ExpectationViolationEvent event) {
      events.add(event);
    }
  }

  public static class MemoryProfile implements QuarkusTestProfile {
    @Override
    public String getConfigProfile() {
      return "memory";
    }
  }
}
