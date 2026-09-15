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
package io.casehub.engine.annotations.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.cbr.CbrConfig;
import io.casehub.api.model.cbr.JqFeatureExtractor;
import io.casehub.engine.annotations.Bind;
import io.casehub.engine.annotations.Case;
import io.casehub.engine.annotations.Cbr;
import io.casehub.engine.annotations.Feature;
import io.casehub.engine.annotations.Weight;
import io.casehub.engine.annotations.Worker;
import io.quarkus.test.QuarkusUnitTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CbrAnnotationTest {

  @RegisterExtension
  static final QuarkusUnitTest test =
      new QuarkusUnitTest()
          .withApplicationRoot(root -> root.addClasses(CbrCase.class, CbrCase.Result.class));

  @Case(namespace = "test", name = "CbrFull", version = "1.0.0")
  @Cbr(
      features = {
        @Feature(name = "severity", expression = ".incident.severity"),
        @Feature(name = "category", expression = ".incident.category")
      },
      weights = {@Weight(name = "severity", value = 0.7), @Weight(name = "category", value = 0.3)},
      domain = "test-domain",
      topK = 10,
      minSimilarity = 0.3,
      crossType = true,
      problemDescription = ".incident.summary",
      timing = "case-lifetime",
      temporalDecayHalfLifeDays = 30,
      minCostSamples = 5)
  public interface CbrCase {
    @Worker(capability = "analyse")
    @Bind(contextChange = ".incident != null")
    Result analyse();

    record Result(String status) {}
  }

  @Inject CaseDefinition definition;

  @Test
  void cbr_annotation_produces_cbrConfig() {
    CbrConfig config = definition.getCbrConfig();
    assertThat(config).isNotNull();
    assertThat(config.domain()).isEqualTo("test-domain");
    assertThat(config.topK()).isEqualTo(10);
    assertThat(config.minSimilarity()).isEqualTo(0.3);
    assertThat(config.crossType()).isTrue();
    assertThat(config.timing()).isEqualTo(CbrConfig.CbrRetrievalTiming.CASE_LIFETIME);
    assertThat(config.temporalDecayHalfLifeDays()).isEqualTo(30);
    assertThat(config.minCostSamples()).isEqualTo(5);
  }

  @Test
  void cbr_features_mapped_correctly() {
    CbrConfig config = definition.getCbrConfig();
    assertThat(config).isNotNull();
    assertThat(config.featureExtractor()).isInstanceOf(JqFeatureExtractor.class);
    JqFeatureExtractor jq = (JqFeatureExtractor) config.featureExtractor();
    assertThat(jq.featureExpressions()).containsEntry("severity", ".incident.severity");
    assertThat(jq.featureExpressions()).containsEntry("category", ".incident.category");
  }

  @Test
  void cbr_weights_mapped_correctly() {
    CbrConfig config = definition.getCbrConfig();
    assertThat(config).isNotNull();
    assertThat(config.weights()).containsEntry("severity", 0.7);
    assertThat(config.weights()).containsEntry("category", 0.3);
  }

  @Test
  void cbr_problemDescription_mapped() {
    CbrConfig config = definition.getCbrConfig();
    assertThat(config).isNotNull();
    assertThat(config.problemDescription()).isNotNull();
  }
}
