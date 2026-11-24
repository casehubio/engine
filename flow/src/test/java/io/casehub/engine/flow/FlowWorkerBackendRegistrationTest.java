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
package io.casehub.engine.flow;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.converter.CaseDefinitionYamlMapper;
import io.casehub.api.spi.WorkerFunctionProviderRegistry;
import io.casehub.worker.api.Worker;
import io.casehub.worker.api.WorkerFunction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FlowWorkerBackendRegistrationTest {

  @Inject WorkerFunctionProviderRegistry providerRegistry;
  @Inject ObjectMapper objectMapper;

  @Test
  void providerRegistry_createsFlowFunction_forDoBlock() throws Exception {
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    JsonNode rawWorker =
        yamlMapper.readTree(
            """
            do:
              - sendNotification:
                  call: http
                  with:
                    method: POST
                    endpoint:
                      uri: https://example.com/notify
            """);

    WorkerFunction<?, ?> fn = providerRegistry.createFunction(rawWorker);

    assertThat(fn)
        .as("WorkerFunctionProviderRegistry should produce a FlowWorkerFunction for do: blocks")
        .isNotNull()
        .isInstanceOf(FlowWorkerFunction.class);
  }

  @Test
  void workerWithDoBlock_getsFlowWorkerFunction_afterYamlLoad() throws Exception {
    ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    JsonNode root =
        yamlMapper.readTree(
            """
            name: test-flow-case
            namespace: test
            version: "1.0"
            workers:
              - name: notifier
                capabilities:
                  - notify-customer
                do:
                  - sendNotification:
                      call: http
                      with:
                        method: POST
                        endpoint:
                          uri: https://example.com/notify
            """);

    CaseDefinition definition =
        CaseDefinitionYamlMapper.load(root, objectMapper, null, providerRegistry, null);

    Worker notifier =
        definition.getWorkers().stream()
            .filter(w -> w.name().equals("notifier"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Worker 'notifier' not found"));

    assertThat(notifier.function())
        .as("Worker with do: block should have a FlowWorkerFunction, not NONE")
        .isNotEqualTo(WorkerFunction.NONE)
        .isInstanceOf(FlowWorkerFunction.class);
  }
}
