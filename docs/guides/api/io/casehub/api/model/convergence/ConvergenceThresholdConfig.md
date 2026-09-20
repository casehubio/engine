# io.casehub.api.model.convergence.ConvergenceThresholdConfig

**Package:** `io.casehub.api.model.convergence`

**Kind:** `record`

## Fields

### `DEFAULT_CONTEXT_MUTATION_RATE` (`double`)

### `DEFAULT_DISPATCH_RATE` (`double`)

### `DEFAULT_EVALUATION_RATE` (`double`)

### `DEFAULT_RATE_WINDOW` (`java.time.Duration`)

### `DEFAULT_SIGNAL_DEPOSIT_RATE` (`double`)

### `DEFAULT_STABILITY_WINDOW` (`java.time.Duration`)

### `contextMutationRateThreshold` (`java.lang.Double`)

### `dispatchRateThreshold` (`java.lang.Double`)

### `evaluationRateThreshold` (`java.lang.Double`)

### `maxWindowEntries` (`java.lang.Integer`)

### `rateWindow` (`java.time.Duration`)

### `signalDepositRateThreshold` (`java.lang.Double`)

### `stabilityWindow` (`java.time.Duration`)

## Record Components

### `contextMutationRateThreshold` (`java.lang.Double`)

### `dispatchRateThreshold` (`java.lang.Double`)

### `evaluationRateThreshold` (`java.lang.Double`)

### `maxWindowEntries` (`java.lang.Integer`)

### `rateWindow` (`java.time.Duration`)

### `signalDepositRateThreshold` (`java.lang.Double`)

### `stabilityWindow` (`java.time.Duration`)

## Constructors

### `public ConvergenceThresholdConfig(java.lang.Double dispatchRateThreshold, java.lang.Double signalDepositRateThreshold, java.lang.Double contextMutationRateThreshold, java.lang.Double evaluationRateThreshold, java.time.Duration stabilityWindow, java.time.Duration rateWindow, java.lang.Integer maxWindowEntries)`

#### Parameters

- `dispatchRateThreshold` (`java.lang.Double`)
- `signalDepositRateThreshold` (`java.lang.Double`)
- `contextMutationRateThreshold` (`java.lang.Double`)
- `evaluationRateThreshold` (`java.lang.Double`)
- `stabilityWindow` (`java.time.Duration`)
- `rateWindow` (`java.time.Duration`)
- `maxWindowEntries` (`java.lang.Integer`)

## Methods

### `public java.lang.Double contextMutationRateThreshold()`

### `public static io.casehub.api.model.convergence.ConvergenceThresholdConfig defaults()`

### `public java.lang.Double dispatchRateThreshold()`

### `public double effectiveContextMutationRateThreshold()`

### `public double effectiveDispatchRateThreshold()`

### `public double effectiveEvaluationRateThreshold()`

### `public int effectiveMaxWindowEntries()`

### `public java.time.Duration effectiveRateWindow()`

### `public double effectiveSignalDepositRateThreshold()`

### `public java.time.Duration effectiveStabilityWindow()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.lang.Double evaluationRateThreshold()`

### `public final int hashCode()`

### `public java.lang.Integer maxWindowEntries()`

### `public java.time.Duration rateWindow()`

### `public java.lang.Double signalDepositRateThreshold()`

### `public java.time.Duration stabilityWindow()`

### `public final java.lang.String toString()`
