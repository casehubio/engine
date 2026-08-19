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
import io.casehub.engine.plan.monitoring.MonitoringConfig;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CaseDefinitionYamlMapperMonitoringTest {

  private CaseDefinition load(String yaml) throws IOException {
    InputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    return CaseDefinitionYamlMapper.load(in);
  }

  @Test
  void parses_monitoring_block() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          monitoring:
            enabled: true
            perCompletionThreshold: 0.3
            windowSize: 10
        """;
    CaseDefinition def = load(yaml);
    MonitoringConfig config = def.getMonitoringConfig();
    assertThat(config).isNotNull();
    assertThat(config.enabled()).isTrue();
    assertThat(config.perCompletionThreshold()).isEqualTo(0.3);
    assertThat(config.windowSize()).isEqualTo(10);
  }

  @Test
  void absent_monitoring_block_returns_null() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec: {}
        """;
    CaseDefinition def = load(yaml);
    assertThat(def.getMonitoringConfig()).isNull();
  }

  @Test
  void monitoring_with_defaults_only() throws IOException {
    String yaml =
        """
        dsl: "0.1.0"
        namespace: test
        name: test-case
        version: "1.0.0"
        spec:
          monitoring:
            enabled: true
        """;
    CaseDefinition def = load(yaml);
    MonitoringConfig config = def.getMonitoringConfig();
    assertThat(config).isNotNull();
    assertThat(config.enabled()).isTrue();
    assertThat(config.perCompletionThreshold()).isEqualTo(MonitoringConfig.DEFAULT_THRESHOLD);
    assertThat(config.windowSize()).isEqualTo(MonitoringConfig.DEFAULT_WINDOW_SIZE);
  }
}
