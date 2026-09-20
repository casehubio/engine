# io.casehub.api.model.stigmergy.BehavioralFingerprint

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `EMPTY` (`io.casehub.api.model.stigmergy.BehavioralFingerprint`)

### `communication` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `decision` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `effect` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `perception` (`java.util.Map<java.lang.String,java.lang.Double>`)

## Record Components

### `communication` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `decision` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `effect` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `perception` (`java.util.Map<java.lang.String,java.lang.Double>`)

## Constructors

### `public BehavioralFingerprint(java.util.Map<java.lang.String,java.lang.Double> perception, java.util.Map<java.lang.String,java.lang.Double> communication, java.util.Map<java.lang.String,java.lang.Double> decision, java.util.Map<java.lang.String,java.lang.Double> effect)`

#### Parameters

- `perception` (`java.util.Map<java.lang.String,java.lang.Double>`)
- `communication` (`java.util.Map<java.lang.String,java.lang.Double>`)
- `decision` (`java.util.Map<java.lang.String,java.lang.Double>`)
- `effect` (`java.util.Map<java.lang.String,java.lang.Double>`)

## Methods

### `public java.util.Map<java.lang.String,java.lang.Double> communication()`

### `public static double cosineSimilarity(java.util.Map<java.lang.String,java.lang.Double> a, java.util.Map<java.lang.String,java.lang.Double> b)`

#### Parameters

- `a` (`java.util.Map<java.lang.String,java.lang.Double>`)
- `b` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `public java.util.Map<java.lang.String,java.lang.Double> decision()`

### `public java.util.Map<java.lang.String,java.lang.Double> effect()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.Map<java.lang.String,java.lang.Double> perception()`

### `public final java.lang.String toString()`

### `public double weightedSimilarity(io.casehub.api.model.stigmergy.BehavioralFingerprint other, io.casehub.api.model.stigmergy.RoleDomainWeights weights)`

#### Parameters

- `other` (`io.casehub.api.model.stigmergy.BehavioralFingerprint`)
- `weights` (`io.casehub.api.model.stigmergy.RoleDomainWeights`)
