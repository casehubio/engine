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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PlaybookTriageModificationParser {

  private static final String PREFIX = "playbook.";
  private static final String SKIP_STEPS = PREFIX + "skipSteps";
  private static final String REPLACE_INPUT_PREFIX = PREFIX + "replaceInput.";
  private static final String UPDATE_PARAMS_PREFIX = PREFIX + "updateParams.";

  private PlaybookTriageModificationParser() {}

  @SuppressWarnings("unchecked")
  public static List<PlaybookTriageModification> parse(Map<String, Object> modifications) {
    List<PlaybookTriageModification> result = new ArrayList<>();

    Object skipSteps = modifications.get(SKIP_STEPS);
    if (skipSteps instanceof List<?> stepIds) {
      for (Object id : stepIds) {
        result.add(new PlaybookTriageModification.SkipStep(id.toString()));
      }
    }

    for (var entry : modifications.entrySet()) {
      String key = entry.getKey();
      if (key.startsWith(REPLACE_INPUT_PREFIX) && entry.getValue() instanceof Map<?, ?> map) {
        String stepId = key.substring(REPLACE_INPUT_PREFIX.length());
        result.add(
            new PlaybookTriageModification.ReplaceStepInput(stepId, (Map<String, Object>) map));
      } else if (key.startsWith(UPDATE_PARAMS_PREFIX)) {
        String paramKey = key.substring(UPDATE_PARAMS_PREFIX.length());
        result.add(new PlaybookTriageModification.UpdateParameter(paramKey, entry.getValue()));
      }
    }

    return List.copyOf(result);
  }
}
