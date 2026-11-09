# io.casehub.api.engine.rest.EngineEventLogApi

**Package:** `io.casehub.api.engine.rest`

**Kind:** `interface`

## Methods

### `public abstract io.casehub.api.view.EventLogPage getEventLog(java.util.UUID caseId, java.lang.String tenancyId, java.lang.Integer offset, java.lang.Integer limit, java.util.List<java.lang.String> eventTypes, java.util.List<java.lang.String> streamTypes)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `offset` (`java.lang.Integer`)
- `limit` (`java.lang.Integer`)
- `eventTypes` (`java.util.List<java.lang.String>`)
- `streamTypes` (`java.util.List<java.lang.String>`)

### `public abstract io.casehub.api.view.EventLogPage getEventsSince(java.util.UUID caseId, java.lang.String tenancyId, java.lang.String since)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `since` (`java.lang.String`)

### `public abstract io.casehub.api.view.EventLogPage getLatestEvents(java.util.UUID caseId, java.lang.String tenancyId, java.lang.Integer limit)`

#### Parameters

- `caseId` (`java.util.UUID`)
- `tenancyId` (`java.lang.String`)
- `limit` (`java.lang.Integer`)
