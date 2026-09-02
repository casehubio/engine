# io.casehub.api.spi.judgment.CallerConfig

**Package:** `io.casehub.api.spi.judgment`

**Kind:** `interface`

Declares who can fulfill a judgment request. Used by `io.casehub.api.model.JudgmentTarget`
to specify the initial caller type and by `EscalationDecision.Escalate` to specify the
escalation target.

<p>Refs engine#1012, engine#1009, engine#994.

## Methods

### `public static io.casehub.api.spi.judgment.CallerConfig.Human human()`
