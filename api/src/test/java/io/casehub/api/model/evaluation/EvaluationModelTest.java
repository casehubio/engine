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
package io.casehub.api.model.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EvaluationModelTest {

  @Test
  void evaluation_subject_rejects_null_subjectId() {
    assertThatThrownBy(() -> new EvaluationSubject(null, "output", "content"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void evaluation_subject_rejects_blank_subjectId() {
    assertThatThrownBy(() -> new EvaluationSubject("  ", "output", "content"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void evaluation_subject_stores_fields() {
    var subject = new EvaluationSubject("s-1", "agent-output", "result text");
    assertThat(subject.subjectId()).isEqualTo("s-1");
    assertThat(subject.subjectType()).isEqualTo("agent-output");
    assertThat(subject.content()).isEqualTo("result text");
  }

  @Test
  void evaluation_criteria_accepts_null_weight() {
    var criteria = new EvaluationCriteria("c-1", "accuracy");
    assertThat(criteria.weight()).isNull();
  }

  @Test
  void evaluation_criteria_stores_weight() {
    var criteria = new EvaluationCriteria("c-1", "accuracy", 0.8);
    assertThat(criteria.weight()).isEqualTo(0.8);
  }

  @Test
  void evaluation_criteria_rejects_blank_criteriaId() {
    assertThatThrownBy(() -> new EvaluationCriteria(" ", "accuracy"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void evaluation_stores_score_and_rationale() {
    var eval = new Evaluation("judge-1", "s-1", 0.95, "excellent");
    assertThat(eval.evaluatorId()).isEqualTo("judge-1");
    assertThat(eval.subjectId()).isEqualTo("s-1");
    assertThat(eval.score()).isEqualTo(0.95);
    assertThat(eval.rationale()).isEqualTo("excellent");
  }

  @Test
  void evaluation_accepts_null_rationale() {
    var eval = new Evaluation("judge-1", "s-1", 0.5, null);
    assertThat(eval.rationale()).isNull();
  }

  @Test
  void scored_candidate_stores_fields() {
    var candidate = new ScoredCandidate("c-1", 0.85, "strong match");
    assertThat(candidate.candidateId()).isEqualTo("c-1");
    assertThat(candidate.score()).isEqualTo(0.85);
    assertThat(candidate.rationale()).isEqualTo("strong match");
  }

  @Test
  void scored_candidate_rejects_null_candidateId() {
    assertThatThrownBy(() -> new ScoredCandidate(null, 0.5, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void evaluation_decision_selected_stores_fields() {
    var decision = new EvaluationDecision.Selected("c-1", 0.9, "best candidate");
    assertThat(decision.candidateId()).isEqualTo("c-1");
    assertThat(decision.confidence()).isEqualTo(0.9);
    assertThat(decision.rationale()).isEqualTo("best candidate");
  }

  @Test
  void evaluation_decision_rejected_stores_reason() {
    var decision = new EvaluationDecision.Rejected("insufficient quality");
    assertThat(decision.reason()).isEqualTo("insufficient quality");
  }

  @Test
  void evaluation_decision_deferred_stores_reason() {
    var decision = new EvaluationDecision.Deferred("awaiting more evaluations");
    assertThat(decision.reason()).isEqualTo("awaiting more evaluations");
  }

  @Test
  void evaluation_decision_is_sealed() {
    assertThat(EvaluationDecision.class).isSealed();
  }

  @Test
  void evaluation_lifecycle_event_is_sealed() {
    assertThat(EvaluationLifecycleEvent.class).isSealed();
  }

  @Test
  void lifecycle_started_stores_fields() {
    var event = new EvaluationLifecycleEvent.Started("judge-1", "s-1");
    assertThat(event.evaluatorId()).isEqualTo("judge-1");
    assertThat(event.subjectId()).isEqualTo("s-1");
  }

  @Test
  void lifecycle_completed_stores_decision() {
    var decision = new EvaluationDecision.Selected("c-1", 0.9, "best");
    var event = new EvaluationLifecycleEvent.Completed("judge-1", decision);
    assertThat(event.evaluatorId()).isEqualTo("judge-1");
    assertThat(event.decision()).isEqualTo(decision);
  }

  @Test
  void lifecycle_invalidated_stores_reason() {
    var event = new EvaluationLifecycleEvent.Invalidated("judge-1", "criteria changed");
    assertThat(event.evaluatorId()).isEqualTo("judge-1");
    assertThat(event.reason()).isEqualTo("criteria changed");
  }
}
