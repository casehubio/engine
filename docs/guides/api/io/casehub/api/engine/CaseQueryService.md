# io.casehub.api.engine.CaseQueryService

**Package:** `io.casehub.api.engine`

**Kind:** `interface`

Public query SPI for case instances. Consumers inject this to list and count cases without
depending on engine internals.

## Methods

### `public abstract long countActive(java.lang.String tenancyId)`

#### Parameters

- `tenancyId` (`java.lang.String`)

### `public abstract java.util.List<io.casehub.api.engine.CaseSummary> listActive(java.lang.String tenancyId, int page, int size)`

#### Parameters

- `tenancyId` (`java.lang.String`)
- `page` (`int`)
- `size` (`int`)
