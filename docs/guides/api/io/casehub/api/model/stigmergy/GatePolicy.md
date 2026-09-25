# io.casehub.api.model.stigmergy.GatePolicy

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `gateTimeoutMinutes` (`java.lang.Integer`)

### `modes` (`java.util.Map<java.lang.String,io.casehub.api.model.stigmergy.GatePolicy.GateMode>`)

## Record Components

### `gateTimeoutMinutes` (`java.lang.Integer`)

### `modes` (`java.util.Map<java.lang.String,io.casehub.api.model.stigmergy.GatePolicy.GateMode>`)

## Constructors

### `public GatePolicy(java.util.Map<java.lang.String,io.casehub.api.model.stigmergy.GatePolicy.GateMode> modes, java.lang.Integer gateTimeoutMinutes)`

#### Parameters

- `modes` (`java.util.Map<java.lang.String,io.casehub.api.model.stigmergy.GatePolicy.GateMode>`)
- `gateTimeoutMinutes` (`java.lang.Integer`)

## Methods

### `public int effectiveGateTimeoutMinutes()`

### `public io.casehub.api.model.stigmergy.GatePolicy.GateMode effectiveMode(java.lang.String stageId)`

#### Parameters

- `stageId` (`java.lang.String`)

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.Integer gateTimeoutMinutes()`

### `public final int hashCode()`

### `public java.util.Map<java.lang.String,io.casehub.api.model.stigmergy.GatePolicy.GateMode> modes()`

### `public final java.lang.String toString()`
