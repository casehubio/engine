# io.casehub.api.spi.ResolutionGuideInput

**Package:** `io.casehub.api.spi`

**Kind:** `record`

## Fields

### `documentId` (`java.lang.String`)

### `domain` (`java.lang.String`)

### `features` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `metadata` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `problem` (`java.lang.String`)

### `solution` (`java.lang.String`)

### `steps` (`java.util.List<io.casehub.api.spi.GuidanceStepInput>`)

## Record Components

### `documentId` (`java.lang.String`)

### `domain` (`java.lang.String`)

### `features` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `metadata` (`java.util.Map<java.lang.String,java.lang.Object>`)

### `problem` (`java.lang.String`)

### `solution` (`java.lang.String`)

### `steps` (`java.util.List<io.casehub.api.spi.GuidanceStepInput>`)

## Constructors

### `public ResolutionGuideInput(java.lang.String documentId, java.lang.String problem, java.lang.String solution, java.util.List<io.casehub.api.spi.GuidanceStepInput> steps, java.util.Map<java.lang.String,java.lang.Object> features, java.lang.String domain, java.util.Map<java.lang.String,java.lang.Object> metadata)`

#### Parameters

- `documentId` (`java.lang.String`)
- `problem` (`java.lang.String`)
- `solution` (`java.lang.String`)
- `steps` (`java.util.List<io.casehub.api.spi.GuidanceStepInput>`)
- `features` (`java.util.Map<java.lang.String,java.lang.Object>`)
- `domain` (`java.lang.String`)
- `metadata` (`java.util.Map<java.lang.String,java.lang.Object>`)

## Methods

### `public java.lang.String documentId()`

### `public java.lang.String domain()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public java.util.Map<java.lang.String,java.lang.Object> features()`

### `public final int hashCode()`

### `public java.util.Map<java.lang.String,java.lang.Object> metadata()`

### `public java.lang.String problem()`

### `public java.lang.String solution()`

### `public java.util.List<io.casehub.api.spi.GuidanceStepInput> steps()`

### `public final java.lang.String toString()`
