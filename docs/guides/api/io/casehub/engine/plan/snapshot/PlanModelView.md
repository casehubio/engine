# io.casehub.engine.plan.snapshot.PlanModelView

**Package:** `io.casehub.engine.plan.snapshot`

**Kind:** `record`

## Fields

### `agenda` (`java.util.List<io.casehub.engine.plan.snapshot.PlanModelView.AgendaItemView>`)

### `caseId` (`java.util.UUID`)

### `compounds` (`java.util.List<io.casehub.engine.plan.snapshot.PlanModelView.CompoundStatusView>`)

### `focus` (`java.lang.String`)

### `focusRationale` (`java.lang.String`)

### `resourceBudget` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `subCases` (`java.util.List<io.casehub.engine.plan.snapshot.PlanModelView.SubCaseView>`)

### `timestamp` (`java.time.Instant`)

## Record Components

### `agenda` (`java.util.List<io.casehub.engine.plan.snapshot.PlanModelView.AgendaItemView>`)

### `caseId` (`java.util.UUID`)

### `compounds` (`java.util.List<io.casehub.engine.plan.snapshot.PlanModelView.CompoundStatusView>`)

### `focus` (`java.lang.String`)

### `focusRationale` (`java.lang.String`)

### `resourceBudget` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `subCases` (`java.util.List<io.casehub.engine.plan.snapshot.PlanModelView.SubCaseView>`)

### `timestamp` (`java.time.Instant`)

## Constructors

### `public PlanModelView(java.util.UUID caseId, java.util.List<io.casehub.engine.plan.snapshot.PlanModelView.AgendaItemView> agenda, java.lang.String focus, java.lang.String focusRationale, java.util.Map<java.lang.String,java.lang.Object> resourceBudget, java.util.List<io.casehub.engine.plan.snapshot.PlanModelView.SubCaseView> subCases, java.util.List<io.casehub.engine.plan.snapshot.PlanModelView.CompoundStatusView> compounds, java.time.Instant timestamp)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `agenda` (`java.util.List<io.casehub.engine.plan.snapshot.PlanModelView.AgendaItemView>`)
- `focus` (`java.lang.String`)
- `focusRationale` (`java.lang.String`)
- `resourceBudget` (`java.util.Map<java.lang.String,java.lang.Object>`)
- `subCases` (`java.util.List<io.casehub.engine.plan.snapshot.PlanModelView.SubCaseView>`)
- `compounds` (`java.util.List<io.casehub.engine.plan.snapshot.PlanModelView.CompoundStatusView>`)
- `timestamp` (`java.time.Instant`)

## Methods

### `public java.util.List<io.casehub.engine.plan.snapshot.PlanModelView.AgendaItemView> agenda()`

### `public java.util.UUID caseId()`

### `public java.util.List<io.casehub.engine.plan.snapshot.PlanModelView.CompoundStatusView> compounds()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.String focus()`

### `public java.lang.String focusRationale()`

### `public final int hashCode()`

### `public java.util.Map<java.lang.String,java.lang.Object> resourceBudget()`

### `public java.util.List<io.casehub.engine.plan.snapshot.PlanModelView.SubCaseView> subCases()`

### `public java.time.Instant timestamp()`

### `public final java.lang.String toString()`
