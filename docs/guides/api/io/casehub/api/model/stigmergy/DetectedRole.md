# io.casehub.api.model.stigmergy.DetectedRole

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `centroid` (`io.casehub.api.model.stigmergy.BehavioralFingerprint`)

### `centroidDrift` (`double`)

### `dominantFeatures` (`java.util.List<java.lang.String>`)

### `memberAgents` (`java.util.Set<java.lang.String>`)

### `roleId` (`java.lang.String`)

### `stabilityCount` (`int`)

## Record Components

### `centroid` (`io.casehub.api.model.stigmergy.BehavioralFingerprint`)

### `centroidDrift` (`double`)

### `dominantFeatures` (`java.util.List<java.lang.String>`)

### `memberAgents` (`java.util.Set<java.lang.String>`)

### `roleId` (`java.lang.String`)

### `stabilityCount` (`int`)

## Constructors

### `public DetectedRole(java.lang.String roleId, java.util.Set<java.lang.String> memberAgents, io.casehub.api.model.stigmergy.BehavioralFingerprint centroid, java.util.List<java.lang.String> dominantFeatures, int stabilityCount, double centroidDrift)`

#### Parameters

- `roleId` (`java.lang.String`)
- `memberAgents` (`java.util.Set<java.lang.String>`)
- `centroid` (`io.casehub.api.model.stigmergy.BehavioralFingerprint`)
- `dominantFeatures` (`java.util.List<java.lang.String>`)
- `stabilityCount` (`int`)
- `centroidDrift` (`double`)

## Methods

### `public io.casehub.api.model.stigmergy.BehavioralFingerprint centroid()`

### `public double centroidDrift()`

### `public java.util.List<java.lang.String> dominantFeatures()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.Set<java.lang.String> memberAgents()`

### `public java.lang.String roleId()`

### `public int stabilityCount()`

### `public final java.lang.String toString()`
