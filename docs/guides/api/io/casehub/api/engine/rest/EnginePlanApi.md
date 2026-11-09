# io.casehub.api.engine.rest.EnginePlanApi

**Package:** `io.casehub.api.engine.rest`

**Kind:** `interface`

## Methods

### `public abstract java.util.concurrent.Flow.Publisher<io.casehub.engine.plan.snapshot.ExecutionStateView> executionStateStream(java.util.UUID caseId)`

#### Parameters

- `caseId` (`java.util.UUID`)

### `public abstract io.casehub.engine.plan.snapshot.DagPlanSnapshot getDagPlan(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.engine.plan.snapshot.DagResultView getDagResult(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.engine.plan.snapshot.DecompositionSnapshot getDecomposition(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.engine.plan.snapshot.ExecutionStateView getExecutionState(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract java.util.List<io.casehub.engine.plan.snapshot.PlanItemDefinitionSnapshot> getPlanDefinitions(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.engine.plan.snapshot.PlanModelView getPlanModel(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.engine.plan.snapshot.PlanOverviewView getPlanOverview(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
