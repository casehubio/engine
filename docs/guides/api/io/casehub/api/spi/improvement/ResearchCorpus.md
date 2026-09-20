# io.casehub.api.spi.improvement.ResearchCorpus

**Package:** `io.casehub.api.spi.improvement`

**Kind:** `interface`

## Methods

### `public abstract void addToHilQueue(io.casehub.api.model.stigmergy.HilQueueEntry entry)`

#### Parameters

- `entry` (`io.casehub.api.model.stigmergy.HilQueueEntry`)

### `public abstract java.util.Optional<io.casehub.api.model.stigmergy.ResearchFinding> get(java.lang.String sourceUrl)`

#### Parameters

- `sourceUrl` (`java.lang.String`)

### `public abstract java.util.List<io.casehub.api.model.stigmergy.HilQueueEntry> pendingHilEntries()`

### `public abstract void resolveHilEntry(java.lang.String sourceUrl, io.casehub.api.model.stigmergy.ResearchCandidate retrieved)`

#### Parameters

- `sourceUrl` (`java.lang.String`)
- `retrieved` (`io.casehub.api.model.stigmergy.ResearchCandidate`)

### `public abstract java.util.List<io.casehub.api.model.stigmergy.ResearchFinding> search(java.lang.String query, java.lang.String capabilityArea, int limit)`

#### Parameters

- `query` (`java.lang.String`)
- `capabilityArea` (`java.lang.String`)
- `limit` (`int`)

### `public abstract void store(java.util.List<io.casehub.api.model.stigmergy.ResearchCandidate> candidates, io.casehub.api.model.stigmergy.ResearchAnalysis analysis)`

#### Parameters

- `candidates` (`java.util.List<io.casehub.api.model.stigmergy.ResearchCandidate>`)
- `analysis` (`io.casehub.api.model.stigmergy.ResearchAnalysis`)
