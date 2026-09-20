# io.casehub.api.spi.stigmergy.SwarmProvisioningAdvisor.ProvisioningContext

**Package:** `io.casehub.api.spi.stigmergy`

**Kind:** `record`

## Fields

### `caseId` (`java.util.UUID`)

### `currentPolicy` (`io.casehub.api.model.stigmergy.IntegrationPolicy`)

### `requestedCapabilities` (`java.util.Set<java.lang.String>`)

### `swarmState` (`io.casehub.api.model.stigmergy.SwarmBootstrapContext`)

## Record Components

### `caseId` (`java.util.UUID`)

### `currentPolicy` (`io.casehub.api.model.stigmergy.IntegrationPolicy`)

### `requestedCapabilities` (`java.util.Set<java.lang.String>`)

### `swarmState` (`io.casehub.api.model.stigmergy.SwarmBootstrapContext`)

## Constructors

### `public ProvisioningContext(java.util.UUID caseId, io.casehub.api.model.stigmergy.SwarmBootstrapContext swarmState, java.util.Set<java.lang.String> requestedCapabilities, io.casehub.api.model.stigmergy.IntegrationPolicy currentPolicy)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `swarmState` (`io.casehub.api.model.stigmergy.SwarmBootstrapContext`)
- `requestedCapabilities` (`java.util.Set<java.lang.String>`)
- `currentPolicy` (`io.casehub.api.model.stigmergy.IntegrationPolicy`)

## Methods

### `public java.util.UUID caseId()`

### `public io.casehub.api.model.stigmergy.IntegrationPolicy currentPolicy()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.Set<java.lang.String> requestedCapabilities()`

### `public io.casehub.api.model.stigmergy.SwarmBootstrapContext swarmState()`

### `public final java.lang.String toString()`
