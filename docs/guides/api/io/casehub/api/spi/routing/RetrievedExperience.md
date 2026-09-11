# io.casehub.api.spi.routing.RetrievedExperience

**Package:** `io.casehub.api.spi.routing`

**Kind:** `record`

A retrieved case experience from the CBR memory store. Represents a past case with a similar
problem to the current case, including the solution that was applied, the outcome achieved, and
the full plan trace showing which bindings were selected.

## Fields

### `caseId` (`java.lang.String`)

### `caseType` (`java.lang.String`)

### `confidence` (`java.lang.Double`)

### `documentContent` (`java.lang.String`)

### `documentSteps` (`java.util.List<io.casehub.api.spi.routing.DocumentStep>`)

### `featureSimilarities` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `features` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `outcome` (`java.lang.String`)

### `planTrace` (`java.util.List<io.casehub.api.spi.routing.ExperiencePlanStep>`)

### `problem` (`java.lang.String`)

### `similarityScore` (`double`)

### `solution` (`java.lang.String`)

### `sourceType` (`io.casehub.api.spi.routing.ResolutionSourceType`)

## Record Components

### `caseId` (`java.lang.String`)

the CBR case store ID for this experience (nullable — null when unavailable or for
    backward compatibility)

### `caseType` (`java.lang.String`)

the case definition type that produced this experience (nullable — null for
    same-type queries where the type is implicit)

### `confidence` (`java.lang.Double`)

the quality/success score of the outcome (0.0-1.0, nullable)

### `documentContent` (`java.lang.String`)

prose solution from a ResolutionGuide (nullable — null for plan trace
    results)

### `documentSteps` (`java.util.List<io.casehub.api.spi.routing.DocumentStep>`)

structured steps from a ResolutionGuide (nullable — null for plan trace
    results)

### `featureSimilarities` (`java.util.Map<java.lang.String,java.lang.Double>`)

per-feature similarity contributions (empty map when unavailable)

### `features` (`java.util.Map<java.lang.String,java.lang.Object>`)

extracted features from the past case (empty map if none)

### `outcome` (`java.lang.String`)

the final case outcome (COMPLETED, FAULTED, etc.)

### `planTrace` (`java.util.List<io.casehub.api.spi.routing.ExperiencePlanStep>`)

the sequence of plan steps that were executed (empty list if none)

### `problem` (`java.lang.String`)

the problem description from the past case

### `similarityScore` (`double`)

how similar this past case is to the current case (-1.0 to 1.0)

### `solution` (`java.lang.String`)

the solution that was applied

### `sourceType` (`io.casehub.api.spi.routing.ResolutionSourceType`)

discriminator: PLAN_TRACE for historical execution traces, RESOLUTION_GUIDE for
    knowledge base documents

## Constructors

### `public RetrievedExperience(java.lang.String problem, java.lang.String solution, java.lang.String outcome, java.lang.Double confidence, double similarityScore, java.util.Map<java.lang.String,java.lang.Object> features, java.util.List<io.casehub.api.spi.routing.ExperiencePlanStep> planTrace, java.util.Map<java.lang.String,java.lang.Double> featureSimilarities)`

#### Parameters

- `problem` (`java.lang.String`)
- `solution` (`java.lang.String`)
- `outcome` (`java.lang.String`)
- `confidence` (`java.lang.Double`)
- `similarityScore` (`double`)
- `features` (`java.util.Map<java.lang.String,java.lang.Object>`)
- `planTrace` (`java.util.List<io.casehub.api.spi.routing.ExperiencePlanStep>`)
- `featureSimilarities` (`java.util.Map<java.lang.String,java.lang.Double>`)

### `public RetrievedExperience(java.lang.String problem, java.lang.String solution, java.lang.String outcome, java.lang.Double confidence, double similarityScore, java.util.Map<java.lang.String,java.lang.Object> features, java.util.List<io.casehub.api.spi.routing.ExperiencePlanStep> planTrace, java.util.Map<java.lang.String,java.lang.Double> featureSimilarities, java.lang.String caseType)`

#### Parameters

