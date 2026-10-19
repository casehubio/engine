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
package io.casehub.engine.runtime.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.casehub.ras.api.SituationRegistrar;
import io.casehub.ras.api.SituationRegistration;
import jakarta.enterprise.inject.Instance;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SituationYamlRegistrarTest {

  @Mock Instance<SituationRegistrar> situationRegistrarInstance;
  @Mock SituationRegistrar situationRegistrar;
  @InjectMocks SituationYamlRegistrar registrar;

  @Test
  void sectionKey() {
    assertThat(registrar.sectionKey()).isEqualTo("situations");
  }

  @Test
  void classpathPaths() {
    assertThat(registrar.classpathPaths()).isEqualTo(Set.of("situations"));
  }

  @Test
  void priority() {
    assertThat(registrar.priority()).isEqualTo(300);
  }

  @Test
  void register_validYaml_registersSituation() {
    when(situationRegistrarInstance.isResolvable()).thenReturn(true);
    when(situationRegistrarInstance.get()).thenReturn(situationRegistrar);

    String yaml =
        """
            situationId: high-latency
            eventTypes:
              - health.metric
            correlationWindow: PT5M
            chainMode:
              type: or
              ganglia:
                - heartbeat-check
                - metrics-trend
            triggerAction:
              type: notify-only
            triggerMode:
              type: fire-once
            """;

    registrar.register("situations/high-latency.yaml", stream(yaml));

    var captor = ArgumentCaptor.forClass(SituationRegistration.class);
    verify(situationRegistrar).register(captor.capture());

    var def = captor.getValue().definition();
    assertThat(def.situationId()).isEqualTo("high-latency");
    assertThat(def.eventTypes()).containsExactly("health.metric");
    assertThat(def.correlationWindow()).hasMinutes(5);
  }

  @Test
  void register_withExpressions_parsesAsJQ() {
    when(situationRegistrarInstance.isResolvable()).thenReturn(true);
    when(situationRegistrarInstance.get()).thenReturn(situationRegistrar);

    String yaml =
        """
            situationId: filtered-events
            eventTypes:
              - ops.alert
            chainMode:
              type: or
              ganglia:
                - alert-detector
            triggerAction:
              type: notify-only
            correlationKeyExpression: ".data.serviceId"
            eventFilter: ".data.severity > 3"
            """;

    registrar.register("situations/filtered.yaml", stream(yaml));

    var captor = ArgumentCaptor.forClass(SituationRegistration.class);
    verify(situationRegistrar).register(captor.capture());

    var def = captor.getValue().definition();
    assertThat(def.correlationKeyExpression()).isNotNull();
    assertThat(def.eventFilter()).isNotNull();
  }

  @Test
  void register_invalidYaml_doesNotCrash() {
    registrar.register("situations/broken.yaml", stream("not: a: valid: yaml"));

    verify(situationRegistrar, never()).register(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void register_noSituationRegistrar_logsButDoesNotFail() {
    when(situationRegistrarInstance.isResolvable()).thenReturn(false);

    String yaml =
        """
        situationId: deferred-sit
        eventTypes:
          - test.event
        chainMode:
          type: or
          ganglia:
            - test-ganglion
        triggerAction:
          type: notify-only
        """;

    registrar.register("situations/deferred.yaml", stream(yaml));

    verify(situationRegistrar, never()).register(org.mockito.ArgumentMatchers.any());
  }

  private static ByteArrayInputStream stream(String content) {
    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
  }
}
