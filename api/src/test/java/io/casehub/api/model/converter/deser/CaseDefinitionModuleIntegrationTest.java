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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.api.model.AdaptationConfig;
import io.casehub.api.model.CapabilityTarget;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.GoalBasedCompletion;
import io.casehub.api.model.MemoryRetrievalConfig;
import io.casehub.api.model.RecoveryPolicy;
import io.casehub.api.model.ReflectionTriggerConfig;
import io.casehub.api.model.cbr.CbrConfig;
import io.casehub.api.model.converter.CaseDefinitionModule;
import io.casehub.api.spi.QuorumConfig;
import io.casehub.engine.plan.monitoring.MonitoringConfig;
import org.junit.jupiter.api.Test;

class CaseDefinitionModuleIntegrationTest {

  private final ObjectMapper yamlMapper =
      new ObjectMapper(new YAMLFactory()).registerModule(new CaseDefinitionModule(null));

  @Test
  void minimalDefinition_deserializes() throws Exception {
    String yaml =
        """
        namespace: test
        name: minimal
        version: "1.0.0"
        spec:
          capabilities:
            - name: process
          workers:
            - name: worker-1
              capabilities: [process]
          bindings:
            - name: trigger
              capability: process
              on:
                contextChange: {}
        """;

    CaseDefinition result = yamlMapper.readValue(yaml, CaseDefinition.class);

    assertEquals("test", result.getNamespace());
    assertEquals("minimal", result.getName());
    assertEquals("1.0.0", result.getVersion());
    assertEquals(1, result.getCapabilities().size());
    assertEquals("process", result.getCapabilities().get(0).name());
    assertEquals(1, result.getWorkers().size());
    assertEquals("worker-1", result.getWorkers().get(0).name());
    assertEquals(1, result.getBindings().size());
    assertEquals("trigger", result.getBindings().get(0).getName());
  }

  @Test
  void bindingResolvesCapabilityTarget() throws Exception {
    String yaml =
        """
        namespace: test
        name: resolved
        version: "1.0.0"
        spec:
          capabilities:
            - name: analyse
              inputProjection: ".data"
          workers:
            - name: analyst
              capabilities: [analyse]
          bindings:
            - name: do-analysis
              capability: analyse
              on:
                contextChange: {}
        """;

    CaseDefinition result = yamlMapper.readValue(yaml, CaseDefinition.class);

    CapabilityTarget ct = (CapabilityTarget) result.getBindings().get(0).target();
    assertEquals("analyse", ct.capability().name());
    assertEquals(".data", ct.capability().inputProjection());
  }

  @Test
  void identityFields_deserialize() throws Exception {
    String yaml =
        """
        namespace: org.example
        name: full-identity
        version: "2.0.0"
        dsl: "0.1.0"
        title: Full Identity Test
        summary: Tests all identity fields
        spec:
          capabilities:
            - name: cap
          workers:
            - name: w
              capabilities: [cap]
          bindings:
            - name: b
              capability: cap
              on:
                contextChange: {}
        """;

    CaseDefinition result = yamlMapper.readValue(yaml, CaseDefinition.class);

    assertEquals("org.example", result.getNamespace());
    assertEquals("full-identity", result.getName());
    assertEquals("2.0.0", result.getVersion());
    assertEquals("0.1.0", result.getDsl());
    assertEquals("Full Identity Test", result.getTitle());
    assertEquals("Tests all identity fields", result.getSummary());
  }

  @Test
  void goalsAndCompletion_deserialize() throws Exception {
    String yaml =
        """
        namespace: test
        name: with-goals
        version: "1.0.0"
        spec:
          capabilities:
            - name: analyse
          workers:
            - name: analyst
              capabilities: [analyse]
          bindings:
            - name: do-analysis
              capability: analyse
              on:
                contextChange: {}
          goals:
            - name: analysed
              condition: ".analysis != null"
          completion:
            success: analysed
        """;

    CaseDefinition result = yamlMapper.readValue(yaml, CaseDefinition.class);

    assertEquals(1, result.getGoals().size());
    assertEquals("analysed", result.getGoals().get(0).getName());
    assertNotNull(result.getGoals().get(0).getCondition());
    assertNotNull(result.getCompletion());
    assertInstanceOf(GoalBasedCompletion.class, result.getCompletion());
  }

  @Test
  void contextStoreFactory_deserializes() throws Exception {
    String yaml =
        """
        namespace: test
        name: ctx
        version: "1.0.0"
        context:
          storeFactory: auditing
        spec:
          capabilities:
            - name: cap
          workers:
            - name: w
              capabilities: [cap]
          bindings:
            - name: b
              capability: cap
              on:
                contextChange: {}
        """;

    CaseDefinition result = yamlMapper.readValue(yaml, CaseDefinition.class);
    assertEquals("auditing", result.getContextStoreFactory());
  }

  @Test
  void specStringFields_deserialize() throws Exception {
    String yaml =
        """
        namespace: test
        name: strategies
        version: "1.0.0"
        spec:
          capabilities:
            - name: cap
          workers:
            - name: w
              capabilities: [cap]
          bindings:
            - name: b
              capability: cap
              on:
                contextChange: {}
          planningStrategy: sequential
          decompositionStrategy: goap
          agentRouting: composable
        """;

    CaseDefinition result = yamlMapper.readValue(yaml, CaseDefinition.class);
    assertEquals("sequential", result.getPlanningStrategy());
    assertEquals("goap", result.getDecompositionStrategy());
    assertEquals("composable", result.getAgentRouting());
  }

