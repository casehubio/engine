# io.casehub.api.model.improvement.ReadinessReport

**Package:** `io.casehub.api.model.improvement`

**Kind:** `record`

## Fields

### `areas` (`java.util.List<io.casehub.api.model.improvement.ReadinessReport.AreaCompliance>`)

### `evaluatedAt` (`java.time.Instant`)

### `passed` (`boolean`)

### `projectLevel` (`io.casehub.api.model.improvement.ComplianceLevel`)

### `targetLevel` (`io.casehub.api.model.improvement.ComplianceLevel`)

## Record Components

### `areas` (`java.util.List<io.casehub.api.model.improvement.ReadinessReport.AreaCompliance>`)

### `evaluatedAt` (`java.time.Instant`)

### `passed` (`boolean`)

### `projectLevel` (`io.casehub.api.model.improvement.ComplianceLevel`)

### `targetLevel` (`io.casehub.api.model.improvement.ComplianceLevel`)

## Constructors

### `public ReadinessReport(io.casehub.api.model.improvement.ComplianceLevel targetLevel, io.casehub.api.model.improvement.ComplianceLevel projectLevel, java.util.List<io.casehub.api.model.improvement.ReadinessReport.AreaCompliance> areas, boolean passed, java.time.Instant evaluatedAt)`

#### Parameters

- `targetLevel` (`io.casehub.api.model.improvement.ComplianceLevel`)
- `projectLevel` (`io.casehub.api.model.improvement.ComplianceLevel`)
- `areas` (`java.util.List<io.casehub.api.model.improvement.ReadinessReport.AreaCompliance>`)
- `passed` (`boolean`)
- `evaluatedAt` (`java.time.Instant`)

## Methods

### `public java.util.List<io.casehub.api.model.improvement.ReadinessReport.AreaCompliance> areas()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.time.Instant evaluatedAt()`

### `public final int hashCode()`

### `public boolean passed()`

### `public io.casehub.api.model.improvement.ComplianceLevel projectLevel()`

### `public io.casehub.api.model.improvement.ComplianceLevel targetLevel()`

### `public final java.lang.String toString()`
