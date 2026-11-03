# io.casehub.api.model.coordination.TopologyType

**Package:** `io.casehub.api.model.coordination`

**Kind:** `enum`

## Enum Constants

### `CENTRALISED` (`io.casehub.api.model.coordination.TopologyType`)

### `DISTRIBUTED` (`io.casehub.api.model.coordination.TopologyType`)

### `GRADUATED` (`io.casehub.api.model.coordination.TopologyType`)

### `HIERARCHICAL` (`io.casehub.api.model.coordination.TopologyType`)

### `HYBRID` (`io.casehub.api.model.coordination.TopologyType`)

## Constructors

### `private TopologyType()`

## Methods

### `public abstract io.casehub.api.model.coordination.DecisionAuthority deriveAuthority(io.casehub.api.model.coordination.StructuralRole role)`

#### Parameters

- `role` (`io.casehub.api.model.coordination.StructuralRole`)

### `public static io.casehub.api.model.coordination.TopologyType valueOf(java.lang.String name)`

#### Parameters

- `name` (`java.lang.String`)

### `public static io.casehub.api.model.coordination.TopologyType[] values()`
