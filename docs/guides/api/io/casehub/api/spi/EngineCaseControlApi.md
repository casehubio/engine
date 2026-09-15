# io.casehub.api.spi.EngineCaseControlApi

**Package:** `io.casehub.api.spi`

**Kind:** `interface`

## Methods

### `public abstract io.casehub.api.view.CaseControlView cancelCase(java.util.UUID caseId, io.casehub.api.view.CaseControlRequest request, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `request` (`io.casehub.api.view.CaseControlRequest`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.api.view.CaseControlView resumeCase(java.util.UUID caseId, io.casehub.api.view.CaseControlRequest request, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `request` (`io.casehub.api.view.CaseControlRequest`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.api.view.SignalResultView sendSignal(java.util.UUID caseId, io.casehub.api.view.SendSignalRequest request, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `request` (`io.casehub.api.view.SendSignalRequest`)
- `tenancyId` (`java.lang.String`)

### `public abstract io.casehub.api.view.CaseControlView suspendCase(java.util.UUID caseId, io.casehub.api.view.CaseControlRequest request, java.lang.String tenancyId)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `request` (`io.casehub.api.view.CaseControlRequest`)
- `tenancyId` (`java.lang.String`)
