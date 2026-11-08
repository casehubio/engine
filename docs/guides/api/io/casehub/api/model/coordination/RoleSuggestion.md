# io.casehub.api.model.coordination.RoleSuggestion

**Package:** `io.casehub.api.model.coordination`

**Kind:** `record`

## Fields

### `confidence` (`double`)

### `source` (`io.casehub.api.model.stigmergy.DetectedRole`)

### `suggestedName` (`java.lang.String`)

### `suggestedPlatformRole` (`io.casehub.api.model.coordination.PlatformRole`)

### `suggestedStructuralRole` (`io.casehub.api.model.coordination.StructuralRole`)

## Record Components

### `confidence` (`double`)

### `source` (`io.casehub.api.model.stigmergy.DetectedRole`)

### `suggestedName` (`java.lang.String`)

### `suggestedPlatformRole` (`io.casehub.api.model.coordination.PlatformRole`)

### `suggestedStructuralRole` (`io.casehub.api.model.coordination.StructuralRole`)

## Constructors

### `public RoleSuggestion(java.lang.String suggestedName, io.casehub.api.model.coordination.StructuralRole suggestedStructuralRole, io.casehub.api.model.coordination.PlatformRole suggestedPlatformRole, double confidence, io.casehub.api.model.stigmergy.DetectedRole source)`

#### Parameters

- `suggestedName` (`java.lang.String`)
- `suggestedStructuralRole` (`io.casehub.api.model.coordination.StructuralRole`)
- `suggestedPlatformRole` (`io.casehub.api.model.coordination.PlatformRole`)
- `confidence` (`double`)
- `source` (`io.casehub.api.model.stigmergy.DetectedRole`)

## Methods

### `public double confidence()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public io.casehub.api.model.stigmergy.DetectedRole source()`

### `public java.lang.String suggestedName()`

### `public io.casehub.api.model.coordination.PlatformRole suggestedPlatformRole()`

### `public io.casehub.api.model.coordination.StructuralRole suggestedStructuralRole()`

### `public final java.lang.String toString()`
