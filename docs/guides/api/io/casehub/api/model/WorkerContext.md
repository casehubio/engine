# io.casehub.api.model.WorkerContext

**Package:** `io.casehub.api.model`

**Kind:** `record`

## Fields

### `caseId` (`java.util.UUID`)

### `channels` (`java.util.List<io.casehub.api.model.CaseChannel>`)

### `experiences` (`java.util.List<io.casehub.api.spi.routing.RetrievedExperience>`)

### `failureDiagnoses` (`java.util.List<io.casehub.api.model.FailureDiagnosis>`)

### `memories` (`java.util.List<io.casehub.api.model.RetrievedMemory>`)

### `priorWorkers` (`java.util.List<io.casehub.api.model.WorkerSummary>`)

### `propagationContext` (`io.casehub.api.context.PropagationContext`)

### `properties` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `taskDescription` (`java.lang.String`)

## Record Components

### `caseId` (`java.util.UUID`)

### `channels` (`java.util.List<io.casehub.api.model.CaseChannel>`)

### `experiences` (`java.util.List<io.casehub.api.spi.routing.RetrievedExperience>`)

### `failureDiagnoses` (`java.util.List<io.casehub.api.model.FailureDiagnosis>`)

### `memories` (`java.util.List<io.casehub.api.model.RetrievedMemory>`)

### `priorWorkers` (`java.util.List<io.casehub.api.model.WorkerSummary>`)

### `propagationContext` (`io.casehub.api.context.PropagationContext`)

### `properties` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `taskDescription` (`java.lang.String`)

## Constructors

### `public WorkerContext(java.lang.String taskDescription, java.util.UUID caseId, java.util.List<io.casehub.api.model.CaseChannel> channels, java.util.List<io.casehub.api.model.WorkerSummary> priorWorkers, io.casehub.api.context.PropagationContext propagationContext, java.util.Map<java.lang.String,java.lang.Object> properties)`

#### Parameters

- `taskDescription` (`java.lang.String`)
- `caseId` (`java.util.UUID`)
- `channels` (`java.util.List<io.casehub.api.model.CaseChannel>`)
- `priorWorkers` (`java.util.List<io.casehub.api.model.WorkerSummary>`)
- `propagationContext` (`io.casehub.api.context.PropagationContext`)
- `properties` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `public WorkerContext(java.lang.String taskDescription, java.util.UUID caseId, java.util.List<io.casehub.api.model.CaseChannel> channels, java.util.List<io.casehub.api.model.WorkerSummary> priorWorkers, io.casehub.api.context.PropagationContext propagationContext, java.util.Map<java.lang.String,java.lang.Object> properties, java.util.List<io.casehub.api.spi.routing.RetrievedExperience> experiences)`

#### Parameters

- `taskDescription` (`java.lang.String`)
- `caseId` (`java.util.UUID`)
- `channels` (`java.util.List<io.casehub.api.model.CaseChannel>`)
- `priorWorkers` (`java.util.List<io.casehub.api.model.WorkerSummary>`)
- `propagationContext` (`io.casehub.api.context.PropagationContext`)
- `properties` (`java.util.Map<java.lang.String,java.lang.Object>`)
- `experiences` (`java.util.List<io.casehub.api.spi.routing.RetrievedExperience>`)

### `public WorkerContext(java.lang.String taskDescription, java.util.UUID caseId, java.util.List<io.casehub.api.model.CaseChannel> channels, java.util.List<io.casehub.api.model.WorkerSummary> priorWorkers, io.casehub.api.context.PropagationContext propagationContext, java.util.Map<java.lang.String,java.lang.Object> properties, java.util.List<io.casehub.api.spi.routing.RetrievedExperience> experiences, java.util.List<io.casehub.api.model.RetrievedMemory> memories)`

#### Parameters

- `taskDescription` (`java.lang.String`)
- `caseId` (`java.util.UUID`)
- `channels` (`java.util.List<io.casehub.api.model.CaseChannel>`)
- `priorWorkers` (`java.util.List<io.casehub.api.model.WorkerSummary>`)
- `propagationContext` (`io.casehub.api.context.PropagationContext`)
- `properties` (`java.util.Map<java.lang.String,java.lang.Object>`)
- `experiences` (`java.util.List<io.casehub.api.spi.routing.RetrievedExperience>`)
- `memories` (`java.util.List<io.casehub.api.model.RetrievedMemory>`)

### `public WorkerContext(java.lang.String taskDescription, java.util.UUID caseId, java.util.List<io.casehub.api.model.CaseChannel> channels, java.util.List<io.casehub.api.model.WorkerSummary> priorWorkers, io.casehub.api.context.PropagationContext propagationContext, java.util.Map<java.lang.String,java.lang.Object> properties, java.util.List<io.casehub.api.spi.routing.RetrievedExperience> experiences, java.util.List<io.casehub.api.model.RetrievedMemory> memories, java.util.List<io.casehub.api.model.FailureDiagnosis> failureDiagnoses)`

#### Parameters

- `taskDescription` (`java.lang.String`)
- `caseId` (`java.util.UUID`)
- `channels` (`java.util.List<io.casehub.api.model.CaseChannel>`)
- `priorWorkers` (`java.util.List<io.casehub.api.model.WorkerSummary>`)
- `propagationContext` (`io.casehub.api.context.PropagationContext`)
- `properties` (`java.util.Map<java.lang.String,java.lang.Object>`)
- `experiences` (`java.util.List<io.casehub.api.spi.routing.RetrievedExperience>`)
- `memories` (`java.util.List<io.casehub.api.model.RetrievedMemory>`)
- `failureDiagnoses` (`java.util.List<io.casehub.api.model.FailureDiagnosis>`)

## Methods

### `public java.util.UUID caseId()`

### `public java.util.List<io.casehub.api.model.CaseChannel> channels()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.List<io.casehub.api.spi.routing.RetrievedExperience> experiences()`

### `public java.util.List<io.casehub.api.model.FailureDiagnosis> failureDiagnoses()`

### `public final int hashCode()`

### `public java.util.List<io.casehub.api.model.RetrievedMemory> memories()`

### `public java.util.List<io.casehub.api.model.WorkerSummary> priorWorkers()`

### `public io.casehub.api.context.PropagationContext propagationContext()`

### `public java.util.Map<java.lang.String,java.lang.Object> properties()`

### `public java.lang.String taskDescription()`

### `public final java.lang.String toString()`
