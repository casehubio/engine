# io.casehub.api.spi.routing.CbrRetrievalResult

**Package:** `io.casehub.api.spi.routing`

**Kind:** `record`

## Fields

### `ensemble` (`io.casehub.api.spi.routing.EnsembleConsensus`)

### `experiences` (`java.util.List<io.casehub.api.spi.routing.RetrievedExperience>`)

## Record Components

### `ensemble` (`io.casehub.api.spi.routing.EnsembleConsensus`)

### `experiences` (`java.util.List<io.casehub.api.spi.routing.RetrievedExperience>`)

## Constructors

### `public CbrRetrievalResult(java.util.List<io.casehub.api.spi.routing.RetrievedExperience> experiences, io.casehub.api.spi.routing.EnsembleConsensus ensemble)`

#### Parameters

- `experiences` (`java.util.List<io.casehub.api.spi.routing.RetrievedExperience>`)
- `ensemble` (`io.casehub.api.spi.routing.EnsembleConsensus`)

## Methods

### `public static io.casehub.api.spi.routing.CbrRetrievalResult empty()`

### `public io.casehub.api.spi.routing.EnsembleConsensus ensemble()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.List<io.casehub.api.spi.routing.RetrievedExperience> experiences()`

### `public final int hashCode()`

### `public final java.lang.String toString()`
