# io.casehub.api.spi.routing.ComposableAgentRoutingStrategy

**Package:** `io.casehub.api.spi.routing`

**Kind:** `class`

## Fields

### `LOG` (`Logger`)

### `assembler` (`io.casehub.api.spi.routing.RoutingSignalAssembler`)

## Constructors

### `public ComposableAgentRoutingStrategy(io.casehub.api.spi.routing.RoutingSignalAssembler assembler)`

#### Parameters

- `assembler` (`io.casehub.api.spi.routing.RoutingSignalAssembler`)

## Methods

### `public java.lang.String id()`

### `private java.util.Map<java.lang.String,java.lang.Double> resolveWeights(io.casehub.api.spi.routing.AgentRoutingContext context, java.util.Set<java.lang.String> discoveredProviders)`

#### Parameters

- `context` (`io.casehub.api.spi.routing.AgentRoutingContext`)
- `discoveredProviders` (`java.util.Set<java.lang.String>`)

### `public io.casehub.api.spi.routing.RoutingResult select(io.casehub.api.spi.routing.AgentRoutingContext context, java.util.List<io.casehub.api.spi.routing.AgentCandidate> candidates)`

#### Parameters

- `context` (`io.casehub.api.spi.routing.AgentRoutingContext`)
- `candidates` (`java.util.List<io.casehub.api.spi.routing.AgentCandidate>`)
