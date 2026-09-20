# io.casehub.api.engine.InterestSpace

**Package:** `io.casehub.api.engine`

**Kind:** `interface`

## Fields

### `NOOP` (`io.casehub.api.engine.InterestSpace`)

## Methods

### `public abstract void deregister(java.lang.String interestId)`

#### Parameters

- `interestId` (`java.lang.String`)

### `public abstract io.casehub.api.spi.observation.InterestLandscape landscape()`

### `public abstract java.util.List<io.casehub.api.spi.observation.InterestRegistration> mine()`

### `public abstract io.casehub.api.spi.observation.InterestRegistration register(io.casehub.api.spi.observation.InterestDeclaration interest)`

#### Parameters

- `interest` (`io.casehub.api.spi.observation.InterestDeclaration`)

### `public abstract boolean registerObserver(io.casehub.api.spi.observation.EnvironmentObserver observer)`

#### Parameters

- `observer` (`io.casehub.api.spi.observation.EnvironmentObserver`)
