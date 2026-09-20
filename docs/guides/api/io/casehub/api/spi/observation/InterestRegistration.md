# io.casehub.api.spi.observation.InterestRegistration

**Package:** `io.casehub.api.spi.observation`

**Kind:** `record`

## Fields

### `declaration` (`io.casehub.api.spi.observation.InterestDeclaration`)

### `interestId` (`java.lang.String`)

### `registeredAt` (`java.time.Instant`)

## Record Components

### `declaration` (`io.casehub.api.spi.observation.InterestDeclaration`)

### `interestId` (`java.lang.String`)

### `registeredAt` (`java.time.Instant`)

## Constructors

### `public InterestRegistration(java.lang.String interestId, io.casehub.api.spi.observation.InterestDeclaration declaration, java.time.Instant registeredAt)`

#### Parameters

- `interestId` (`java.lang.String`)
- `declaration` (`io.casehub.api.spi.observation.InterestDeclaration`)
- `registeredAt` (`java.time.Instant`)

## Methods

### `public io.casehub.api.spi.observation.InterestDeclaration declaration()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.String interestId()`

### `public java.time.Instant registeredAt()`

### `public final java.lang.String toString()`
