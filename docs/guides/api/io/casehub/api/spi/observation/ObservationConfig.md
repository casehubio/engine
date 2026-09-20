# io.casehub.api.spi.observation.ObservationConfig

**Package:** `io.casehub.api.spi.observation`

**Kind:** `record`

## Fields

### `DEFAULT_MAX_HISTORY_AGE` (`java.time.Duration`)

### `DEFAULT_MAX_HISTORY_ENTRIES` (`int`)

### `DEFAULT_MAX_OBSERVERS_PER_CASE` (`int`)

### `allowJqInterests` (`boolean`)

### `maxHistoryAge` (`java.time.Duration`)

### `maxHistoryEntries` (`int`)

### `maxObserversPerCase` (`int`)

## Record Components

### `allowJqInterests` (`boolean`)

### `maxHistoryAge` (`java.time.Duration`)

### `maxHistoryEntries` (`int`)

### `maxObserversPerCase` (`int`)

## Constructors

### `public ObservationConfig(int maxHistoryEntries, java.time.Duration maxHistoryAge, int maxObserversPerCase)`

#### Parameters

- `maxHistoryEntries` (`int`)
- `maxHistoryAge` (`java.time.Duration`)
- `maxObserversPerCase` (`int`)

### `public ObservationConfig(int maxHistoryEntries, java.time.Duration maxHistoryAge, int maxObserversPerCase, boolean allowJqInterests)`

#### Parameters

- `maxHistoryEntries` (`int`)
- `maxHistoryAge` (`java.time.Duration`)
- `maxObserversPerCase` (`int`)
- `allowJqInterests` (`boolean`)

## Methods

### `public boolean allowJqInterests()`

### `public static io.casehub.api.spi.observation.ObservationConfig defaults()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public java.time.Duration maxHistoryAge()`

### `public int maxHistoryEntries()`

### `public int maxObserversPerCase()`

### `public final java.lang.String toString()`
