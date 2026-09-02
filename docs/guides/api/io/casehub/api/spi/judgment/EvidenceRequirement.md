# io.casehub.api.spi.judgment.EvidenceRequirement

**Package:** `io.casehub.api.spi.judgment`

**Kind:** `record`

Declares what evidence a judgment response must include. Verifiers use these requirements to
validate responses.

<p>Refs engine#1009, engine#994.

## Fields

### `key` (`java.lang.String`)

### `required` (`boolean`)

### `type` (`io.casehub.api.spi.judgment.EvidenceType`)

## Record Components

### `key` (`java.lang.String`)

### `required` (`boolean`)

### `type` (`io.casehub.api.spi.judgment.EvidenceType`)

## Constructors

### `public EvidenceRequirement(java.lang.String key, io.casehub.api.spi.judgment.EvidenceType type)`

#### Parameters

- `key` (`java.lang.String`)
- `type` (`io.casehub.api.spi.judgment.EvidenceType`)

### `public EvidenceRequirement(java.lang.String key, io.casehub.api.spi.judgment.EvidenceType type, boolean required)`

#### Parameters

- `key` (`java.lang.String`)
- `type` (`io.casehub.api.spi.judgment.EvidenceType`)
- `required` (`boolean`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.String key()`

### `public static io.casehub.api.spi.judgment.EvidenceRequirement optional(java.lang.String key, io.casehub.api.spi.judgment.EvidenceType type)`

#### Parameters

- `key` (`java.lang.String`)
- `type` (`io.casehub.api.spi.judgment.EvidenceType`)

### `public boolean required()`

### `public static io.casehub.api.spi.judgment.EvidenceRequirement required(java.lang.String key, io.casehub.api.spi.judgment.EvidenceType type)`

#### Parameters

- `key` (`java.lang.String`)
- `type` (`io.casehub.api.spi.judgment.EvidenceType`)

### `public final java.lang.String toString()`

### `public io.casehub.api.spi.judgment.EvidenceType type()`
