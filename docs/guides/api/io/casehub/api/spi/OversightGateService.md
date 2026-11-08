# io.casehub.api.spi.OversightGateService

**Package:** `io.casehub.api.spi`

**Kind:** `interface`

Engine SPI for approval-gating agent actions. Implements `SupervisorGateBehavior` — the
`io.casehub.api.model.coordination.PlatformRole.SUPERVISOR` gating contract at the
worker-action level.

<p>`ActionRiskClassifier` determines whether a `io.casehub.worker.api.PlannedAction`
requires gating. When gating is required, this service manages the gate lifecycle.

## Methods

### `public abstract void fulfill(java.util.UUID gateId, java.lang.String rawOutput)`

#### Parameters

- `gateId` (`java.util.UUID`)
- `rawOutput` (`java.lang.String`)

### `public abstract io.casehub.api.spi.GateOutcome openGate(java.lang.String agentId, java.lang.String commitmentId, java.lang.String outcome, java.lang.String tenancyId)`

#### Parameters

- `agentId` (`java.lang.String`)
- `commitmentId` (`java.lang.String`)
- `outcome` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)
