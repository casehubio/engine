# io.casehub.api.spi.stigmergy.SwarmProvisioningAdvisor.ProvisioningAdvice

**Package:** `io.casehub.api.spi.stigmergy`

**Kind:** `record`

## Fields

### `adjustedCapabilities` (`java.util.Set<java.lang.String>`)

### `adjustedPolicy` (`io.casehub.api.model.stigmergy.IntegrationPolicy`)

### `reasoning` (`java.lang.String`)

### `shouldProvision` (`boolean`)

## Record Components

### `adjustedCapabilities` (`java.util.Set<java.lang.String>`)

### `adjustedPolicy` (`io.casehub.api.model.stigmergy.IntegrationPolicy`)

### `reasoning` (`java.lang.String`)

### `shouldProvision` (`boolean`)

## Constructors

### `public ProvisioningAdvice(boolean shouldProvision, java.util.Set<java.lang.String> adjustedCapabilities, io.casehub.api.model.stigmergy.IntegrationPolicy adjustedPolicy, java.lang.String reasoning)`

#### Parameters

- `shouldProvision` (`boolean`)
- `adjustedCapabilities` (`java.util.Set<java.lang.String>`)
- `adjustedPolicy` (`io.casehub.api.model.stigmergy.IntegrationPolicy`)
- `reasoning` (`java.lang.String`)

## Methods

### `public java.util.Set<java.lang.String> adjustedCapabilities()`

### `public io.casehub.api.model.stigmergy.IntegrationPolicy adjustedPolicy()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.lang.String reasoning()`

### `public boolean shouldProvision()`

### `public final java.lang.String toString()`
