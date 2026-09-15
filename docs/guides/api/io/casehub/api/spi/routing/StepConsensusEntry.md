# io.casehub.api.spi.routing.StepConsensusEntry

**Package:** `io.casehub.api.spi.routing`

**Kind:** `record`

## Fields

### `agreement` (`io.casehub.api.spi.routing.AgreementLevel`)

### `bindingName` (`java.lang.String`)

### `capabilityName` (`java.lang.String`)

### `contributingCaseIds` (`java.util.List<java.lang.String>`)

### `occurrenceCount` (`int`)

### `outcomeDistribution` (`java.util.Map<java.lang.String,java.lang.Integer>`)

### `priorityDistribution` (`java.util.Map<java.lang.Integer,java.lang.Integer>`)

### `totalPlans` (`int`)

### `workerDistribution` (`java.util.Map<java.lang.String,java.lang.Integer>`)

## Record Components

### `agreement` (`io.casehub.api.spi.routing.AgreementLevel`)

### `bindingName` (`java.lang.String`)

### `capabilityName` (`java.lang.String`)

### `contributingCaseIds` (`java.util.List<java.lang.String>`)

### `occurrenceCount` (`int`)

### `outcomeDistribution` (`java.util.Map<java.lang.String,java.lang.Integer>`)

### `priorityDistribution` (`java.util.Map<java.lang.Integer,java.lang.Integer>`)

### `totalPlans` (`int`)

### `workerDistribution` (`java.util.Map<java.lang.String,java.lang.Integer>`)

## Constructors

### `public StepConsensusEntry(java.lang.String bindingName, java.lang.String capabilityName, int occurrenceCount, int totalPlans, java.util.Map<java.lang.String,java.lang.Integer> workerDistribution, java.util.Map<java.lang.String,java.lang.Integer> outcomeDistribution, java.util.Map<java.lang.Integer,java.lang.Integer> priorityDistribution, java.util.List<java.lang.String> contributingCaseIds, io.casehub.api.spi.routing.AgreementLevel agreement)`

#### Parameters

- `bindingName` (`java.lang.String`)
- `capabilityName` (`java.lang.String`)
- `occurrenceCount` (`int`)
- `totalPlans` (`int`)
- `workerDistribution` (`java.util.Map<java.lang.String,java.lang.Integer>`)
- `outcomeDistribution` (`java.util.Map<java.lang.String,java.lang.Integer>`)
- `priorityDistribution` (`java.util.Map<java.lang.Integer,java.lang.Integer>`)
- `contributingCaseIds` (`java.util.List<java.lang.String>`)
- `agreement` (`io.casehub.api.spi.routing.AgreementLevel`)

## Methods

### `public io.casehub.api.spi.routing.AgreementLevel agreement()`

### `public java.lang.String bindingName()`

### `public java.lang.String capabilityName()`

### `public java.util.List<java.lang.String> contributingCaseIds()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public int occurrenceCount()`

### `public java.util.Map<java.lang.String,java.lang.Integer> outcomeDistribution()`

### `public java.util.Map<java.lang.Integer,java.lang.Integer> priorityDistribution()`

### `public final java.lang.String toString()`

### `public int totalPlans()`

### `public java.util.Map<java.lang.String,java.lang.Integer> workerDistribution()`
