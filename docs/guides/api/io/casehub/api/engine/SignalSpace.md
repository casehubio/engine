# io.casehub.api.engine.SignalSpace

**Package:** `io.casehub.api.engine`

**Kind:** `interface`

## Fields

### `NOOP` (`io.casehub.api.engine.SignalSpace`)

## Methods

### `public abstract void deposit(java.lang.String name, double strength)`

#### Parameters

- `name` (`java.lang.String`)
- `strength` (`double`)

### `public abstract void deposit(java.lang.String name, double strength, java.time.Duration halfLife)`

#### Parameters

- `name` (`java.lang.String`)
- `strength` (`double`)
- `halfLife` (`java.time.Duration`)

### `public abstract java.util.Map<java.lang.String,io.casehub.api.model.signal.PerceivedSignal> perceive()`
