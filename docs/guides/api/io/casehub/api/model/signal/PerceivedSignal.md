# io.casehub.api.model.signal.PerceivedSignal

**Package:** `io.casehub.api.model.signal`

**Kind:** `record`

## Fields

### `age` (`java.time.Duration`)

### `effectiveStrength` (`double`)

### `lastSource` (`java.lang.String`)

### `name` (`java.lang.String`)

### `reinforcementCount` (`int`)

### `sources` (`java.util.Set<java.lang.String>`)

## Record Components

### `age` (`java.time.Duration`)

### `effectiveStrength` (`double`)

### `lastSource` (`java.lang.String`)

### `name` (`java.lang.String`)

### `reinforcementCount` (`int`)

### `sources` (`java.util.Set<java.lang.String>`)

## Constructors

### `public PerceivedSignal(java.lang.String name, double effectiveStrength, int reinforcementCount, java.lang.String lastSource, java.time.Duration age, java.util.Set<java.lang.String> sources)`

#### Parameters

- `name` (`java.lang.String`)
- `effectiveStrength` (`double`)
- `reinforcementCount` (`int`)
- `lastSource` (`java.lang.String`)
- `age` (`java.time.Duration`)
- `sources` (`java.util.Set<java.lang.String>`)

## Methods

### `public java.time.Duration age()`

### `public double effectiveStrength()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.String lastSource()`

### `public java.lang.String name()`

### `public int reinforcementCount()`

### `public java.util.Set<java.lang.String> sources()`

### `public final java.lang.String toString()`
