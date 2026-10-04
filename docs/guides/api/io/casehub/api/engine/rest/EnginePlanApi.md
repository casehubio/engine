# io.casehub.api.engine.rest.EnginePlanApi

**Package:** `io.casehub.api.engine.rest`

**Kind:** `interface`

## Methods

### `public abstract Multi<JsonNode> executionStateStream(java.util.UUID caseId)`

#### Parameters

- `caseId` (`java.util.UUID`)

### `public abstract JsonNode getDagPlan(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract JsonNode getDagResult(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract JsonNode getDecomposition(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract JsonNode getExecutionState(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract JsonNode getPlanDefinitions(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract JsonNode getPlanModel(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
