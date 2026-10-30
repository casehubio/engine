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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

  public static List<PlaybookTriageModification> mergeAll(List<Map<String, Object>> rounds) {
    if (rounds.isEmpty()) {
      return List.of();
    }

    Set<String> skippedSteps = new LinkedHashSet<>();
    Map<String, Map<String, Object>> replacedInputs = new LinkedHashMap<>();
    Map<String, Object> updatedParams = new LinkedHashMap<>();

    for (Map<String, Object> round : rounds) {
      for (PlaybookTriageModification mod : parse(round)) {
        switch (mod) {
          case PlaybookTriageModification.SkipStep s -> skippedSteps.add(s.stepId());
          case PlaybookTriageModification.ReplaceStepInput r ->
              replacedInputs.put(r.stepId(), r.newInput());
          case PlaybookTriageModification.UpdateParameter u ->
              updatedParams.put(u.key(), u.value());
        }
      }
    }

    List<PlaybookTriageModification> merged = new ArrayList<>();
    skippedSteps.forEach(id -> merged.add(new PlaybookTriageModification.SkipStep(id)));
    replacedInputs.forEach(
        (id, input) -> merged.add(new PlaybookTriageModification.ReplaceStepInput(id, input)));
    updatedParams.forEach(
        (k, v) -> merged.add(new PlaybookTriageModification.UpdateParameter(k, v)));
    return List.copyOf(merged);
  }
}
