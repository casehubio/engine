# io.casehub.api.model.convergence.BudgetConfig

**Package:** `io.casehub.api.model.convergence`

**Kind:** `record`

## Fields

### `maxContextMutations` (`java.lang.Integer`)

### `maxDispatches` (`java.lang.Integer`)

### `maxEvaluationCycles` (`java.lang.Integer`)

### `maxSignalDeposits` (`java.lang.Integer`)

## Record Components

### `maxContextMutations` (`java.lang.Integer`)

### `maxDispatches` (`java.lang.Integer`)

### `maxEvaluationCycles` (`java.lang.Integer`)

### `maxSignalDeposits` (`java.lang.Integer`)

## Constructors

### `public BudgetConfig(java.lang.Integer maxDispatches, java.lang.Integer maxSignalDeposits, java.lang.Integer maxContextMutations, java.lang.Integer maxEvaluationCycles)`

#### Parameters

- `maxDispatches` (`java.lang.Integer`)
- `maxSignalDeposits` (`java.lang.Integer`)
- `maxContextMutations` (`java.lang.Integer`)
- `maxEvaluationCycles` (`java.lang.Integer`)

## Methods

### `public static io.casehub.api.model.convergence.BudgetConfig defaults()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public static boolean isExceeded(long count, java.lang.Integer cap)`

#### Parameters

- `count` (`long`)
- `cap` (`java.lang.Integer`)

### `public java.lang.Integer maxContextMutations()`

### `public java.lang.Integer maxDispatches()`

### `public java.lang.Integer maxEvaluationCycles()`

### `public java.lang.Integer maxSignalDeposits()`

### `public final java.lang.String toString()`
