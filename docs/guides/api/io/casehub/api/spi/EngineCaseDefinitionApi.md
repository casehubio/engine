# io.casehub.api.spi.EngineCaseDefinitionApi

**Package:** `io.casehub.api.spi`

**Kind:** `interface`

## Methods

### `public abstract io.casehub.api.view.CaseDefinitionView getDefinitionByKey(java.lang.String namespace, java.lang.String name, java.lang.String version, java.lang.String tenancyId)`

#### Parameters

- `namespace` (`java.lang.String`)
- `name` (`java.lang.String`)
- `version` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract java.util.List<io.casehub.api.view.CaseDefinitionView> getDefinitionsByName(java.lang.String namespace, java.lang.String name, java.lang.String tenancyId)`

#### Parameters

- `namespace` (`java.lang.String`)
- `name` (`java.lang.String`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.api.view.CaseDefinitionPage listDefinitions(java.lang.String tenancyId, java.lang.Integer offset, java.lang.Integer limit)`

#### Parameters

- `tenancyId` (`java.lang.String`)
- `offset` (`java.lang.Integer`)
- `limit` (`java.lang.Integer`)
