# io.casehub.api.spi.judgment.Evidence

**Package:** `io.casehub.api.spi.judgment`

**Kind:** `record`

A single piece of typed evidence provided with a judgment response. Carries a name, evidence
type, string content, and an optional external reference.

<p>Refs engine#1012, engine#1009, engine#994.

## Fields

### `content` (`java.lang.String`)

### `name` (`java.lang.String`)

### `ref` (`java.lang.String`)

### `type` (`io.casehub.api.spi.judgment.EvidenceType`)

## Record Components

### `content` (`java.lang.String`)

### `name` (`java.lang.String`)

### `ref` (`java.lang.String`)

### `type` (`io.casehub.api.spi.judgment.EvidenceType`)

## Constructors

### `public Evidence(java.lang.String name, io.casehub.api.spi.judgment.EvidenceType type, java.lang.String content, java.lang.String ref)`

#### Parameters

- `name` (`java.lang.String`)
- `type` (`io.casehub.api.spi.judgment.EvidenceType`)
- `content` (`java.lang.String`)
- `ref` (`java.lang.String`)

## Methods

### `public java.lang.String content()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.String name()`

### `public static io.casehub.api.spi.judgment.Evidence of(java.lang.String name, io.casehub.api.spi.judgment.EvidenceType type, java.lang.String content)`

#### Parameters

- `name` (`java.lang.String`)
- `type` (`io.casehub.api.spi.judgment.EvidenceType`)
- `content` (`java.lang.String`)

### `public java.lang.String ref()`

### `public final java.lang.String toString()`

### `public io.casehub.api.spi.judgment.EvidenceType type()`
