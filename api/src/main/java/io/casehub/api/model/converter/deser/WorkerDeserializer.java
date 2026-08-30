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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.casehub.platform.api.governance.ExecutionPolicy;
import io.casehub.worker.api.Worker;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

public class WorkerDeserializer extends StdDeserializer<Worker> {

  public WorkerDeserializer() {
    super(Worker.class);
  }

  @Override
  public Worker deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    JsonNode node = p.readValueAsTree();
    if (node == null || node.isNull()) {
      return null;
    }

    String name = node.has("name") ? node.get("name").asText() : null;

    Set<String> capabilities = new LinkedHashSet<>();
    JsonNode capsNode = node.get("capabilities");
    if (capsNode != null && capsNode.isArray()) {
      capsNode.forEach(n -> capabilities.add(n.asText()));
    }

    String description = node.has("description") ? node.get("description").asText() : null;
    String definitionRef = node.has("definitionRef") ? node.get("definitionRef").asText() : null;

    ExecutionPolicy executionPolicy = null;
    JsonNode epNode = node.get("executionPolicy");
    if (epNode != null && epNode.isObject()) {
      Integer timeoutMs = epNode.has("timeoutMs") ? epNode.get("timeoutMs").asInt() : null;
      io.casehub.platform.api.governance.RetryPolicy retries = null;
      JsonNode retriesNode = epNode.get("retries");
      if (retriesNode != null && retriesNode.isObject()) {
        Integer maxAttempts =
            retriesNode.has("maxAttempts") ? retriesNode.get("maxAttempts").asInt() : null;
        Integer delayMs = retriesNode.has("delayMs") ? retriesNode.get("delayMs").asInt() : null;
        io.casehub.platform.api.governance.BackoffStrategy backoff =
            retriesNode.has("backoffStrategy")
                ? io.casehub.platform.api.governance.BackoffStrategy.valueOf(
                    retriesNode.get("backoffStrategy").asText())
                : io.casehub.platform.api.governance.BackoffStrategy.FIXED;
        Integer maxDelayMs =
            retriesNode.has("maxDelayMs") ? retriesNode.get("maxDelayMs").asInt() : null;
        retries =
            new io.casehub.platform.api.governance.RetryPolicy(
                maxAttempts, delayMs, backoff, maxDelayMs);
      }
      executionPolicy =
          retries != null
              ? new ExecutionPolicy(timeoutMs, retries)
              : new ExecutionPolicy(
                  timeoutMs, new io.casehub.platform.api.governance.RetryPolicy());
    }

    Worker.Builder builder = Worker.builder().name(name).capabilityNames(capabilities).noFunction();
    if (description != null) {
      builder.description(description);
    }
    if (executionPolicy != null) {
      builder.executionPolicy(executionPolicy);
    }
    if (definitionRef != null) {
      builder.definitionRef(definitionRef);
    }
    return builder.build();
  }

  @Override
  public Worker getNullValue(DeserializationContext ctxt) {
    return null;
  }
}
