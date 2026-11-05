# io.casehub.api.model.coordination.CoordinationPattern

**Package:** `io.casehub.api.model.coordination`

**Kind:** `enum`

## Fields

### `defaultAuthorityModel` (`io.casehub.api.model.coordination.TopologyType`)

## Enum Constants

### `HIVE` (`io.casehub.api.model.coordination.CoordinationPattern`)

### `MANY` (`io.casehub.api.model.coordination.CoordinationPattern`)

### `MESH` (`io.casehub.api.model.coordination.CoordinationPattern`)

### `SWARM` (`io.casehub.api.model.coordination.CoordinationPattern`)

## Constructors

### `private CoordinationPattern(io.casehub.api.model.coordination.TopologyType defaultAuthorityModel)`

#### Parameters

- `defaultAuthorityModel` (`io.casehub.api.model.coordination.TopologyType`)

## Methods

### `public java.util.Optional<io.casehub.api.model.coordination.TopologyType> defaultAuthorityModel()`

### `public static java.util.Optional<io.casehub.api.model.coordination.CoordinationPattern> fromTopologyString(java.lang.String topology)`

#### Parameters

- `topology` (`java.lang.String`)

### `public static io.casehub.api.model.coordination.CoordinationPattern valueOf(java.lang.String name)`

#### Parameters

- `name` (`java.lang.String`)

### `public static io.casehub.api.model.coordination.CoordinationPattern[] values()`
