# io.casehub.api.view.CaseOverviewView

**Package:** `io.casehub.api.view`

**Kind:** `record`

## Fields

### `context` (`JsonNode`)

### `goals` (`io.casehub.api.view.GoalEvaluationView`)

### `instance` (`io.casehub.api.view.CaseInstanceView`)

### `planItems` (`java.util.List<io.casehub.api.view.PlanItemView>`)

### `recentEvents` (`io.casehub.api.view.EventLogPage`)

## Record Components

### `context` (`JsonNode`)

### `goals` (`io.casehub.api.view.GoalEvaluationView`)

### `instance` (`io.casehub.api.view.CaseInstanceView`)

### `planItems` (`java.util.List<io.casehub.api.view.PlanItemView>`)

### `recentEvents` (`io.casehub.api.view.EventLogPage`)

## Constructors

### `public CaseOverviewView(io.casehub.api.view.CaseInstanceView instance, JsonNode context, java.util.List<io.casehub.api.view.PlanItemView> planItems, io.casehub.api.view.GoalEvaluationView goals, io.casehub.api.view.EventLogPage recentEvents)`

#### Parameters

- `instance` (`io.casehub.api.view.CaseInstanceView`)
- `context` (`JsonNode`)
- `planItems` (`java.util.List<io.casehub.api.view.PlanItemView>`)
- `goals` (`io.casehub.api.view.GoalEvaluationView`)
- `recentEvents` (`io.casehub.api.view.EventLogPage`)

## Methods

### `public JsonNode context()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public io.casehub.api.view.GoalEvaluationView goals()`

### `public final int hashCode()`

### `public io.casehub.api.view.CaseInstanceView instance()`

### `public java.util.List<io.casehub.api.view.PlanItemView> planItems()`

### `public io.casehub.api.view.EventLogPage recentEvents()`

### `public final java.lang.String toString()`
