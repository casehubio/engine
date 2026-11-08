# io.casehub.api.model.coordination.PlatformRole

**Package:** `io.casehub.api.model.coordination`

**Kind:** `enum`

Intrinsic behavioral contracts — the platform implements specific behavior for these roles.

## Fields

### `requiredStructuralRole` (`io.casehub.api.model.coordination.StructuralRole`)

## Enum Constants

### `COORDINATOR` (`io.casehub.api.model.coordination.PlatformRole`)

### `JUDGE` (`io.casehub.api.model.coordination.PlatformRole`)

### `MODERATOR` (`io.casehub.api.model.coordination.PlatformRole`)

### `SUPERVISOR` (`io.casehub.api.model.coordination.PlatformRole`)

Oversees, approves/rejects, gates progression. Implemented at two granularities via `SupervisorGateBehavior`:

<ul>
  <li>`io.casehub.api.spi.OversightGateService` — worker-action gates for `PlannedAction` approval
  <li>`ConductorInboxManager` — evolution-pipeline gates for research stage approval
</ul>

<p>Classification (deciding <em>whether</em> gating is needed) is handled by `io.casehub.api.spi.ActionRiskClassifier`, which precedes the gate.

## Constructors

### `private PlatformRole(io.casehub.api.model.coordination.StructuralRole requiredStructuralRole)`

#### Parameters

- `requiredStructuralRole` (`io.casehub.api.model.coordination.StructuralRole`)

## Methods

### `public io.casehub.api.model.coordination.StructuralRole requiredStructuralRole()`

### `public static io.casehub.api.model.coordination.PlatformRole valueOf(java.lang.String name)`

#### Parameters

- `name` (`java.lang.String`)

### `public static io.casehub.api.model.coordination.PlatformRole[] values()`
