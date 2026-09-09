# io.casehub.api.spi.DispatchBudget

**Package:** `io.casehub.api.spi`

**Kind:** `interface`

External dispatch budget for session-level capacity providers. The engine consults this SPI
before dispatching bindings to avoid wasted work (routing, EventLog, PlanItem creation) when the
provider's capacity is exhausted.

<p>Advisory semantics — the capacity query prevents most wasted work, but cross-case TOCTOU races
can briefly over-dispatch. `WorkerExecutionManager.submit()` remains the hard gate.

<p>Default implementation returns `Integer.MAX_VALUE` (unlimited). Consumer implementations
(e.g. claudony session pool) provide `@ApplicationScoped` beans that displace the default.

## Methods

### `public abstract int availableCapacity(io.casehub.api.spi.DispatchBudgetQuery query)`

#### Parameters

- `query` (`io.casehub.api.spi.DispatchBudgetQuery`)
