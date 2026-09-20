# io.casehub.api.model.stigmergy.SwarmConfig

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `detectionInterval` (`java.lang.Integer`)

### `domainWeights` (`io.casehub.api.model.stigmergy.RoleDomainWeights`)

### `integrationPolicy` (`io.casehub.api.model.stigmergy.IntegrationPolicy`)

### `maxSwarmSize` (`java.lang.Integer`)

### `progressChangeThreshold` (`java.lang.Double`)

### `provisionBudget` (`io.casehub.api.model.stigmergy.ProvisionBudget`)

### `roleDetectionWindow` (`java.lang.Integer`)

### `roleMinClusterSize` (`java.lang.Integer`)

### `roleSimilarityThreshold` (`java.lang.Double`)

### `teamAffinityThreshold` (`java.lang.Double`)

### `teamMinSize` (`java.lang.Integer`)

## Record Components

### `detectionInterval` (`java.lang.Integer`)

### `domainWeights` (`io.casehub.api.model.stigmergy.RoleDomainWeights`)

### `integrationPolicy` (`io.casehub.api.model.stigmergy.IntegrationPolicy`)

### `maxSwarmSize` (`java.lang.Integer`)

### `progressChangeThreshold` (`java.lang.Double`)

### `provisionBudget` (`io.casehub.api.model.stigmergy.ProvisionBudget`)

### `roleDetectionWindow` (`java.lang.Integer`)

### `roleMinClusterSize` (`java.lang.Integer`)

### `roleSimilarityThreshold` (`java.lang.Double`)

### `teamAffinityThreshold` (`java.lang.Double`)

### `teamMinSize` (`java.lang.Integer`)

## Constructors

### `public SwarmConfig(java.lang.Integer maxSwarmSize, java.lang.Double roleSimilarityThreshold, java.lang.Integer roleMinClusterSize, java.lang.Integer roleDetectionWindow, java.lang.Integer detectionInterval, io.casehub.api.model.stigmergy.RoleDomainWeights domainWeights, java.lang.Double teamAffinityThreshold, java.lang.Integer teamMinSize, java.lang.Double progressChangeThreshold)`

#### Parameters

- `maxSwarmSize` (`java.lang.Integer`)
- `roleSimilarityThreshold` (`java.lang.Double`)
- `roleMinClusterSize` (`java.lang.Integer`)
- `roleDetectionWindow` (`java.lang.Integer`)
- `detectionInterval` (`java.lang.Integer`)
- `domainWeights` (`io.casehub.api.model.stigmergy.RoleDomainWeights`)
- `teamAffinityThreshold` (`java.lang.Double`)
- `teamMinSize` (`java.lang.Integer`)
- `progressChangeThreshold` (`java.lang.Double`)

### `public SwarmConfig(java.lang.Integer maxSwarmSize, java.lang.Double roleSimilarityThreshold, java.lang.Integer roleMinClusterSize, java.lang.Integer roleDetectionWindow, java.lang.Integer detectionInterval, io.casehub.api.model.stigmergy.RoleDomainWeights domainWeights, java.lang.Double teamAffinityThreshold, java.lang.Integer teamMinSize, java.lang.Double progressChangeThreshold, io.casehub.api.model.stigmergy.ProvisionBudget provisionBudget, io.casehub.api.model.stigmergy.IntegrationPolicy integrationPolicy)`

#### Parameters

- `maxSwarmSize` (`java.lang.Integer`)
- `roleSimilarityThreshold` (`java.lang.Double`)
- `roleMinClusterSize` (`java.lang.Integer`)
- `roleDetectionWindow` (`java.lang.Integer`)
- `detectionInterval` (`java.lang.Integer`)
- `domainWeights` (`io.casehub.api.model.stigmergy.RoleDomainWeights`)
- `teamAffinityThreshold` (`java.lang.Double`)
- `teamMinSize` (`java.lang.Integer`)
- `progressChangeThreshold` (`java.lang.Double`)
- `provisionBudget` (`io.casehub.api.model.stigmergy.ProvisionBudget`)
- `integrationPolicy` (`io.casehub.api.model.stigmergy.IntegrationPolicy`)

## Methods

### `public java.lang.Integer detectionInterval()`

### `public io.casehub.api.model.stigmergy.RoleDomainWeights domainWeights()`

### `public int effectiveDetectionInterval()`

### `public io.casehub.api.model.stigmergy.RoleDomainWeights effectiveDomainWeights()`

### `public int effectiveMaxSwarmSize()`

### `public double effectiveProgressChangeThreshold()`

### `public int effectiveRoleDetectionWindow()`

### `public int effectiveRoleMinClusterSize()`

### `public double effectiveRoleSimilarityThreshold()`

### `public double effectiveTeamAffinityThreshold()`

### `public int effectiveTeamMinSize()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public io.casehub.api.model.stigmergy.IntegrationPolicy integrationPolicy()`

### `public java.lang.Integer maxSwarmSize()`

### `public java.lang.Double progressChangeThreshold()`

### `public io.casehub.api.model.stigmergy.ProvisionBudget provisionBudget()`

### `public java.lang.Integer roleDetectionWindow()`

### `public java.lang.Integer roleMinClusterSize()`

### `public java.lang.Double roleSimilarityThreshold()`

### `public java.lang.Double teamAffinityThreshold()`

### `public java.lang.Integer teamMinSize()`

### `public final java.lang.String toString()`
