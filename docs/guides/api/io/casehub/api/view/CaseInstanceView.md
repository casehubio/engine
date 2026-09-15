# io.casehub.api.view.CaseInstanceView

**Package:** `io.casehub.api.view`

**Kind:** `record`

## Fields

### `actorId` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `createdAt` (`java.time.Instant`)

### `name` (`java.lang.String`)

### `namespace` (`java.lang.String`)

### `status` (`io.casehub.api.model.CaseStatus`)

### `version` (`java.lang.String`)

## Record Components

### `actorId` (`java.lang.String`)

### `caseId` (`java.util.UUID`)

### `createdAt` (`java.time.Instant`)

### `name` (`java.lang.String`)

### `namespace` (`java.lang.String`)

### `status` (`io.casehub.api.model.CaseStatus`)

### `version` (`java.lang.String`)

## Constructors

### `public CaseInstanceView(java.util.UUID caseId, io.casehub.api.model.CaseStatus status, java.lang.String namespace, java.lang.String name, java.lang.String version, java.time.Instant createdAt, java.lang.String actorId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `status` (`io.casehub.api.model.CaseStatus`)
- `namespace` (`java.lang.String`)
- `name` (`java.lang.String`)
- `version` (`java.lang.String`)
- `createdAt` (`java.time.Instant`)
- `actorId` (`java.lang.String`)

## Methods

### `public java.lang.String actorId()`

### `public java.util.UUID caseId()`

### `public java.time.Instant createdAt()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.String name()`

### `public java.lang.String namespace()`

### `public io.casehub.api.model.CaseStatus status()`

### `public final java.lang.String toString()`

### `public java.lang.String version()`
