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
package io.casehub.api.model.stigmergy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RegressionVerdictTest {

  @Test
  void noRegressionIsInstanceOfVerdict() {
    RegressionVerdict verdict = new RegressionVerdict.NoRegression();
    assertThat(verdict).isInstanceOf(RegressionVerdict.class);
  }

  @Test
  void detectedCarriesConfidenceAndReason() {
    var detected = new RegressionVerdict.Detected(0.85, "health score dropped");
    assertThat(detected.confidence()).isEqualTo(0.85);
    assertThat(detected.reason()).isEqualTo("health score dropped");
  }

  @Test
  void patternMatchExhaustive() {
    RegressionVerdict verdict = new RegressionVerdict.Detected(0.5, "test");
    String result =
        switch (verdict) {
          case RegressionVerdict.NoRegression nr -> "none";
          case RegressionVerdict.Detected d -> "detected:" + d.confidence();
        };
    assertThat(result).isEqualTo("detected:0.5");
  }
}
