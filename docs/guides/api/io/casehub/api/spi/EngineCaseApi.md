# io.casehub.api.spi.EngineCaseApi

**Package:** `io.casehub.api.spi`

**Kind:** `interface`

## Methods

### `public abstract Multi<io.casehub.api.view.CaseContextChangeEventView> caseContextChange(java.util.UUID caseId)`

#### Parameters

- `caseId` (`java.util.UUID`)

### `public abstract Multi<io.casehub.api.view.CaseLifecycleEventView> caseLifecycle(java.util.UUID caseId)`

#### Parameters

- `caseId` (`java.util.UUID`)

### `public abstract Multi<io.casehub.api.view.CaseStreamEventView> caseStream(java.util.UUID caseId)`

#### Parameters

- `caseId` (`java.util.UUID`)

### `public abstract io.casehub.api.view.CaseInstanceView getCaseById(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract java.util.Map<java.lang.String,java.lang.Object> getCaseContext(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract java.util.Map<java.lang.String,java.lang.Object> getCaseContextPath(java.util.UUID caseId, java.lang.String path, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `path` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.api.view.GoalEvaluationView getGoals(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract java.util.List<io.casehub.api.view.PlanItemView> getPlanItems(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.api.view.CasePage listCases(io.casehub.api.model.CaseStatus status, java.lang.String namespace, java.lang.String name, java.lang.String tenancyId, java.lang.Integer offset, java.lang.Integer limit)`

#### Parameters

- `status` (`io.casehub.api.model.CaseStatus`)
- `namespace` (`java.lang.String`)
- `name` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
- `offset` (`java.lang.Integer`)
- `limit` (`java.lang.Integer`)

### `public abstract io.casehub.api.view.CaseInstanceView startCase(io.casehub.api.view.StartCaseRequest request, java.lang.String tenancyId)`

#### Parameters

- `request` (`io.casehub.api.view.StartCaseRequest`)
- `tenancyId` (`java.lang.String`)
