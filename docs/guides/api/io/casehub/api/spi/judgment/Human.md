# io.casehub.api.spi.judgment.CallerConfig.Human

**Package:** `io.casehub.api.spi.judgment`

**Kind:** `record`

## Fields

### `candidateGroups` (`io.casehub.api.spi.routing.CandidateSetSpec`)

### `candidateUsers` (`io.casehub.api.spi.routing.CandidateSetSpec`)

### `claimDeadlineHours` (`java.lang.Integer`)

### `outcomes` (`java.util.Set<java.lang.String>`)

### `payloadType` (`java.lang.Class<?>`)

### `priority` (`java.lang.String`)

### `quorum` (`io.casehub.api.spi.QuorumConfig`)

### `scope` (`java.lang.String`)

### `scopeExpression` (`ExpressionEvaluator`)

### `templateRef` (`java.lang.String`)

### `title` (`java.lang.String`)

### `titleExpression` (`ExpressionEvaluator`)

## Record Components

### `candidateGroups` (`io.casehub.api.spi.routing.CandidateSetSpec`)

### `candidateUsers` (`io.casehub.api.spi.routing.CandidateSetSpec`)

### `claimDeadlineHours` (`java.lang.Integer`)

### `outcomes` (`java.util.Set<java.lang.String>`)

### `payloadType` (`java.lang.Class<?>`)

### `priority` (`java.lang.String`)

### `quorum` (`io.casehub.api.spi.QuorumConfig`)

### `scope` (`java.lang.String`)

### `scopeExpression` (`ExpressionEvaluator`)

### `templateRef` (`java.lang.String`)

### `title` (`java.lang.String`)

### `titleExpression` (`ExpressionEvaluator`)

## Constructors

### `public Human(io.casehub.api.spi.routing.CandidateSetSpec candidateGroups, io.casehub.api.spi.routing.CandidateSetSpec candidateUsers, java.lang.String title, ExpressionEvaluator titleExpression, java.util.Set<java.lang.String> outcomes, java.lang.Integer claimDeadlineHours, java.lang.String scope, ExpressionEvaluator scopeExpression, java.lang.String priority, java.lang.String templateRef, java.lang.Class<?> payloadType, io.casehub.api.spi.QuorumConfig quorum)`

#### Parameters

- `candidateGroups` (`io.casehub.api.spi.routing.CandidateSetSpec`)
- `candidateUsers` (`io.casehub.api.spi.routing.CandidateSetSpec`)
- `title` (`java.lang.String`)
- `titleExpression` (`ExpressionEvaluator`)
- `outcomes` (`java.util.Set<java.lang.String>`)
- `claimDeadlineHours` (`java.lang.Integer`)
- `scope` (`java.lang.String`)
- `scopeExpression` (`ExpressionEvaluator`)
- `priority` (`java.lang.String`)
- `templateRef` (`java.lang.String`)
- `payloadType` (`java.lang.Class<?>`)
- `quorum` (`io.casehub.api.spi.QuorumConfig`)

## Methods

### `public io.casehub.api.spi.routing.CandidateSetSpec candidateGroups()`

### `public io.casehub.api.spi.routing.CandidateSetSpec candidateUsers()`

### `public java.lang.Integer claimDeadlineHours()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.util.Set<java.lang.String> outcomes()`

### `public java.lang.Class<?> payloadType()`

### `public java.lang.String priority()`

### `public io.casehub.api.spi.QuorumConfig quorum()`

### `public java.lang.String scope()`

### `public ExpressionEvaluator scopeExpression()`

### `public java.lang.String templateRef()`

### `public java.lang.String title()`

### `public ExpressionEvaluator titleExpression()`

### `public final java.lang.String toString()`
