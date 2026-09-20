# io.casehub.api.model.signal.Signal

**Package:** `io.casehub.api.model.signal`

**Kind:** `record`

## Fields

### `expired` (`boolean`)

### `firstDeposited` (`java.time.Instant`)

### `halfLife` (`java.time.Duration`)

### `lastReinforced` (`java.time.Instant`)

### `lastSource` (`java.lang.String`)

### `name` (`java.lang.String`)

### `reinforcementCount` (`int`)

### `sources` (`java.util.Set<java.lang.String>`)

### `strength` (`double`)

## Record Components

### `expired` (`boolean`)

### `firstDeposited` (`java.time.Instant`)

### `halfLife` (`java.time.Duration`)

### `lastReinforced` (`java.time.Instant`)

### `lastSource` (`java.lang.String`)

### `name` (`java.lang.String`)

### `reinforcementCount` (`int`)

### `sources` (`java.util.Set<java.lang.String>`)

### `strength` (`double`)

## Constructors

### `public Signal(java.lang.String name, double strength, java.time.Instant firstDeposited, java.time.Instant lastReinforced, java.time.Duration halfLife, java.lang.String lastSource, int reinforcementCount, boolean expired)`

#### Parameters

- `name` (`java.lang.String`)
- `strength` (`double`)
- `firstDeposited` (`java.time.Instant`)
- `lastReinforced` (`java.time.Instant`)
- `halfLife` (`java.time.Duration`)
- `lastSource` (`java.lang.String`)
- `reinforcementCount` (`int`)
- `expired` (`boolean`)

### `public Signal(java.lang.String name, double strength, java.time.Instant firstDeposited, java.time.Instant lastReinforced, java.time.Duration halfLife, java.lang.String lastSource, int reinforcementCount, boolean expired, java.util.Set<java.lang.String> sources)`

#### Parameters

- `name` (`java.lang.String`)
- `strength` (`double`)
- `firstDeposited` (`java.time.Instant`)
- `lastReinforced` (`java.time.Instant`)
- `halfLife` (`java.time.Duration`)
- `lastSource` (`java.lang.String`)
- `reinforcementCount` (`int`)
- `expired` (`boolean`)
- `sources` (`java.util.Set<java.lang.String>`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public boolean expired()`

### `public java.time.Instant firstDeposited()`

### `public java.time.Duration halfLife()`

### `public final int hashCode()`

### `public java.time.Instant lastReinforced()`

### `public java.lang.String lastSource()`

### `public java.lang.String name()`

### `public int reinforcementCount()`

### `public java.util.Set<java.lang.String> sources()`

### `public double strength()`

### `public final java.lang.String toString()`
