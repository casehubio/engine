# io.casehub.api.model.coordination.CommunicationMode

**Package:** `io.casehub.api.model.coordination`

**Kind:** `enum`

How agents exchange information within a coordination topology. Independent of topology and scope
— a DISTRIBUTED topology can use stigmergy or direct messaging.

## Enum Constants

### `BROADCAST` (`io.casehub.api.model.coordination.CommunicationMode`)

### `DIRECT_MESSAGING` (`io.casehub.api.model.coordination.CommunicationMode`)

### `GROUP_CHANNEL` (`io.casehub.api.model.coordination.CommunicationMode`)

### `STIGMERGY` (`io.casehub.api.model.coordination.CommunicationMode`)

## Constructors

### `private CommunicationMode()`

## Methods

### `public static io.casehub.api.model.coordination.CommunicationMode valueOf(java.lang.String name)`

#### Parameters

- `name` (`java.lang.String`)

### `public static io.casehub.api.model.coordination.CommunicationMode[] values()`
