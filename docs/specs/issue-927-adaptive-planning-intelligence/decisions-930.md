# Decisions — #930 Failure Taxonomy and Diagnosis Routing

## D1: Classification site

**Choice:** Inside `handleSemanticFailure()` — classify before writing `_diagnostics`
**Alternatives:**
- Separate handler consuming WORKER_OUTCOME_RESOLVED — cleaner separation but requires re-reading state
- In OutcomePolicy itself — mixes classification with routing concerns
**Rationale:** handleSemanticFailure already has outcome, reason, binding context, and retry history. Adding classification at the top and branching behavior is the minimal change path.
**Trade-offs:** Makes handleSemanticFailure larger. Acceptable for pre-release.
**Sources:** WorkflowExecutionCompletedHandler.java:357-538
**Exploration:** quick
**Status:** captured

## D2: Transient failure agent exclusion

**Choice:** No exclusion — Transient failures do NOT add the agent to excludedAgents
**Alternatives:**
- Soft exclusion with TTL — timestamp-based exclusion expiry. More complex.
- Keep permanent exclusion — Transient classification only affects adaptation triggers
**Rationale:** A transient timeout doesn't indicate the agent can't do the job. Permanently excluding it wastes a capable agent. Attempt count still increments toward maxRerouteAttempts as a safety valve.
**Trade-offs:** May immediately re-select a still-failing agent. The retry backoff in QuartzRetryService mitigates this.
**Sources:** CaseContextChangedEventHandler.java:437-456, WorkflowExecutionCompletedHandler.java:452-457
**Exploration:** quick
**Status:** captured

## D3: Adaptation link

**Choice:** Classify only — store category in _diagnostics. #934 reads it for Persist/Refine/Concede decisions.
**Alternatives:**
- Wire a KNOWLEDGE_FAILURE event for the adaptation layer. More immediate but couples to adaptation pipeline.
**Rationale:** Clean separation of concerns. This issue owns classification. #934 (meta-reasoning) owns the adaptation decision. The category is available in _diagnostics for any consumer.
**Trade-offs:** Knowledge failures don't immediately trigger adaptation until #934 is implemented.
**Sources:** research/2026-08-18-adaptive-planning-intelligence.md §Issue 3, §Issue 7
**Exploration:** quick
**Status:** captured

## D4: Diagnosis threading path

**Choice:** Add `failureDiagnosis` field to `WorkerContext`. Same threading pattern as experiences/memories.
**Alternatives:**
- Workers read from case context _diagnostics directly — no WorkerContext change but less explicit
**Rationale:** WorkerContext already carries experiences (List<RetrievedExperience>) and memories (List<RetrievedMemory>) with the same populate-at-dispatch pattern. Adding FailureDiagnosis follows the established convention.
**Trade-offs:** One more field on WorkerContext. Backward-compatible via constructor overloads.
**Sources:** WorkerContext.java:37-94, WorkerScheduleEventHandler (dispatch-time population)
**Exploration:** quick
**Status:** captured