- `problem` (`java.lang.String`)
- `solution` (`java.lang.String`)
- `outcome` (`java.lang.String`)
- `confidence` (`java.lang.Double`)
- `similarityScore` (`double`)
- `features` (`java.util.Map<java.lang.String,java.lang.Object>`)
- `planTrace` (`java.util.List<io.casehub.api.spi.routing.ExperiencePlanStep>`)
- `featureSimilarities` (`java.util.Map<java.lang.String,java.lang.Double>`)
- `caseType` (`java.lang.String`)

### `public RetrievedExperience(java.lang.String problem, java.lang.String solution, java.lang.String outcome, java.lang.Double confidence, double similarityScore, java.util.Map<java.lang.String,java.lang.Object> features, java.util.List<io.casehub.api.spi.routing.ExperiencePlanStep> planTrace, java.util.Map<java.lang.String,java.lang.Double> featureSimilarities, java.lang.String caseType, io.casehub.api.spi.routing.ResolutionSourceType sourceType, java.lang.String documentContent, java.util.List<io.casehub.api.spi.routing.DocumentStep> documentSteps)`

#### Parameters

- `problem` (`java.lang.String`)
- `solution` (`java.lang.String`)
- `outcome` (`java.lang.String`)
- `confidence` (`java.lang.Double`)
- `similarityScore` (`double`)
- `features` (`java.util.Map<java.lang.String,java.lang.Object>`)
- `planTrace` (`java.util.List<io.casehub.api.spi.routing.ExperiencePlanStep>`)
- `featureSimilarities` (`java.util.Map<java.lang.String,java.lang.Double>`)
- `caseType` (`java.lang.String`)
- `sourceType` (`io.casehub.api.spi.routing.ResolutionSourceType`)
- `documentContent` (`java.lang.String`)
- `documentSteps` (`java.util.List<io.casehub.api.spi.routing.DocumentStep>`)

### `public RetrievedExperience(java.lang.String problem, java.lang.String solution, java.lang.String outcome, java.lang.Double confidence, double similarityScore, java.util.Map<java.lang.String,java.lang.Object> features, java.util.List<io.casehub.api.spi.routing.ExperiencePlanStep> planTrace, java.util.Map<java.lang.String,java.lang.Double> featureSimilarities, java.lang.String caseType, io.casehub.api.spi.routing.ResolutionSourceType sourceType, java.lang.String documentContent, java.util.List<io.casehub.api.spi.routing.DocumentStep> documentSteps, java.lang.String caseId)`

#### Parameters

- `problem` (`java.lang.String`)
- `solution` (`java.lang.String`)
- `outcome` (`java.lang.String`)
- `confidence` (`java.lang.Double`)
- `similarityScore` (`double`)
- `features` (`java.util.Map<java.lang.String,java.lang.Object>`)
- `planTrace` (`java.util.List<io.casehub.api.spi.routing.ExperiencePlanStep>`)
- `featureSimilarities` (`java.util.Map<java.lang.String,java.lang.Double>`)
- `caseType` (`java.lang.String`)
- `sourceType` (`io.casehub.api.spi.routing.ResolutionSourceType`)
- `documentContent` (`java.lang.String`)
- `documentSteps` (`java.util.List<io.casehub.api.spi.routing.DocumentStep>`)
- `caseId` (`java.lang.String`)

## Methods

### `public java.lang.String caseId()`

### `public java.lang.String caseType()`

### `public java.lang.Double confidence()`

### `public java.lang.String documentContent()`

### `public java.util.List<io.casehub.api.spi.routing.DocumentStep> documentSteps()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.Map<java.lang.String,java.lang.Double> featureSimilarities()`

### `public java.util.Map<java.lang.String,java.lang.Object> features()`

### `public final int hashCode()`

### `public java.lang.String outcome()`

### `public java.util.List<io.casehub.api.spi.routing.ExperiencePlanStep> planTrace()`

### `public java.lang.String problem()`

### `public double similarityScore()`

### `public java.lang.String solution()`

### `public io.casehub.api.spi.routing.ResolutionSourceType sourceType()`

### `public final java.lang.String toString()`
