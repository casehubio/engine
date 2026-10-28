# io.casehub.api.view.PlanItemView

**Package:** `io.casehub.api.view`

**Kind:** `record`

## Fields

### `capabilityName` (`java.lang.String`)

### `id` (`java.util.UUID`)

### `name` (`java.lang.String`)

### `parentId` (`java.util.UUID`)

### `status` (`io.casehub.api.model.TaskStatus`)

### `subCaseIds` (`java.util.List<java.util.UUID>`)

### `type` (`java.lang.String`)

## Record Components

### `capabilityName` (`java.lang.String`)

### `id` (`java.util.UUID`)

### `name` (`java.lang.String`)

### `parentId` (`java.util.UUID`)

### `status` (`io.casehub.api.model.TaskStatus`)

### `subCaseIds` (`java.util.List<java.util.UUID>`)

### `type` (`java.lang.String`)

## Constructors

### `public PlanItemView(java.util.UUID id, java.lang.String name, io.casehub.api.model.TaskStatus status, java.lang.String type, java.util.UUID parentId, java.lang.String capabilityName, java.util.List<java.util.UUID> subCaseIds)`

#### Parameters

- `id` (`java.util.UUID`)
- `name` (`java.lang.String`)
- `status` (`io.casehub.api.model.TaskStatus`)
- `type` (`java.lang.String`)
- `parentId` (`java.util.UUID`)
- `capabilityName` (`java.lang.String`)
- `subCaseIds` (`java.util.List<java.util.UUID>`)

## Methods

### `public java.lang.String capabilityName()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.UUID id()`

### `public java.lang.String name()`

### `public java.util.UUID parentId()`

### `public io.casehub.api.model.TaskStatus status()`

### `public java.util.List<java.util.UUID> subCaseIds()`

### `public final java.lang.String toString()`

### `public java.lang.String type()`
