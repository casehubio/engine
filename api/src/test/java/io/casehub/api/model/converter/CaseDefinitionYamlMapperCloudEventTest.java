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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.casehub.api.model.Binding;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.CloudEventTrigger;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CaseDefinitionYamlMapperCloudEventTest {

  @Test
  void load_cloudEventTrigger_stringForm() throws IOException {
    String yaml =
        """
        namespace: test
        name: ce-string
        version: 1.0.0
        dsl: 0.1.0
        spec:
          capabilities:
            - name: handle-event
          workers:
            - name: event-handler
              capabilities:
                - handle-event
          bindings:
            - name: on-document-received
              capability: handle-event
              on:
                cloudEvent: "document.received"
        """;

    CaseDefinition def =
        CaseDefinitionYamlMapper.load(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    assertThat(def.getBindings()).hasSize(1);
    Binding binding = def.getBindings().get(0);
    assertThat(binding.getOn()).isInstanceOf(CloudEventTrigger.class);
    CloudEventTrigger trigger = (CloudEventTrigger) binding.getOn();
    assertThat(trigger.getType()).isEqualTo("document.received");
    assertThat(trigger.getSource()).isNull();
    assertThat(trigger.getSubject()).isNull();
    assertThat(trigger.getFilter()).isNull();
  }

  @Test
  void load_cloudEventTrigger_objectWithTypeOnly() throws IOException {
    String yaml =
        """
        namespace: test
        name: ce-type-only
        version: 1.0.0
        dsl: 0.1.0
        spec:
          capabilities:
            - name: handle-event
          workers:
            - name: event-handler
              capabilities:
                - handle-event
          bindings:
            - name: on-document-received
              capability: handle-event
              on:
                cloudEvent:
                  type: "document.received"
        """;

    CaseDefinition def =
        CaseDefinitionYamlMapper.load(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    CloudEventTrigger trigger = (CloudEventTrigger) def.getBindings().get(0).getOn();
    assertThat(trigger.getType()).isEqualTo("document.received");
    assertThat(trigger.getSource()).isNull();
    assertThat(trigger.getSubject()).isNull();
    assertThat(trigger.getFilter()).isNull();
  }

  @Test
  void load_cloudEventTrigger_objectWithAllFields() throws IOException {
    String yaml =
        """
        namespace: test
        name: ce-full
        version: 1.0.0
        dsl: 0.1.0
        spec:
          capabilities:
            - name: handle-event
          workers:
            - name: event-handler
              capabilities:
                - handle-event
          bindings:
            - name: on-document-received
              capability: handle-event
              on:
                cloudEvent:
                  type: "document.received"
                  source: "upload-service"
                  subject: "invoices"
                  filter: '.data.size > 1024'
        """;

    CaseDefinition def =
        CaseDefinitionYamlMapper.load(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    CloudEventTrigger trigger = (CloudEventTrigger) def.getBindings().get(0).getOn();
    assertThat(trigger.getType()).isEqualTo("document.received");
    assertThat(trigger.getSource()).isEqualTo("upload-service");
    assertThat(trigger.getSubject()).isEqualTo("invoices");
    assertThat(trigger.getFilter()).isNotNull();
  }

  @Test
  void load_cloudEventTrigger_objectWithSourceOnly() throws IOException {
    String yaml =
        """
        namespace: test
        name: ce-source
        version: 1.0.0
        dsl: 0.1.0
        spec:
          capabilities:
            - name: handle-event
          workers:
            - name: event-handler
              capabilities:
                - handle-event
          bindings:
            - name: on-document-received
              capability: handle-event
              on:
                cloudEvent:
                  type: "document.received"
                  source: "upload-service"
        """;

    CaseDefinition def =
        CaseDefinitionYamlMapper.load(
            new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));

    CloudEventTrigger trigger = (CloudEventTrigger) def.getBindings().get(0).getOn();
    assertThat(trigger.getType()).isEqualTo("document.received");
    assertThat(trigger.getSource()).isEqualTo("upload-service");
    assertThat(trigger.getSubject()).isNull();
    assertThat(trigger.getFilter()).isNull();
  }

  @Test
  void load_cloudEventTrigger_objectMissingType_throws() {
    String yaml =
        """
        namespace: test
        name: ce-no-type
        version: 1.0.0
        dsl: 0.1.0
        spec:
          capabilities:
            - name: handle-event
          workers:
            - name: event-handler
              capabilities:
                - handle-event
          bindings:
            - name: on-document-received
              capability: handle-event
              on:
                cloudEvent:
                  source: "upload-service"
        """;

    assertThatThrownBy(
            () ->
                CaseDefinitionYamlMapper.load(
                    new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("type");
  }
}
