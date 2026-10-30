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

  @Test
  void mergeAll_emptyListReturnsEmpty() {
    assertThat(PlaybookTriageModificationParser.mergeAll(List.of())).isEmpty();
  }

  @Test
  void mergeAll_singleRoundSameAsParse() {
    Map<String, Object> round = Map.of("playbook.skipSteps", List.of("s1"));
    List<PlaybookTriageModification> merged =
        PlaybookTriageModificationParser.mergeAll(List.of(round));
    assertThat(merged).hasSize(1);
    assertThat(merged.getFirst()).isInstanceOf(PlaybookTriageModification.SkipStep.class);
  }

  @Test
  void mergeAll_cumulativeSkipSteps() {
    var round1 = Map.<String, Object>of("playbook.skipSteps", List.of("step-a"));
    var round2 = Map.<String, Object>of("playbook.skipSteps", List.of("step-b"));
    List<PlaybookTriageModification> merged =
        PlaybookTriageModificationParser.mergeAll(List.of(round1, round2));

    List<String> skipped =
        merged.stream()
            .filter(m -> m instanceof PlaybookTriageModification.SkipStep)
            .map(m -> ((PlaybookTriageModification.SkipStep) m).stepId())
            .toList();
    assertThat(skipped).containsExactlyInAnyOrder("step-a", "step-b");
  }

  @Test
  void mergeAll_duplicateSkipStepDeduplicates() {
    var round1 = Map.<String, Object>of("playbook.skipSteps", List.of("step-a"));
    var round2 = Map.<String, Object>of("playbook.skipSteps", List.of("step-a", "step-b"));
    List<PlaybookTriageModification> merged =
        PlaybookTriageModificationParser.mergeAll(List.of(round1, round2));

    List<String> skipped =
        merged.stream()
            .filter(m -> m instanceof PlaybookTriageModification.SkipStep)
            .map(m -> ((PlaybookTriageModification.SkipStep) m).stepId())
            .toList();
    assertThat(skipped).containsExactlyInAnyOrder("step-a", "step-b");
  }

  @Test
  void mergeAll_replaceStepInputLastWriterWins() {
    var round1 = Map.<String, Object>of("playbook.replaceInput.fetch", Map.of("url", "/v1"));
    var round2 = Map.<String, Object>of("playbook.replaceInput.fetch", Map.of("url", "/v2"));
    List<PlaybookTriageModification> merged =
        PlaybookTriageModificationParser.mergeAll(List.of(round1, round2));

    var replace =
        merged.stream()
            .filter(m -> m instanceof PlaybookTriageModification.ReplaceStepInput)
            .map(m -> (PlaybookTriageModification.ReplaceStepInput) m)
            .findFirst()
            .orElseThrow();
    assertThat(replace.newInput()).containsEntry("url", "/v2");
  }

  @Test
  void mergeAll_updateParameterLastWriterWins() {
    var round1 = Map.<String, Object>of("playbook.updateParams.timeout", 1000);
    var round2 = Map.<String, Object>of("playbook.updateParams.timeout", 5000);
    List<PlaybookTriageModification> merged =
        PlaybookTriageModificationParser.mergeAll(List.of(round1, round2));

    var update =
        merged.stream()
            .filter(m -> m instanceof PlaybookTriageModification.UpdateParameter)
            .map(m -> (PlaybookTriageModification.UpdateParameter) m)
            .findFirst()
            .orElseThrow();
    assertThat(update.value()).isEqualTo(5000);
  }
}
