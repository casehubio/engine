# io.casehub.api.model.RoutingConfig

**Package:** `io.casehub.api.model`

**Kind:** `interface`

Sealed interface for caller-type-specific routing hints on `JudgmentTarget`.

<p>Separates WHO should answer a yield from WHAT is being asked (yield semantics on
JudgmentTarget) and HOW the answer is verified (verifier/escalator on JudgmentTarget).

<p>Refs engine#995, engine#994.
