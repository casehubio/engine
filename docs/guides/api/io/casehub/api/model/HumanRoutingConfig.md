# io.casehub.api.model.HumanRoutingConfig

**Package:** `io.casehub.api.model`

**Kind:** `record`

Routing configuration for human callers — carried on `JudgmentTarget.routingConfig()`.

<p>These fields are consumed by the scheduler layer to create WorkItems.

<p>Refs engine#995.

## Fields

### `candidateGroups` (`io.casehub.api.spi.routing.CandidateSetSpec`)

### `candidateUsers` (`io.casehub.api.spi.routing.CandidateSetSpec`)

### `claimDeadlineHours` (`java.lang.Integer`)

### `payloadType` (`java.lang.Class<?>`)

### `templateRef` (`java.lang.String`)

## Record Components

### `candidateGroups` (`io.casehub.api.spi.routing.CandidateSetSpec`)

### `candidateUsers` (`io.casehub.api.spi.routing.CandidateSetSpec`)

### `claimDeadlineHours` (`java.lang.Integer`)

### `payloadType` (`java.lang.Class<?>`)

### `templateRef` (`java.lang.String`)

## Constructors

### `public HumanRoutingConfig(java.lang.String templateRef, io.casehub.api.spi.routing.CandidateSetSpec candidateGroups, io.casehub.api.spi.routing.CandidateSetSpec candidateUsers, java.lang.Integer claimDeadlineHours, java.lang.Class<?> payloadType)`

#### Parameters

- `templateRef` (`java.lang.String`)
- `candidateGroups` (`io.casehub.api.spi.routing.CandidateSetSpec`)
- `candidateUsers` (`io.casehub.api.spi.routing.CandidateSetSpec`)
- `claimDeadlineHours` (`java.lang.Integer`)
- `payloadType` (`java.lang.Class<?>`)

## Methods

### `public io.casehub.api.spi.routing.CandidateSetSpec candidateGroups()`

### `public io.casehub.api.spi.routing.CandidateSetSpec candidateUsers()`

### `public java.lang.Integer claimDeadlineHours()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.Class<?> payloadType()`

### `public java.lang.String templateRef()`

### `public final java.lang.String toString()`
