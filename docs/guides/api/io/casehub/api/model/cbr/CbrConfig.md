# io.casehub.api.model.cbr.CbrConfig

**Package:** `io.casehub.api.model.cbr`

**Kind:** `record`

## Fields

### `caseType` (`java.lang.String`)

### `cbrType` (`java.lang.String`)

### `crossType` (`boolean`)

### `domain` (`java.lang.String`)

### `featureExtractor` (`io.casehub.api.model.cbr.FeatureExtractor`)

### `minCostSamples` (`java.lang.Integer`)

### `minSimilarity` (`double`)

### `problemDescription` (`java.lang.String`)

### `temporalDecayHalfLifeDays` (`java.lang.Integer`)

### `timing` (`io.casehub.api.model.cbr.CbrConfig.CbrRetrievalTiming`)

### `topK` (`int`)

### `vectorWeight` (`double`)

### `weights` (`java.util.Map<java.lang.String,java.lang.Double>`)

## Record Components

### `caseType` (`java.lang.String`)

### `cbrType` (`java.lang.String`)

### `crossType` (`boolean`)

### `domain` (`java.lang.String`)

### `featureExtractor` (`io.casehub.api.model.cbr.FeatureExtractor`)

### `minCostSamples` (`java.lang.Integer`)

### `minSimilarity` (`double`)

### `problemDescription` (`java.lang.String`)

### `temporalDecayHalfLifeDays` (`java.lang.Integer`)

### `timing` (`io.casehub.api.model.cbr.CbrConfig.CbrRetrievalTiming`)

### `topK` (`int`)

### `vectorWeight` (`double`)

### `weights` (`java.util.Map<java.lang.String,java.lang.Double>`)

## Constructors

### `public CbrConfig(io.casehub.api.model.cbr.FeatureExtractor featureExtractor, int topK, double minSimilarity, java.util.Map<java.lang.String,java.lang.Double> weights, java.lang.String domain, java.lang.String caseType, double vectorWeight, io.casehub.api.model.cbr.CbrConfig.CbrRetrievalTiming timing, java.lang.String cbrType, java.lang.Integer temporalDecayHalfLifeDays, java.lang.Integer minCostSamples, boolean crossType, java.lang.String problemDescription)`

#### Parameters

- `featureExtractor` (`io.casehub.api.model.cbr.FeatureExtractor`)
- `topK` (`int`)
- `minSimilarity` (`double`)
- `weights` (`java.util.Map<java.lang.String,java.lang.Double>`)
- `domain` (`java.lang.String`)
- `caseType` (`java.lang.String`)
- `vectorWeight` (`double`)
- `timing` (`io.casehub.api.model.cbr.CbrConfig.CbrRetrievalTiming`)
- `cbrType` (`java.lang.String`)
- `temporalDecayHalfLifeDays` (`java.lang.Integer`)
- `minCostSamples` (`java.lang.Integer`)
- `crossType` (`boolean`)
- `problemDescription` (`java.lang.String`)

## Methods

### `public static io.casehub.api.model.cbr.CbrConfig.Builder builder()`

### `public java.lang.String caseType()`

### `public java.lang.String cbrType()`

### `public boolean crossType()`

### `public java.lang.String domain()`

### `public final boolean equals(java.lang.Object o)`

#### Parameters

- `o` (`java.lang.Object`)

### `public io.casehub.api.model.cbr.FeatureExtractor featureExtractor()`

### `public final int hashCode()`

### `public java.lang.Integer minCostSamples()`

### `public double minSimilarity()`

### `public java.lang.String problemDescription()`

### `public java.lang.Integer temporalDecayHalfLifeDays()`

### `public io.casehub.api.model.cbr.CbrConfig.CbrRetrievalTiming timing()`

### `public final java.lang.String toString()`

### `public int topK()`

### `public double vectorWeight()`

### `public java.util.Map<java.lang.String,java.lang.Double> weights()`
