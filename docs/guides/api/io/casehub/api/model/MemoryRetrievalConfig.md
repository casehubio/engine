# io.casehub.api.model.MemoryRetrievalConfig

**Package:** `io.casehub.api.model`

**Kind:** `record`

## Fields

### `caseScopedDomains` (`java.util.Set<java.lang.String>`)

### `domains` (`java.util.Set<java.lang.String>`)

### `enabled` (`boolean`)

### `maxCaseMemories` (`int`)

### `maxMemories` (`int`)

## Record Components

### `caseScopedDomains` (`java.util.Set<java.lang.String>`)

### `domains` (`java.util.Set<java.lang.String>`)

### `enabled` (`boolean`)

### `maxCaseMemories` (`int`)

### `maxMemories` (`int`)

## Constructors

### `public MemoryRetrievalConfig(boolean enabled, int maxMemories, java.util.Set<java.lang.String> domains)`

#### Parameters

- `enabled` (`boolean`)
- `maxMemories` (`int`)
- `domains` (`java.util.Set<java.lang.String>`)

### `public MemoryRetrievalConfig(boolean enabled, int maxMemories, java.util.Set<java.lang.String> domains, java.util.Set<java.lang.String> caseScopedDomains, int maxCaseMemories)`

#### Parameters

- `enabled` (`boolean`)
- `maxMemories` (`int`)
- `domains` (`java.util.Set<java.lang.String>`)
- `caseScopedDomains` (`java.util.Set<java.lang.String>`)
- `maxCaseMemories` (`int`)

## Methods

### `public java.util.Set<java.lang.String> caseScopedDomains()`

### `public static io.casehub.api.model.MemoryRetrievalConfig defaults()`

### `public java.util.Set<java.lang.String> domains()`

### `public boolean enabled()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public final int hashCode()`

### `public boolean isCaseScopedRetrievalEffectivelyDisabled()`

### `public int maxCaseMemories()`

### `public int maxMemories()`

### `public final java.lang.String toString()`
