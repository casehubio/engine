## D1: Critique storage approach

**Choice:** Add nullable `String critique` field to existing `FailureDiagnosis` record
**Alternatives:**
- Separate `FailureCritique` type — extra type for one string field, over-engineering
- Store critique only in `_diagnostics` context (not on the record) — loses type-safe threading to WorkerContext
**Rationale:** FailureDiagnosis already threads to rerouted agents via WorkerContext.failureDiagnoses — adding critique to the record means rerouted agents get it automatically with no additional plumbing
**Trade-offs:** Record gains a 5th field (mild API growth)
**Sources:** #930 spec §6 (WorkerContext threading), FailureDiagnosis.java
**Exploration:** quick
**Status:** captured

## D2: Critique generation approach

**Choice:** `FailureCritiqueService` as `@ApplicationScoped` internal service in runtime — NOT an SPI
**Alternatives:**
- FailureCritiqueGenerator SPI — extensibility not needed; FailureClassifier SPI already handles the extensible classification layer
- Inline logic in handleSemanticFailure — mixes concerns, harder to test
**Rationale:** Single responsibility. The classifier SPI (#930) is the extensible layer; critique is the engine's own reflection on top of classification. Consumer-specific critique logic belongs in consumer FailureClassifier implementations.
**Trade-offs:** Not consumer-replaceable; revisit if a concrete use case emerges
**Sources:** FailureClassifier SPI, DefaultFailureClassifier, research §3 (Reflexion)
**Exploration:** quick
**Status:** captured

## D3: How ForwardReplanRevision gets failure context

**Choice:** Read `_diagnostics` from `adaptationContext.currentContext()` directly
**Alternatives:**
- Thread critique through AdaptationCause.StepFailed — adds a field to a sealed type for data already available in context
- Add critique to RevisionContext — couples revision to failure path unnecessarily
**Rationale:** The working layer already contains `_diagnostics` and is available on `AdaptationContext.currentContext()`. No new fields or threading needed.
**Trade-offs:** ForwardReplanRevision must parse `_diagnostics` JSON rather than receiving a typed object
**Sources:** ForwardReplanRevision.java (already reads currentContext), DefaultPlanAdaptationEvaluator.java
**Exploration:** quick
**Status:** captured
