# io.casehub.api.spi.improvement.EngineEvolutionApi

**Package:** `io.casehub.api.spi.improvement`

**Kind:** `interface`

## Methods

### `public abstract void addDenyPattern(java.util.UUID caseId, java.lang.String tenancyId, java.lang.String pattern)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `pattern` (`java.lang.String`)

### `public abstract void addWatchPattern(java.util.UUID caseId, java.lang.String tenancyId, java.lang.String category, java.lang.String areaId, java.lang.String targetPattern, java.lang.Integer minEstimatedSize)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `category` (`java.lang.String`)
- `areaId` (`java.lang.String`)
- `targetPattern` (`java.lang.String`)
- `minEstimatedSize` (`java.lang.Integer`)

### `public abstract void blockImprovement(java.util.UUID caseId, java.lang.String tenancyId, java.util.UUID improvementId, java.util.UUID blockedBy)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `improvementId` (`java.util.UUID`)
- `blockedBy` (`java.util.UUID`)

### `public abstract io.casehub.api.model.stigmergy.ArtifactManifest getArtifactTrail(java.util.UUID caseId, java.lang.String tenancyId, java.util.UUID improvementCaseId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `improvementCaseId` (`java.util.UUID`)

### `public abstract io.casehub.api.view.DenyPatternView getDenyPatterns(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.api.view.EvolutionStateSnapshot getEvolutionState(java.util.UUID caseId, java.lang.String tenancyId, io.casehub.api.model.stigmergy.ImprovementConfig config)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `config` (`io.casehub.api.model.stigmergy.ImprovementConfig`)

### `public abstract java.util.List<io.casehub.api.model.stigmergy.ConductorInboxEntry> getInbox(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.api.model.stigmergy.ReadinessReport getReadinessReport(java.util.UUID caseId, java.lang.String tenancyId, io.casehub.api.model.stigmergy.ComplianceLevel targetLevel, io.casehub.api.model.stigmergy.ImprovementConfig config)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `targetLevel` (`io.casehub.api.model.stigmergy.ComplianceLevel`)
- `config` (`io.casehub.api.model.stigmergy.ImprovementConfig`)

### `public abstract io.casehub.api.view.ResearchCorpusView getResearchCorpus(java.lang.String query, java.lang.String areaId, int limit)`

#### Parameters

- `query` (`java.lang.String`)
- `areaId` (`java.lang.String`)
- `limit` (`int`)

### `public abstract java.util.List<io.casehub.api.view.EvolutionStateSnapshot.ImprovementStreamView> getStreamProgress(java.util.UUID caseId, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.api.view.EvolutionSummary getSummary(java.util.UUID caseId, java.lang.String tenancyId, java.lang.String areaId, java.lang.String category, java.lang.Integer timeWindowMinutes, java.util.UUID improvementCaseId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `areaId` (`java.lang.String`)
- `category` (`java.lang.String`)
- `timeWindowMinutes` (`java.lang.Integer`)
- `improvementCaseId` (`java.util.UUID`)

### `public abstract java.util.List<io.casehub.api.model.stigmergy.TickTrace> getTickHistory(java.util.UUID caseId, java.lang.Integer limit)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `limit` (`java.lang.Integer`)

### `public abstract void pauseCategory(java.util.UUID caseId, java.lang.String tenancyId, java.lang.String category, int durationMinutes)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `category` (`java.lang.String`)
- `durationMinutes` (`int`)

### `public abstract void removeDenyPattern(java.util.UUID caseId, java.lang.String tenancyId, java.lang.String pattern)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `pattern` (`java.lang.String`)

### `public abstract void removeWatchPattern(java.util.UUID caseId, java.lang.String tenancyId, java.lang.String patternId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `patternId` (`java.lang.String`)

### `public abstract void resetCircuitBreaker(java.util.UUID caseId)`

#### Parameters

- `caseId` (`java.util.UUID`)

### `public abstract void resolveGate(java.util.UUID caseId, java.lang.String tenancyId, java.lang.String entryId, io.casehub.api.model.stigmergy.ConductorInboxEntry.Status outcome, java.lang.String reason, java.lang.String feedback)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `entryId` (`java.lang.String`)
- `outcome` (`io.casehub.api.model.stigmergy.ConductorInboxEntry.Status`)
- `reason` (`java.lang.String`)
- `feedback` (`java.lang.String`)

### `public abstract void setGatePolicy(java.util.UUID caseId, java.lang.String tenancyId, io.casehub.api.model.stigmergy.GatePolicy policy)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `policy` (`io.casehub.api.model.stigmergy.GatePolicy`)

### `public abstract io.casehub.api.model.stigmergy.ReadinessReport triggerReadinessValidation(java.util.UUID caseId, java.lang.String tenancyId, io.casehub.api.model.stigmergy.ComplianceLevel targetLevel, io.casehub.api.model.stigmergy.ImprovementConfig config)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `targetLevel` (`io.casehub.api.model.stigmergy.ComplianceLevel`)
- `config` (`io.casehub.api.model.stigmergy.ImprovementConfig`)

### `public abstract void unblockImprovement(java.util.UUID caseId, java.lang.String tenancyId, java.util.UUID improvementId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `improvementId` (`java.util.UUID`)

### `public abstract void unpauseCategory(java.util.UUID caseId, java.lang.String tenancyId, java.lang.String category)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `category` (`java.lang.String`)
