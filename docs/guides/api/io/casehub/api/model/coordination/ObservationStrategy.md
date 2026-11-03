# io.casehub.api.model.coordination.ObservationStrategy

**Package:** `io.casehub.api.model.coordination`

**Kind:** `enum`

What triggers agent reasoning within a coordination topology. Independent of topology and scope —
a STIMULUS_DRIVEN agent can observe via events or ticks.

## Enum Constants

### `DEMAND_DRIVEN` (`io.casehub.api.model.coordination.ObservationStrategy`)

### `EVENT_DRIVEN` (`io.casehub.api.model.coordination.ObservationStrategy`)

### `TICK_DRIVEN` (`io.casehub.api.model.coordination.ObservationStrategy`)

## Constructors

### `private ObservationStrategy()`

## Methods

### `public static io.casehub.api.model.coordination.ObservationStrategy valueOf(java.lang.String name)`

#### Parameters

- `name` (`java.lang.String`)

### `public static io.casehub.api.model.coordination.ObservationStrategy[] values()`
