# io.casehub.api.spi.enforcement.ProtocolEnforcement

**Package:** `io.casehub.api.spi.enforcement`

**Kind:** `interface`

## Methods

### `public abstract boolean checkCompliance(java.lang.String agentId, java.util.List<io.casehub.api.model.enforcement.InteractionRule> rules)`

#### Parameters

- `agentId` (`java.lang.String`)
- `rules` (`java.util.List<io.casehub.api.model.enforcement.InteractionRule>`)

### `public abstract io.casehub.api.model.enforcement.ViolationResponse handleViolation(io.casehub.api.model.enforcement.Violation violation)`

#### Parameters

- `violation` (`io.casehub.api.model.enforcement.Violation`)

### `public abstract void onLifecycleEvent(io.casehub.api.model.enforcement.EnforcementLifecycleEvent event)`

#### Parameters

- `event` (`io.casehub.api.model.enforcement.EnforcementLifecycleEvent`)

### `public abstract io.casehub.api.model.enforcement.TurnDecision requestTurn(io.casehub.api.model.enforcement.TurnRequest request, java.util.List<io.casehub.api.model.enforcement.InteractionRule> activeRules)`

#### Parameters

- `request` (`io.casehub.api.model.enforcement.TurnRequest`)
- `activeRules` (`java.util.List<io.casehub.api.model.enforcement.InteractionRule>`)
