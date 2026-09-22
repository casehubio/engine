# io.casehub.api.model.stigmergy.ReadinessReport

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `areas` (`java.util.List<io.casehub.api.model.stigmergy.ReadinessReport.AreaCompliance>`)

### `evaluatedAt` (`java.time.Instant`)

### `passed` (`boolean`)

### `projectLevel` (`io.casehub.api.model.stigmergy.ComplianceLevel`)

### `targetLevel` (`io.casehub.api.model.stigmergy.ComplianceLevel`)

## Record Components

### `areas` (`java.util.List<io.casehub.api.model.stigmergy.ReadinessReport.AreaCompliance>`)

### `evaluatedAt` (`java.time.Instant`)

### `passed` (`boolean`)

### `projectLevel` (`io.casehub.api.model.stigmergy.ComplianceLevel`)

### `targetLevel` (`io.casehub.api.model.stigmergy.ComplianceLevel`)

## Constructors

### `public ReadinessReport(io.casehub.api.model.stigmergy.ComplianceLevel targetLevel, io.casehub.api.model.stigmergy.ComplianceLevel projectLevel, java.util.List<io.casehub.api.model.stigmergy.ReadinessReport.AreaCompliance> areas, boolean passed, java.time.Instant evaluatedAt)`

#### Parameters

- `targetLevel` (`io.casehub.api.model.stigmergy.ComplianceLevel`)
- `projectLevel` (`io.casehub.api.model.stigmergy.ComplianceLevel`)
- `areas` (`java.util.List<io.casehub.api.model.stigmergy.ReadinessReport.AreaCompliance>`)
- `passed` (`boolean`)
- `evaluatedAt` (`java.time.Instant`)

## Methods

### `public java.util.List<io.casehub.api.model.stigmergy.ReadinessReport.AreaCompliance> areas()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.time.Instant evaluatedAt()`

### `public final int hashCode()`

### `public boolean passed()`

### `public io.casehub.api.model.stigmergy.ComplianceLevel projectLevel()`

### `public io.casehub.api.model.stigmergy.ComplianceLevel targetLevel()`

### `public final java.lang.String toString()`
