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
package io.casehub.engine.runtime.worker;

import com.fasterxml.jackson.databind.JsonNode;
import io.casehub.api.model.CaseDefinition;
import io.casehub.api.model.FailureCategory;
import io.casehub.api.model.FailureDiagnosis;
import io.casehub.platform.agent.AgentProvider;
import io.casehub.platform.agent.AgentSessionConfig;
import java.time.Duration;
import java.util.Optional;
import org.jboss.logging.Logger;

public class FailureCritiqueService {

  private static final Logger LOG = Logger.getLogger(FailureCritiqueService.class);

  private static final Duration LLM_TIMEOUT = Duration.ofSeconds(120);

  private static final String SYSTEM_PROMPT =
      "You are a failure analyst. Given a worker failure and the current case "
          + "context, produce a single sentence explaining what went wrong and what "
          + "should change on retry. Be specific and actionable.";

  private final Optional<AgentProvider> agentProvider;

  public FailureCritiqueService(Optional<AgentProvider> agentProvider) {
    this.agentProvider = agentProvider;
  }

  public String generateCritique(
      FailureDiagnosis diagnosis, JsonNode workingLayer, CaseDefinition definition) {

    if (!(diagnosis.category() instanceof FailureCategory.Knowledge)) {
      return diagnosis.category().reason();
    }

    if (agentProvider.isEmpty()) {
      return diagnosis.category().reason();
    }

    try {
      String userPrompt = buildUserPrompt(diagnosis, workingLayer);
      var config = AgentSessionConfig.of(SYSTEM_PROMPT, userPrompt);
      var invocationResult =
          io.casehub.api.model.ai.AgentInvocation.invoke(agentProvider.get(), config, LLM_TIMEOUT);
      var text = invocationResult.text();
      return text != null && !text.isBlank() ? text.trim() : diagnosis.category().reason();
    } catch (Exception e) {
      LOG.debugf(e, "LLM critique generation failed — using classical fallback");
      return diagnosis.category().reason();
    }
  }

  private String buildUserPrompt(FailureDiagnosis diagnosis, JsonNode workingLayer) {
    var sb = new StringBuilder();
    sb.append("Worker '").append(diagnosis.workerId()).append("' failed.\n");
    sb.append("Category: ").append(diagnosis.category().categoryName()).append("\n");
    sb.append("Reason: ").append(diagnosis.category().reason()).append("\n");
    if (diagnosis.category() instanceof FailureCategory.Knowledge k && k.missingContext() != null) {
      sb.append("Missing context: ").append(k.missingContext()).append("\n");
    }
    if (workingLayer != null) {
      String contextStr = workingLayer.toString();
      if (contextStr.length() > 1000) {
        contextStr = contextStr.substring(0, 1000) + "...";
      }
      sb.append("\nCurrent context:\n").append(contextStr);
    }
    return sb.toString();
  }
}
