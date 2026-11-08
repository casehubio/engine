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
package io.casehub.engine.runtime.evaluation;

import io.casehub.api.model.evaluation.Evaluation;
import io.casehub.api.model.evaluation.EvaluationCriteria;
import io.casehub.api.model.evaluation.EvaluationDecision;
import io.casehub.api.model.evaluation.EvaluationLifecycleEvent;
import io.casehub.api.model.evaluation.EvaluationSubject;
import io.casehub.api.model.evaluation.ScoredCandidate;
import io.casehub.api.model.evaluation.VotingStrategy;
import io.casehub.api.spi.evaluation.EvaluationProtocol;
import java.util.List;

public class NoOpEvaluationProtocol implements EvaluationProtocol {

  @Override
  public Evaluation evaluate(EvaluationSubject subject, List<EvaluationCriteria> criteria) {
    return new Evaluation("no-op", subject.subjectId(), 0.0, "no evaluator configured");
  }

  @Override
  public List<ScoredCandidate> score(
      List<EvaluationSubject> candidates, List<EvaluationCriteria> criteria) {
    return List.of();
  }

  @Override
  public EvaluationDecision renderDecision(List<Evaluation> evaluations, VotingStrategy strategy) {
    return new EvaluationDecision.Deferred("no evaluator configured");
  }

  @Override
  public void onLifecycleEvent(EvaluationLifecycleEvent event) {}
}
