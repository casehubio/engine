# io.casehub.api.model.stigmergy.TickTrace

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `caseId` (`java.util.UUID`)

### `gates` (`java.util.List<io.casehub.api.model.stigmergy.TickTrace.GateResult>`)

### `outcome` (`io.casehub.api.model.stigmergy.TickTrace.TickOutcome`)

### `timestamp` (`java.time.Instant`)

### `trigger` (`io.casehub.api.model.stigmergy.TickTrace.TickTrigger`)

## Record Components

### `caseId` (`java.util.UUID`)

### `gates` (`java.util.List<io.casehub.api.model.stigmergy.TickTrace.GateResult>`)

### `outcome` (`io.casehub.api.model.stigmergy.TickTrace.TickOutcome`)

### `timestamp` (`java.time.Instant`)

### `trigger` (`io.casehub.api.model.stigmergy.TickTrace.TickTrigger`)

## Constructors

### `public TickTrace(java.util.UUID caseId, java.time.Instant timestamp, io.casehub.api.model.stigmergy.TickTrace.TickTrigger trigger, java.util.List<io.casehub.api.model.stigmergy.TickTrace.GateResult> gates, io.casehub.api.model.stigmergy.TickTrace.TickOutcome outcome)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `timestamp` (`java.time.Instant`)
- `trigger` (`io.casehub.api.model.stigmergy.TickTrace.TickTrigger`)
- `gates` (`java.util.List<io.casehub.api.model.stigmergy.TickTrace.GateResult>`)
- `outcome` (`io.casehub.api.model.stigmergy.TickTrace.TickOutcome`)

## Methods

### `public java.util.UUID caseId()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.List<io.casehub.api.model.stigmergy.TickTrace.GateResult> gates()`

### `public final int hashCode()`

### `public io.casehub.api.model.stigmergy.TickTrace.TickOutcome outcome()`

### `public java.time.Instant timestamp()`

### `public final java.lang.String toString()`

### `public io.casehub.api.model.stigmergy.TickTrace.TickTrigger trigger()`
