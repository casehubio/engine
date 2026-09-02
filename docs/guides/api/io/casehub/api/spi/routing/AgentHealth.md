# io.casehub.api.spi.routing.AgentHealth

**Package:** `io.casehub.api.spi.routing`

**Kind:** `enum`

Pre-probed agent health status, mapped from `casehub-eidos-api` `CapabilityStatus` at
candidate construction time.

<p>`UNAVAILABLE` and `EXCLUDED` workers are filtered before the candidate list is
built — they never reach `AgentRoutingStrategy.select`. This enum exists so `casehub-engine-api` does not take a compile-time dependency on `casehub-eidos-api`.

<p>Enum declaration order reflects severity (softest first): `READY` > `BEHAVIORAL_VIOLATION` > `EPISTEMICALLY_WEAK` > `DEGRADED`.

## Enum Constants

### `BEHAVIORAL_VIOLATION` (`io.casehub.api.spi.routing.AgentHealth`)

Agent has behavioral compliance violations but is still operational — soft demotion.

### `DEGRADED` (`io.casehub.api.spi.routing.AgentHealth`)

Agent is available but operating in a degraded state — keep, consider demoting.

### `EPISTEMICALLY_WEAK` (`io.casehub.api.spi.routing.AgentHealth`)

Agent's epistemic coverage for this capability is uncertain — keep, consider demoting.

### `READY` (`io.casehub.api.spi.routing.AgentHealth`)

Agent is available and operating normally.

## Constructors

### `private AgentHealth()`

## Methods

### `public static io.casehub.api.spi.routing.AgentHealth valueOf(java.lang.String name)`

#### Parameters

- `name` (`java.lang.String`)

### `public static io.casehub.api.spi.routing.AgentHealth[] values()`
