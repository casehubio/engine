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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlaybookTriageModificationParserTest {

  @Test
  void parsesSkipSteps() {
    Map<String, Object> mods = Map.of("playbook.skipSteps", List.of("step-3", "step-5"));
    List<PlaybookTriageModification> result = PlaybookTriageModificationParser.parse(mods);

    assertThat(result).hasSize(2);
    assertThat(result.getFirst()).isInstanceOf(PlaybookTriageModification.SkipStep.class);
    assertThat(((PlaybookTriageModification.SkipStep) result.getFirst()).stepId())
        .isEqualTo("step-3");
  }

  @Test
  void parsesReplaceInput() {
    Map<String, Object> mods =
        Map.of("playbook.replaceInput.step-2", Map.of("url", "/api/v2/contacts"));
    List<PlaybookTriageModification> result = PlaybookTriageModificationParser.parse(mods);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst()).isInstanceOf(PlaybookTriageModification.ReplaceStepInput.class);
    var replace = (PlaybookTriageModification.ReplaceStepInput) result.getFirst();
    assertThat(replace.stepId()).isEqualTo("step-2");
    assertThat(replace.newInput()).containsEntry("url", "/api/v2/contacts");
  }

  @Test
  void parsesUpdateParameter() {
    Map<String, Object> mods = Map.of("playbook.updateParams.timeout", 30000);
    List<PlaybookTriageModification> result = PlaybookTriageModificationParser.parse(mods);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst()).isInstanceOf(PlaybookTriageModification.UpdateParameter.class);
    var update = (PlaybookTriageModification.UpdateParameter) result.getFirst();
    assertThat(update.key()).isEqualTo("timeout");
    assertThat(update.value()).isEqualTo(30000);
  }

  @Test
  void ignoresUnknownKeys() {
    Map<String, Object> mods = Map.of("unknown.key", "value", "playbook.skipSteps", List.of("s1"));
    List<PlaybookTriageModification> result = PlaybookTriageModificationParser.parse(mods);

    assertThat(result).hasSize(1);
  }

  @Test
  void emptyMapReturnsEmptyList() {
    assertThat(PlaybookTriageModificationParser.parse(Map.of())).isEmpty();
  }
}
