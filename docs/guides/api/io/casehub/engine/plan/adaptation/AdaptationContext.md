# io.casehub.engine.plan.adaptation.AdaptationContext

**Package:** `io.casehub.engine.plan.adaptation`

**Kind:** `record`

## Fields

### `adaptationGeneration` (`int`)

### `caseId` (`java.util.UUID`)

### `completedSteps` (`java.util.List<io.casehub.engine.plan.adaptation.CompletedStep>`)

### `compoundId` (`java.lang.String`)

### `currentContext` (`JsonNode`)

### `definition` (`io.casehub.api.model.CaseDefinition`)

### `goalName` (`java.lang.String`)

### `latestBindingName` (`java.lang.String`)

### `latestStatus` (`io.casehub.api.model.TaskStatus`)

### `pendingSteps` (`java.util.List<io.casehub.engine.plan.adaptation.PlanStepDescriptor>`)

### `runningSteps` (`java.util.List<io.casehub.engine.plan.adaptation.PlanStepDescriptor>`)

### `tenancyId` (`java.lang.String`)

## Record Components

### `adaptationGeneration` (`int`)

### `caseId` (`java.util.UUID`)

### `completedSteps` (`java.util.List<io.casehub.engine.plan.adaptation.CompletedStep>`)

### `compoundId` (`java.lang.String`)

### `currentContext` (`JsonNode`)

### `definition` (`io.casehub.api.model.CaseDefinition`)

### `goalName` (`java.lang.String`)

### `latestBindingName` (`java.lang.String`)

### `latestStatus` (`io.casehub.api.model.TaskStatus`)

### `pendingSteps` (`java.util.List<io.casehub.engine.plan.adaptation.PlanStepDescriptor>`)

### `runningSteps` (`java.util.List<io.casehub.engine.plan.adaptation.PlanStepDescriptor>`)

### `tenancyId` (`java.lang.String`)

## Constructors

### `public AdaptationContext(java.util.UUID caseId, java.lang.String tenancyId, java.lang.String compoundId, java.lang.String goalName, java.util.List<io.casehub.engine.plan.adaptation.CompletedStep> completedSteps, java.util.List<io.casehub.engine.plan.adaptation.PlanStepDescriptor> pendingSteps, java.util.List<io.casehub.engine.plan.adaptation.PlanStepDescriptor> runningSteps, JsonNode currentContext, io.casehub.api.model.CaseDefinition definition, io.casehub.api.model.TaskStatus latestStatus, java.lang.String latestBindingName, int adaptationGeneration)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `compoundId` (`java.lang.String`)
- `goalName` (`java.lang.String`)
- `completedSteps` (`java.util.List<io.casehub.engine.plan.adaptation.CompletedStep>`)
- `pendingSteps` (`java.util.List<io.casehub.engine.plan.adaptation.PlanStepDescriptor>`)
- `runningSteps` (`java.util.List<io.casehub.engine.plan.adaptation.PlanStepDescriptor>`)
- `currentContext` (`JsonNode`)
- `definition` (`io.casehub.api.model.CaseDefinition`)
- `latestStatus` (`io.casehub.api.model.TaskStatus`)
- `latestBindingName` (`java.lang.String`)
- `adaptationGeneration` (`int`)

## Methods

### `public int adaptationGeneration()`

### `public java.util.UUID caseId()`

### `public java.util.List<io.casehub.engine.plan.adaptation.CompletedStep> completedSteps()`

### `public java.lang.String compoundId()`

### `public JsonNode currentContext()`

### `public io.casehub.api.model.CaseDefinition definition()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.String goalName()`

### `public final int hashCode()`

### `public java.lang.String latestBindingName()`

### `public io.casehub.api.model.TaskStatus latestStatus()`

### `public java.util.List<io.casehub.engine.plan.adaptation.PlanStepDescriptor> pendingSteps()`

### `public java.util.List<io.casehub.engine.plan.adaptation.PlanStepDescriptor> runningSteps()`

### `public java.lang.String tenancyId()`

### `public final java.lang.String toString()`
