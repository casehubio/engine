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
package io.casehub.api.model.converter;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.CaseDefinition;
import io.casehub.eidos.api.AgentCapability;
import io.casehub.eidos.api.AgentConstraint;
import io.casehub.eidos.api.AgentDescriptor;
import io.casehub.eidos.api.AgentGoal;
import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.GoalPriority;
import io.casehub.eidos.api.Visibility;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CaseDefinitionYamlMapperAgentDescriptorTest {

  @Test
  void worker_with_full_agent_descriptor() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          workers:
            - name: risk-analyst
              capabilities: []
              agentDescriptor:
                slot: risk-analysis
                briefing: "You are a risk analyst specializing in financial risk assessment."
                provider: openai
                modelFamily: gpt-4
                modelVersion: "2025-06"
                goals:
                  - name: assess-risk
                    description: "Evaluate risk factors for the given case"
                    priority: PRIMARY
                  - name: recommend-mitigation
                    description: "Suggest risk mitigation strategies"
                    priority: SECONDARY
                    visibility: PRIVATE
                    capabilities:
                      - risk-scoring
                constraints:
                  - name: no-pii
                    description: "Do not include PII in outputs"
                    severity: HARD
                  - name: prefer-conservative
                    description: "Prefer conservative risk estimates"
                    severity: SOFT
                    visibility: PRIVATE
                disposition:
                  socialOrient: cooperative
                  ruleFollowing: strict
                  riskAppetite: cautious
                  autonomy: guided
                  conflictMode: accommodating
                  delegation: false
                capabilities:
                  - name: risk-scoring
                    description: "Score risks on a 1-10 scale"
                    qualityHint: 0.9
                    tags:
                      - risk
                      - scoring
                  - name: report-generation
                    description: "Generate risk reports"
        """;
    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    CaseDefinition def = CaseDefinitionYamlMapper.load(is);

    assertThat(def.agentDescriptorFor("risk-analyst")).isPresent();
    AgentDescriptor desc = def.agentDescriptorFor("risk-analyst").get();

    assertThat(desc.agentId()).isEqualTo("risk-analyst");
    assertThat(desc.name()).isEqualTo("risk-analyst");
    assertThat(desc.slot()).isEqualTo("risk-analysis");
    assertThat(desc.tenancyId()).isEqualTo("default");
    assertThat(desc.briefing())
        .isEqualTo("You are a risk analyst specializing in financial risk assessment.");
    assertThat(desc.provider()).isEqualTo("openai");
    assertThat(desc.modelFamily()).isEqualTo("gpt-4");
    assertThat(desc.modelVersion()).isEqualTo("2025-06");

    assertThat(desc.goals()).hasSize(2);
    AgentGoal g1 = desc.goals().get(0);
    assertThat(g1.name()).isEqualTo("assess-risk");
    assertThat(g1.description()).isEqualTo("Evaluate risk factors for the given case");
    assertThat(g1.priority()).isEqualTo(GoalPriority.PRIMARY);
    assertThat(g1.visibility()).isEqualTo(Visibility.PUBLIC);

    AgentGoal g2 = desc.goals().get(1);
    assertThat(g2.name()).isEqualTo("recommend-mitigation");
    assertThat(g2.priority()).isEqualTo(GoalPriority.SECONDARY);
    assertThat(g2.visibility()).isEqualTo(Visibility.PRIVATE);
    assertThat(g2.capabilities()).containsExactly("risk-scoring");

    assertThat(desc.constraints()).hasSize(2);
    AgentConstraint c1 = desc.constraints().get(0);
    assertThat(c1.name()).isEqualTo("no-pii");
    assertThat(c1.severity()).isEqualTo(ConstraintSeverity.HARD);
    assertThat(c1.visibility()).isEqualTo(Visibility.PUBLIC);

    AgentConstraint c2 = desc.constraints().get(1);
    assertThat(c2.name()).isEqualTo("prefer-conservative");
    assertThat(c2.severity()).isEqualTo(ConstraintSeverity.SOFT);
    assertThat(c2.visibility()).isEqualTo(Visibility.PRIVATE);

    assertThat(desc.disposition()).isNotNull();
    assertThat(desc.disposition().delegation()).isFalse();

    assertThat(desc.capabilities()).hasSize(2);
    AgentCapability cap1 = desc.capabilities().get(0);
    assertThat(cap1.name()).isEqualTo("risk-scoring");
    assertThat(cap1.description()).isEqualTo("Score risks on a 1-10 scale");
    assertThat(cap1.qualityHint()).isEqualTo(0.9);
    assertThat(cap1.tags()).containsExactly("risk", "scoring");
  }

  @Test
  void worker_with_minimal_agent_descriptor() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          workers:
            - name: simple-agent
              capabilities: []
              agentDescriptor:
                briefing: "A simple agent."
        """;
    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    CaseDefinition def = CaseDefinitionYamlMapper.load(is);

    assertThat(def.agentDescriptorFor("simple-agent")).isPresent();
    AgentDescriptor desc = def.agentDescriptorFor("simple-agent").get();

    assertThat(desc.agentId()).isEqualTo("simple-agent");
    assertThat(desc.name()).isEqualTo("simple-agent");
    assertThat(desc.slot()).isEqualTo("simple-agent");
    assertThat(desc.tenancyId()).isEqualTo("default");
    assertThat(desc.briefing()).isEqualTo("A simple agent.");
    assertThat(desc.goals()).isNullOrEmpty();
    assertThat(desc.constraints()).isNullOrEmpty();
    assertThat(desc.capabilities()).isNullOrEmpty();
  }

  @Test
  void multiple_workers_with_agent_descriptors() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          workers:
            - name: analyst
              capabilities: []
              agentDescriptor:
                briefing: "Financial analyst"
                goals:
                  - name: analyze
                    description: "Analyze data"
            - name: reviewer
              capabilities: []
              agentDescriptor:
                briefing: "Code reviewer"
                goals:
                  - name: review
                    description: "Review code"
            - name: plain-worker
              capabilities: []
        """;
    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    CaseDefinition def = CaseDefinitionYamlMapper.load(is);

    assertThat(def.agentDescriptorFor("analyst")).isPresent();
    assertThat(def.agentDescriptorFor("reviewer")).isPresent();
    assertThat(def.agentDescriptorFor("plain-worker")).isEmpty();

    assertThat(def.agentDescriptorFor("analyst").get().briefing()).isEqualTo("Financial analyst");
    assertThat(def.agentDescriptorFor("reviewer").get().briefing()).isEqualTo("Code reviewer");
  }

  @Test
  void worker_without_agent_descriptor_returns_empty() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          workers:
            - name: plain-worker
              capabilities: []
        """;
    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    CaseDefinition def = CaseDefinitionYamlMapper.load(is);

    assertThat(def.agentDescriptorFor("plain-worker")).isEmpty();
  }

  @Test
  void agent_descriptor_with_custom_agent_id_and_slot() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          workers:
            - name: my-worker
              capabilities: []
              agentDescriptor:
                agentId: custom-agent-id
                name: Custom Agent Name
                slot: custom-slot
                tenancyId: tenant-42
                briefing: "Custom IDs"
        """;
    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    CaseDefinition def = CaseDefinitionYamlMapper.load(is);

    AgentDescriptor desc = def.agentDescriptorFor("my-worker").get();
    assertThat(desc.agentId()).isEqualTo("custom-agent-id");
    assertThat(desc.name()).isEqualTo("Custom Agent Name");
    assertThat(desc.slot()).isEqualTo("custom-slot");
    assertThat(desc.tenancyId()).isEqualTo("tenant-42");
  }
}
