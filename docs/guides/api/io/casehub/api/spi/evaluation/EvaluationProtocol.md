# io.casehub.api.spi.evaluation.EvaluationProtocol

**Package:** `io.casehub.api.spi.evaluation`

**Kind:** `interface`

## Methods

### `public abstract io.casehub.api.model.evaluation.Evaluation evaluate(io.casehub.api.model.evaluation.EvaluationSubject subject, java.util.List<io.casehub.api.model.evaluation.EvaluationCriteria> criteria)`

#### Parameters

- `subject` (`io.casehub.api.model.evaluation.EvaluationSubject`)
- `criteria` (`java.util.List<io.casehub.api.model.evaluation.EvaluationCriteria>`)

### `public abstract void onLifecycleEvent(io.casehub.api.model.evaluation.EvaluationLifecycleEvent event)`

#### Parameters

- `event` (`io.casehub.api.model.evaluation.EvaluationLifecycleEvent`)

### `public abstract io.casehub.api.model.evaluation.EvaluationDecision renderDecision(java.util.List<io.casehub.api.model.evaluation.Evaluation> evaluations, io.casehub.api.model.evaluation.VotingStrategy strategy)`

#### Parameters

- `evaluations` (`java.util.List<io.casehub.api.model.evaluation.Evaluation>`)
- `strategy` (`io.casehub.api.model.evaluation.VotingStrategy`)

### `public abstract java.util.List<io.casehub.api.model.evaluation.ScoredCandidate> score(java.util.List<io.casehub.api.model.evaluation.EvaluationSubject> candidates, java.util.List<io.casehub.api.model.evaluation.EvaluationCriteria> criteria)`

#### Parameters

- `candidates` (`java.util.List<io.casehub.api.model.evaluation.EvaluationSubject>`)
- `criteria` (`java.util.List<io.casehub.api.model.evaluation.EvaluationCriteria>`)
