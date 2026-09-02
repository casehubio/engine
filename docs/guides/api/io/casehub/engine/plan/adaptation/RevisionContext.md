# io.casehub.engine.plan.adaptation.RevisionContext

**Package:** `io.casehub.engine.plan.adaptation`

**Kind:** `record`

## Fields

### `adaptationContext` (`io.casehub.engine.plan.adaptation.AdaptationContext`)

### `capabilities` (`java.util.List<Capability>`)

### `cause` (`io.casehub.engine.plan.adaptation.AdaptationCause`)

### `experiences` (`java.util.List<io.casehub.api.spi.routing.RetrievedExperience>`)

### `memories` (`java.util.List<io.casehub.api.model.RetrievedMemory>`)

## Record Components

### `adaptationContext` (`io.casehub.engine.plan.adaptation.AdaptationContext`)

### `capabilities` (`java.util.List<Capability>`)

### `cause` (`io.casehub.engine.plan.adaptation.AdaptationCause`)

### `experiences` (`java.util.List<io.casehub.api.spi.routing.RetrievedExperience>`)

### `memories` (`java.util.List<io.casehub.api.model.RetrievedMemory>`)

## Constructors

### `public RevisionContext(io.casehub.engine.plan.adaptation.AdaptationContext adaptationContext, io.casehub.engine.plan.adaptation.AdaptationCause cause, java.util.List<Capability> capabilities, java.util.List<io.casehub.api.model.RetrievedMemory> memories)`

#### Parameters

- `adaptationContext` (`io.casehub.engine.plan.adaptation.AdaptationContext`)
- `cause` (`io.casehub.engine.plan.adaptation.AdaptationCause`)
- `capabilities` (`java.util.List<Capability>`)
- `memories` (`java.util.List<io.casehub.api.model.RetrievedMemory>`)

### `public RevisionContext(io.casehub.engine.plan.adaptation.AdaptationContext adaptationContext, io.casehub.engine.plan.adaptation.AdaptationCause cause, java.util.List<Capability> capabilities, java.util.List<io.casehub.api.model.RetrievedMemory> memories, java.util.List<io.casehub.api.spi.routing.RetrievedExperience> experiences)`

#### Parameters

- `adaptationContext` (`io.casehub.engine.plan.adaptation.AdaptationContext`)
- `cause` (`io.casehub.engine.plan.adaptation.AdaptationCause`)
- `capabilities` (`java.util.List<Capability>`)
- `memories` (`java.util.List<io.casehub.api.model.RetrievedMemory>`)
- `experiences` (`java.util.List<io.casehub.api.spi.routing.RetrievedExperience>`)

## Methods

### `public io.casehub.engine.plan.adaptation.AdaptationContext adaptationContext()`

### `public java.util.List<Capability> capabilities()`

### `public io.casehub.engine.plan.adaptation.AdaptationCause cause()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.List<io.casehub.api.spi.routing.RetrievedExperience> experiences()`

### `public final int hashCode()`

### `public java.util.List<io.casehub.api.model.RetrievedMemory> memories()`

### `public final java.lang.String toString()`
