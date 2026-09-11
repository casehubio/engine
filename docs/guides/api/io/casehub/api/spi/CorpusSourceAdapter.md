# io.casehub.api.spi.CorpusSourceAdapter

**Package:** `io.casehub.api.spi`

**Kind:** `interface`

## Methods

### `public abstract java.util.List<io.casehub.api.spi.ResolutionGuideInput> discover(java.lang.String tenancyId)`

#### Parameters

- `tenancyId` (`java.lang.String`)

### `public default void onChange(java.util.function.Consumer<io.casehub.api.spi.CorpusChangeEvent> listener)`

#### Parameters

- `listener` (`java.util.function.Consumer<io.casehub.api.spi.CorpusChangeEvent>`)

### `public default boolean supportsChangeDetection()`
