# io.casehub.api.spi.judgment.JudgmentVerifier

**Package:** `io.casehub.api.spi.judgment`

**Kind:** `interface`

Post-response verification strategy for judgment yields.

<p>Resolved via `EngineStrategyResolver` from `JudgmentTarget.verifierStrategy()`.
When no strategy is configured (null), verification is skipped entirely. The `VerificationContext` carries both the original yield context and the response fields (decision,
evidence, callerId, callerType).

<p>Refs engine#997, engine#994.

## Methods

### `public default java.lang.String id()`

### `public abstract io.casehub.api.spi.judgment.VerificationResult verify(io.casehub.api.spi.judgment.VerificationContext context)`

#### Parameters

- `context` (`io.casehub.api.spi.judgment.VerificationContext`)
