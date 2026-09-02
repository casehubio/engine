# io.casehub.api.spi.judgment.CallerIdentity

**Package:** `io.casehub.api.spi.judgment`

**Kind:** `record`

Identity of the caller who responded to a judgment request. Both callerId and callerType are
required — if no caller identity is known, the field is null at the container level.

<p>Refs engine#1012, engine#1009, engine#994.

## Fields

### `callerId` (`java.lang.String`)

### `callerType` (`java.lang.String`)

### `trustScore` (`java.lang.Double`)

## Record Components

### `callerId` (`java.lang.String`)

### `callerType` (`java.lang.String`)

### `trustScore` (`java.lang.Double`)

## Constructors

### `public CallerIdentity(java.lang.String callerId, java.lang.String callerType, java.lang.Double trustScore)`

#### Parameters

- `callerId` (`java.lang.String`)
- `callerType` (`java.lang.String`)
- `trustScore` (`java.lang.Double`)

## Methods

### `public java.lang.String callerId()`

### `public java.lang.String callerType()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public static io.casehub.api.spi.judgment.CallerIdentity of(java.lang.String callerId, java.lang.String callerType)`

#### Parameters

- `callerId` (`java.lang.String`)
- `callerType` (`java.lang.String`)

### `public static io.casehub.api.spi.judgment.CallerIdentity of(java.lang.String callerId, java.lang.String callerType, java.lang.Double trustScore)`

#### Parameters

- `callerId` (`java.lang.String`)
- `callerType` (`java.lang.String`)
- `trustScore` (`java.lang.Double`)

### `public final java.lang.String toString()`

### `public java.lang.Double trustScore()`
