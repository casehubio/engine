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

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.casehub.api.engine.CaseHub;
import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.JudgmentTarget;
import io.casehub.engine.common.spi.JudgmentScheduleRequest;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class JudgmentTargetDispatchTest {

  @Inject JudgmentDispatchCaseBean caseBean;

  @BeforeEach
  void reset() {
    RecordingJudgmentScheduler.events.clear();
  }

  @Test
  void judgmentBinding_dispatchesToScheduler_withCorrectFields() {
    UUID caseId = caseBean.startCase(Map.of("transaction", Map.of("amount", 50000)));

    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(() -> assertThat(RecordingJudgmentScheduler.events).isNotEmpty());

    JudgmentScheduleRequest request = RecordingJudgmentScheduler.events.get(0);
    assertThat(request.caseId()).isEqualTo(caseId);
    assertThat(request.bindingName()).isEqualTo("risk-judgment");
    assertThat(request.target().prompt()).isEqualTo("Assess the risk level");
    assertThat(request.inputData()).containsKey("amount");
    assertThat(request.target().evidenceRequirements()).containsExactly("riskScore", "rationale");
  }

  @ApplicationScoped
  static class JudgmentDispatchCaseBean extends CaseHub {

    @Override
    public CaseDefinition getDefinition() {
      return CaseDefinition.builder()
          .namespace("test")
          .name("judgment-dispatch-test")
          .version("1.0.0")
          .bindings(
              Binding.builder()
                  .name("risk-judgment")
                  .judgment(
                      JudgmentTarget.builder()
                          .prompt("Assess the risk level")
                          .inputMapping(".transaction")
                          .expiresIn(Duration.ofHours(1))
                          .evidenceRequirements(List.of("riskScore", "rationale"))
                          .build())
                  .on(new ContextChangeTrigger(".transaction != null"))
                  .build())
          .build();
    }
  }
}