  @Test
  void milestones_deserialize() throws Exception {
    String yaml =
        """
        namespace: test
        name: with-milestones
        version: "1.0.0"
        spec:
          capabilities:
            - name: cap
          workers:
            - name: w
              capabilities: [cap]
          bindings:
            - name: b
              capability: cap
              on:
                contextChange: {}
          milestones:
            - name: data-received
              entryCriteria: ".data != null"
              completionCriteria: ".data.processed == true"
        """;

    CaseDefinition result = yamlMapper.readValue(yaml, CaseDefinition.class);
    assertEquals(1, result.getMilestones().size());
    assertEquals("data-received", result.getMilestones().get(0).getName());
    assertNotNull(result.getMilestones().get(0).getEntryCriteria());
  }

  @Test
  void cbrConfig_deserializes() throws Exception {
    String yaml =
        """
        namespace: test
        name: with-cbr
        version: "1.0.0"
        spec:
          capabilities:
            - name: cap
          workers:
            - name: w
              capabilities: [cap]
          bindings:
            - name: b
              capability: cap
              on:
                contextChange: {}
          cbr:
            features:
              amount: ".amount"
            topK: 3
            domain: aml
        """;

    CaseDefinition result = yamlMapper.readValue(yaml, CaseDefinition.class);

    CbrConfig cbr = result.getCbrConfig();
    assertNotNull(cbr);
    assertEquals(3, cbr.topK());
    assertEquals("aml", cbr.domain());
  }

  @Test
  void adaptationConfig_objectForm_deserializes() throws Exception {
    String yaml =
        """
        namespace: test
        name: with-adaptation
        version: "1.0.0"
        spec:
          capabilities:
            - name: cap
          workers:
            - name: w
              capabilities: [cap]
          bindings:
            - name: b
              capability: cap
              on:
                contextChange: {}
          adaptation:
            trigger: on-failure
            optimization: forward-replan
            threshold: 0.4
        """;

    CaseDefinition result = yamlMapper.readValue(yaml, CaseDefinition.class);

    AdaptationConfig adaptation = result.getAdaptationConfig();
    assertNotNull(adaptation);
    assertEquals("on-failure", adaptation.trigger());
    assertEquals("forward-replan", adaptation.optimization());
    assertEquals(0.4, adaptation.threshold(), 0.001);
  }

  @Test
  void adaptationConfig_presetForm_deserializes() throws Exception {
    String yaml =
        """
        namespace: test
        name: with-adaptation-preset
        version: "1.0.0"
        spec:
          capabilities:
            - name: cap
          workers:
            - name: w
              capabilities: [cap]
          bindings:
            - name: b
              capability: cap
              on:
                contextChange: {}
          adaptation: adaptive
        """;

    CaseDefinition result = yamlMapper.readValue(yaml, CaseDefinition.class);

    AdaptationConfig adaptation = result.getAdaptationConfig();
    assertNotNull(adaptation);
    assertEquals("every-step", adaptation.trigger());
    assertEquals("forward-replan", adaptation.optimization());
    assertNull(adaptation.threshold());
  }

  @Test
  void configRecords_deserialize() throws Exception {
    String yaml =
        """
        namespace: test
        name: with-configs
        version: "1.0.0"
        spec:
          capabilities:
            - name: cap
          workers:
            - name: w
              capabilities: [cap]
          bindings:
            - name: b
              capability: cap
              on:
                contextChange: {}
          recoveryPolicy:
            maxRetries: 2
            maxRerouteAttempts: 2
            classifierId: heuristic
            enabled: true
          monitoring:
            enabled: true
            perCompletionThreshold: 0.6
            windowSize: 3
          reflection:
            enabled: true
            importanceThreshold: 4.0
            maxUnreflectedOutcomes: 5
            maxSourceMemories: 20
          memoryRetrieval:
            enabled: true
            maxMemories: 15
          quorum:
            instances: 3
            required: 2
        """;

    CaseDefinition result = yamlMapper.readValue(yaml, CaseDefinition.class);

    RecoveryPolicy rp = result.getRecoveryPolicy();
    assertNotNull(rp);
    assertEquals(2, rp.maxRetries());
    assertTrue(rp.enabled());

    MonitoringConfig mc = result.getMonitoringConfig();
    assertNotNull(mc);
    assertTrue(mc.enabled());
    assertEquals(0.6, mc.perCompletionThreshold(), 0.001);
    assertEquals(3, mc.windowSize());

    ReflectionTriggerConfig rtc = result.getReflectionTrigger();
    assertNotNull(rtc);
    assertTrue(rtc.enabled());
    assertEquals(4.0, rtc.importanceThreshold(), 0.001);

    MemoryRetrievalConfig mrc = result.getMemoryRetrieval();
    assertNotNull(mrc);
    assertTrue(mrc.enabled());
    assertEquals(15, mrc.maxMemories());

    QuorumConfig qc = result.getDefaultQuorum();
    assertNotNull(qc);
    assertEquals(3, qc.instances());
    assertEquals(2, qc.required());
  }
}
