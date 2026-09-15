# io.casehub.api.spi.routing.EnsembleConsensus

**Package:** `io.casehub.api.spi.routing`

**Kind:** `record`

## Fields

### `ensembleConfidence` (`double`)

### `inputCount` (`int`)

### `scope` (`io.casehub.api.spi.routing.ConsensusScope`)

### `sourceCaseIds` (`java.util.List<java.lang.String>`)

### `stepAnalysis` (`java.util.List<io.casehub.api.spi.routing.StepConsensusEntry>`)

## Record Components

### `ensembleConfidence` (`double`)

### `inputCount` (`int`)

### `scope` (`io.casehub.api.spi.routing.ConsensusScope`)

### `sourceCaseIds` (`java.util.List<java.lang.String>`)

### `stepAnalysis` (`java.util.List<io.casehub.api.spi.routing.StepConsensusEntry>`)

## Constructors

### `public EnsembleConsensus(io.casehub.api.spi.routing.ConsensusScope scope, java.util.List<io.casehub.api.spi.routing.StepConsensusEntry> stepAnalysis, double ensembleConfidence, int inputCount, java.util.List<java.lang.String> sourceCaseIds)`

#### Parameters

- `scope` (`io.casehub.api.spi.routing.ConsensusScope`)
- `stepAnalysis` (`java.util.List<io.casehub.api.spi.routing.StepConsensusEntry>`)
- `ensembleConfidence` (`double`)
- `inputCount` (`int`)
- `sourceCaseIds` (`java.util.List<java.lang.String>`)

## Methods

### `public double ensembleConfidence()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public int inputCount()`

### `public io.casehub.api.spi.routing.ConsensusScope scope()`

### `public java.util.List<java.lang.String> sourceCaseIds()`

### `public java.util.List<io.casehub.api.spi.routing.StepConsensusEntry> stepAnalysis()`

### `public final java.lang.String toString()`
