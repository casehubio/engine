# io.casehub.api.model.stigmergy.SwarmBootstrapContext

**Package:** `io.casehub.api.model.stigmergy`

**Kind:** `record`

## Fields

### `activeInterestKeys` (`java.util.Set<java.lang.String>`)

### `activeSignals` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `currentRoles` (`java.util.List<io.casehub.api.model.stigmergy.DetectedRole>`)

### `currentTeams` (`java.util.List<io.casehub.api.model.stigmergy.DetectedTeam>`)

### `policy` (`io.casehub.api.model.stigmergy.IntegrationPolicy`)

### `swarmProgress` (`io.casehub.api.model.stigmergy.SwarmProgress`)

### `triggeringRequest` (`io.casehub.api.model.stigmergy.ProvisioningRequest`)

## Record Components

### `activeInterestKeys` (`java.util.Set<java.lang.String>`)

### `activeSignals` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `currentRoles` (`java.util.List<io.casehub.api.model.stigmergy.DetectedRole>`)

### `currentTeams` (`java.util.List<io.casehub.api.model.stigmergy.DetectedTeam>`)

### `policy` (`io.casehub.api.model.stigmergy.IntegrationPolicy`)

### `swarmProgress` (`io.casehub.api.model.stigmergy.SwarmProgress`)

### `triggeringRequest` (`io.casehub.api.model.stigmergy.ProvisioningRequest`)

## Constructors

### `public SwarmBootstrapContext(java.util.Map<java.lang.String,java.lang.Double> activeSignals, java.util.Set<java.lang.String> activeInterestKeys, java.util.List<io.casehub.api.model.stigmergy.DetectedRole> currentRoles, java.util.List<io.casehub.api.model.stigmergy.DetectedTeam> currentTeams, io.casehub.api.model.stigmergy.SwarmProgress swarmProgress, io.casehub.api.model.stigmergy.ProvisioningRequest triggeringRequest, io.casehub.api.model.stigmergy.IntegrationPolicy policy)`

#### Parameters

- `activeSignals` (`java.util.Map<java.lang.String,java.lang.Double>`)
- `activeInterestKeys` (`java.util.Set<java.lang.String>`)
- `currentRoles` (`java.util.List<io.casehub.api.model.stigmergy.DetectedRole>`)
- `currentTeams` (`java.util.List<io.casehub.api.model.stigmergy.DetectedTeam>`)
- `swarmProgress` (`io.casehub.api.model.stigmergy.SwarmProgress`)
- `triggeringRequest` (`io.casehub.api.model.stigmergy.ProvisioningRequest`)
- `policy` (`io.casehub.api.model.stigmergy.IntegrationPolicy`)

## Methods

### `public java.util.Set<java.lang.String> activeInterestKeys()`

### `public java.util.Map<java.lang.String,java.lang.Double> activeSignals()`

### `public java.util.List<io.casehub.api.model.stigmergy.DetectedRole> currentRoles()`

### `public java.util.List<io.casehub.api.model.stigmergy.DetectedTeam> currentTeams()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public io.casehub.api.model.stigmergy.IntegrationPolicy policy()`

### `public io.casehub.api.model.stigmergy.SwarmProgress swarmProgress()`

### `public final java.lang.String toString()`

### `public io.casehub.api.model.stigmergy.ProvisioningRequest triggeringRequest()`
