# io.casehub.api.model.stigmergy.ProvisioningRequest

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `preferredModelId` (`java.lang.String`)

### `reason` (`java.lang.String`)

### `requestedAt` (`java.time.Instant`)

### `requestingAgentId` (`java.lang.String`)

### `requiredCapabilities` (`java.util.Set<java.lang.String>`)

## Record Components

### `preferredModelId` (`java.lang.String`)

### `reason` (`java.lang.String`)

### `requestedAt` (`java.time.Instant`)

### `requestingAgentId` (`java.lang.String`)

### `requiredCapabilities` (`java.util.Set<java.lang.String>`)

## Constructors

### `public ProvisioningRequest(java.util.Set<java.lang.String> requiredCapabilities, java.lang.String preferredModelId, java.lang.String reason, java.lang.String requestingAgentId, java.time.Instant requestedAt)`

#### Parameters

- `requiredCapabilities` (`java.util.Set<java.lang.String>`)
- `preferredModelId` (`java.lang.String`)
- `reason` (`java.lang.String`)
- `requestingAgentId` (`java.lang.String`)
- `requestedAt` (`java.time.Instant`)

## Methods

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.String preferredModelId()`

### `public java.lang.String reason()`

### `public java.time.Instant requestedAt()`

### `public java.lang.String requestingAgentId()`

### `public java.util.Set<java.lang.String> requiredCapabilities()`

### `public final java.lang.String toString()`
