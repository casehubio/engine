# io.casehub.api.spi.EnginePlanApi

**Package:** `io.casehub.api.spi`

**Kind:** `interface`

## Methods

### `public abstract Multi<java.lang.Object> executionStateStream(java.util.UUID caseId)`

#### Parameters

- `caseId` (`java.util.UUID`)

### `public abstract io.casehub.engine.plan.snapshot.DagPlanSnapshot getDagPlan(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract java.lang.Object getDagResult(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.engine.plan.snapshot.DecompositionSnapshot getDecomposition(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract java.lang.Object getExecutionState(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract java.util.List<io.casehub.engine.plan.snapshot.PlanItemDefinitionSnapshot> getPlanDefinitions(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract java.lang.Object getPlanModel(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
