# io.casehub.api.spi.observation.InterestDeclaration.SignalThreshold

**Package:** `io.casehub.api.spi.observation`

**Kind:** `record`

## Fields

### `operator` (`io.casehub.api.spi.observation.InterestDeclaration.ComparisonOperator`)

### `signalName` (`java.lang.String`)

### `threshold` (`double`)

## Record Components

### `operator` (`io.casehub.api.spi.observation.InterestDeclaration.ComparisonOperator`)

### `signalName` (`java.lang.String`)

### `threshold` (`double`)

## Constructors

### `public SignalThreshold(java.lang.String signalName, io.casehub.api.spi.observation.InterestDeclaration.ComparisonOperator operator, double threshold)`

#### Parameters

- `signalName` (`java.lang.String`)
- `operator` (`io.casehub.api.spi.observation.InterestDeclaration.ComparisonOperator`)
- `threshold` (`double`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public io.casehub.api.spi.observation.InterestDeclaration.ComparisonOperator operator()`

### `public java.lang.String signalName()`

### `public double threshold()`

### `public final java.lang.String toString()`
