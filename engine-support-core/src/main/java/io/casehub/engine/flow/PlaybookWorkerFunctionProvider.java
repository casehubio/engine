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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.spi.WorkerFunctionProvider;
import io.casehub.platform.compiler.CompilationOutcome;
import io.casehub.platform.compiler.CompilationResult;
import io.casehub.platform.compiler.PlaybookCompiler;
import io.casehub.worker.api.WorkerFunction;
import java.util.Map;

public class PlaybookWorkerFunctionProvider implements WorkerFunctionProvider {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final PlaybookCompiler compiler = new PlaybookCompiler();

  @Override
  public boolean handles(final JsonNode rawWorkerNode) {
    return rawWorkerNode.has("steps");
  }

  @SuppressWarnings("unchecked")
  @Override
  public WorkerFunction<?, ?> create(final JsonNode rawWorkerNode) {
    try {
      Map<String, Object> content = MAPPER.convertValue(rawWorkerNode, Map.class);
      CompilationOutcome outcome = compiler.compile(content);
      if (outcome instanceof CompilationOutcome.Success success) {
        CompilationResult result = success.result();
        return new PlaybookWorkerFunction(result.workflow(), result);
      } else if (outcome instanceof CompilationOutcome.Failure failure) {
        throw new IllegalArgumentException("Failed to compile playbook: " + failure.diagnostics());
      }
      throw new IllegalArgumentException("Unknown compilation outcome: " + outcome);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to compile playbook definition from YAML", e);
    }
  }
}
