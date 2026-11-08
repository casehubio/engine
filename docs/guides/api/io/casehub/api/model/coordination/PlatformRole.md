# io.casehub.api.model.coordination.PlatformRole

**Package:** `io.casehub.api.model.coordination`

**Kind:** `enum`

Intrinsic behavioral contracts — the platform implements specific behavior for these roles.

## Fields

### `requiredStructuralRole` (`io.casehub.api.model.coordination.StructuralRole`)

## Enum Constants

### `COORDINATOR` (`io.casehub.api.model.coordination.PlatformRole`)

Orchestrates, delegates tasks, allocates agents, and aggregates results.

### `JUDGE` (`io.casehub.api.model.coordination.PlatformRole`)

Evaluates, scores, and renders decisions on agent outputs. Coordinates multi-judge voting
protocols (majority, consensus, weighted, threshold) via `JudgeBehavior`.

<p>The behavioral contract is defined by `io.casehub.api.spi.evaluation.EvaluationProtocol`, which provides:

<ul>
  <li>Single-subject evaluation against criteria
  <li>Multi-candidate scoring and ranking
  <li>Decision rendering with voting strategy
</ul>

### `MODERATOR` (`io.casehub.api.model.coordination.PlatformRole`)

Enforces protocol rules, manages turn-taking, and handles violations in agent interactions.
Mode-aware — enforcement behavior adapts to the active `CommunicationMode`.

<p>The behavioral contract is defined by `io.casehub.api.spi.enforcement.ProtocolEnforcement`, which provides:

<ul>
  <li>Turn-taking control (request/permit/deny)
  <li>Protocol compliance checking
  <li>Violation detection and response (warn/block/eject)
</ul>

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
