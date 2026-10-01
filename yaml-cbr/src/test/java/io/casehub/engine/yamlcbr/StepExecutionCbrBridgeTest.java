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
package io.casehub.engine.yamlcbr;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.cbr.CbrConfig;
import io.casehub.api.spi.YamlStepExecutionEvent;
import io.casehub.engine.common.spi.CaseDefinitionRegistry;
import io.casehub.neocortex.memory.EraseRequest;
import io.casehub.neocortex.memory.MemoryDomain;
import io.casehub.neocortex.memory.cbr.CbrFilter;
import io.casehub.neocortex.memory.cbr.CbrMatch;
import io.casehub.neocortex.memory.cbr.CbrOutcome;
import io.casehub.neocortex.memory.cbr.CbrPlanRecord;
import io.casehub.neocortex.memory.cbr.CbrQuery;
import io.casehub.neocortex.memory.cbr.CbrRecord;
import io.casehub.neocortex.memory.cbr.CbrRecordSchema;
import io.casehub.neocortex.memory.cbr.CbrRecordStore;
import io.casehub.neocortex.memory.cbr.CbrRetentionPolicy;
import io.casehub.neocortex.memory.cbr.FeatureValue;
import io.casehub.neocortex.memory.cbr.SupersessionStatus;
import io.casehub.platform.api.path.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StepExecutionCbrBridgeTest {

  private RecordingCbrStore cbrStore;
  private StubRegistry registry;
  private StepExecutionCbrBridge bridge;

  @BeforeEach
  void setUp() {
    cbrStore = new RecordingCbrStore();
    registry = new StubRegistry();
    bridge = new StepExecutionCbrBridge(cbrStore, registry, List.of());
  }

  @Test
  void recordsStepExecutionWhenCbrConfigured() {
    var config =
        CbrConfig.builder().feature("severity", ".incident.severity").domain("test").build();
    var definition =
        CaseDefinition.builder()
            .namespace("ns")
            .name("test-case")
            .version("1.0.0")
            .title("Test")
            .summary("Test case")
            .cbrConfig(config)
            .build();
    registry.register(definition);

    var event =
        new YamlStepExecutionEvent(
            UUID.randomUUID(),
            "tenant-1",
            "test-case",
            "rest-call",
            200L,
            true,
            Map.of("status", 200),
            "rest",
            "SUCCESS",
            null,
            null,
            "playbook-v1",
            "1.0.0",
            null,
            Map.of("incident", Map.of("severity", "high")));

    bridge.onYamlStepExecution(event);

    assertEquals(1, cbrStore.stored.size());
    CbrRecord stored = cbrStore.stored.get(0).record;
    assertInstanceOf(CbrPlanRecord.class, stored);
    assertFalse(stored.features().isEmpty());
  }

  @Test
  void skipsWhenNoCbrConfig() {
    var definition =
        CaseDefinition.builder()
            .namespace("ns")
            .name("no-cbr")
            .version("1.0.0")
            .title("No CBR")
            .summary("No CBR")
            .build();
    registry.register(definition);

    var event =
        new YamlStepExecutionEvent(
            UUID.randomUUID(),
            "tenant-1",
            "no-cbr",
            "action",
            10L,
            true,
            Map.of(),
            "process",
            "SUCCESS",
            null,
            null,
            null,
            null,
            null,
            Map.of());

    bridge.onYamlStepExecution(event);

    assertTrue(cbrStore.stored.isEmpty());
  }

  @Test
  void skipsWhenCaseTypeNotRegistered() {
    var event =
        new YamlStepExecutionEvent(
            UUID.randomUUID(),
            "tenant-1",
            "unknown-case",
            "action",
            10L,
            true,
            Map.of(),
            "process",
            "SUCCESS",
            null,
            null,
            null,
            null,
            null,
            Map.of());

    bridge.onYamlStepExecution(event);

    assertTrue(cbrStore.stored.isEmpty());
  }

  @Test
  void usesPlaybookContextExtractorWhenAvailable() {
    var config = CbrConfig.builder().feature("f", ".x").domain("test").build();
    var definition =
        CaseDefinition.builder()
            .namespace("ns")
            .name("with-extractor")
            .version("1.0.0")
            .title("T")
            .summary("S")
            .cbrConfig(config)
            .build();
    registry.register(definition);

    PlaybookContextExtractor extractor =
        new PlaybookContextExtractor() {
          @Override
          public boolean supports(CbrConfig c) {
            return "test".equals(c.domain());
          }

          @Override
          public Map<String, FeatureValue> extract(YamlStepExecutionEvent e) {
            return Map.of("custom-feature", FeatureValue.of("custom-value"));
          }
        };

    bridge = new StepExecutionCbrBridge(cbrStore, registry, List.of(extractor));

    var event =
        new YamlStepExecutionEvent(
            UUID.randomUUID(),
            "tenant-1",
            "with-extractor",
            "action",
            10L,
            true,
            Map.of(),
            "process",
            "SUCCESS",
            null,
            null,
            null,
            null,
            null,
            Map.of());

    bridge.onYamlStepExecution(event);

    assertEquals(1, cbrStore.stored.size());
    assertTrue(cbrStore.stored.get(0).record.features().containsKey("custom-feature"));
  }

  @Test
  void continuesWhenStoreThrows() {
    cbrStore.shouldThrow = true;
    var config = CbrConfig.builder().feature("f", ".x").domain("test").build();
    var definition =
        CaseDefinition.builder()
            .namespace("ns")
            .name("throw-case")
            .version("1.0.0")
            .title("T")
            .summary("S")
            .cbrConfig(config)
            .build();
    registry.register(definition);

    var event =
        new YamlStepExecutionEvent(
            UUID.randomUUID(),
            "tenant-1",
            "throw-case",
            "action",
            10L,
            true,
            Map.of(),
            "process",
            "SUCCESS",
            null,
            null,
            null,
            null,
            null,
            Map.of());

    assertDoesNotThrow(() -> bridge.onYamlStepExecution(event));
  }

  @Test
  void includesGenericFeaturesAlways() {
    var config = CbrConfig.builder().feature("f", ".x").domain("test").build();
    var definition =
        CaseDefinition.builder()
            .namespace("ns")
            .name("generic-test")
            .version("1.0.0")
            .title("T")
            .summary("S")
            .cbrConfig(config)
            .build();
    registry.register(definition);

    var event =
        new YamlStepExecutionEvent(
            UUID.randomUUID(),
            "tenant-1",
            "generic-test",
            "rest-call",
            150L,
            true,
            Map.of(),
            "rest",
            "SUCCESS",
            "prod",
            null,
            "my-playbook",
            "2.0",
            null,
            Map.of());

    bridge.onYamlStepExecution(event);

    var features = cbrStore.stored.get(0).record.features();
    assertEquals(FeatureValue.of("rest-call"), features.get("actionName"));
    assertEquals(FeatureValue.of(150L), features.get("durationMs"));
    assertEquals(FeatureValue.of("rest"), features.get("bindingType"));
    assertEquals(FeatureValue.of(true), features.get("success"));
    assertEquals(FeatureValue.of("prod"), features.get("executionEnvironment"));
    assertEquals(FeatureValue.of("my-playbook"), features.get("playbookName"));
    assertEquals(FeatureValue.of("2.0"), features.get("playbookVersion"));
  }

  // --- Test doubles ---

  static class RecordingCbrStore implements CbrRecordStore {
    record StoredEntry(
        CbrRecord record, String caseType, String entityId, MemoryDomain domain, String tenantId) {}

    final List<StoredEntry> stored = new ArrayList<>();
    boolean shouldThrow = false;

    @Override
    public String store(
        CbrRecord cbrRecord,
        String caseType,
        String entityId,
        MemoryDomain domain,
        String tenantId,
        String caseId,
        Path scope) {
      if (shouldThrow) throw new RuntimeException("store failed");
      stored.add(new StoredEntry(cbrRecord, caseType, entityId, domain, tenantId));
      return "cbr-id-" + stored.size();
    }

    @Override
    public void registerSchema(CbrRecordSchema s) {}

    @Override
    public Integer erase(EraseRequest r) {
      return 0;
    }

    @Override
    public Integer eraseEntity(String entityId, String tenantId) {
      return 0;
    }

    @Override
    public Integer eraseByScope(Path scope, String tenantId) {
      return 0;
    }

    @Override
    public <C extends CbrRecord> List<CbrMatch<C>> retrieveSimilar(
        CbrQuery query, Class<C> caseType) {
      return List.of();
    }

    @Override
    public List<String> findCaseIds(
        String tenantId, MemoryDomain domain, String caseType, Map<String, CbrFilter> filters) {
      return List.of();
    }

    @Override
    public boolean supersede(
        String caseId, String tenantId, String supersedingCaseId, String reason) {
      return false;
    }

    @Override
    public boolean reinstate(String caseId, String tenantId) {
      return false;
    }

    @Override
    public SupersessionStatus getSupersessionStatus(String caseId, String tenantId) {
      return null;
    }

    @Override
    public List<SupersessionStatus> findSupersededCases(String tenantId, MemoryDomain domain) {
      return List.of();
    }

    @Override
    public void recordOutcome(String caseId, String tenantId, CbrOutcome outcome) {}

    @Override
    public int supersedeMatching(
        String tenantId,
        MemoryDomain domain,
        String caseType,
        Map<String, CbrFilter> filters,
        String reason) {
      return 0;
    }

    @Override
    public int supersedeAll(java.util.Collection<String> caseIds, String tenantId, String reason) {
      return 0;
    }

    @Override
    public int reinstateMatching(
        String tenantId, MemoryDomain domain, String caseType, Map<String, CbrFilter> filters) {
      return 0;
    }

    @Override
    public int reinstateAll(java.util.Collection<String> caseIds, String tenantId) {
      return 0;
    }

    @Override
    public Integer purge(CbrRetentionPolicy policy) {
      return 0;
    }
  }

  static class StubRegistry implements CaseDefinitionRegistry {
    private final Map<String, CaseDefinition> definitions = new HashMap<>();

    void register(CaseDefinition def) {
      definitions.put(def.getName(), def);
    }

    @Override
    public io.casehub.engine.common.internal.model.CaseMetaModel registerCaseDefinition(
        CaseDefinition model) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CaseDefinition getCaseDefinition(
        io.casehub.engine.common.internal.model.CaseMetaModel definition) {
      throw new UnsupportedOperationException();
    }

    @Override
    public io.casehub.engine.common.internal.model.CaseMetaModel getCaseMetaModel(
        CaseDefinition caseDefinition) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<CaseDefinition> findByName(String name) {
      return Optional.ofNullable(definitions.get(name));
    }
  }
}
