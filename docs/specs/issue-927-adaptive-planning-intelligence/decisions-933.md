## D1: Configuration location

**Choice:** Per-case `PortfolioConfig` record on `CaseDefinition`, same pattern as `AdaptationConfig`/`MonitoringConfig`
**Alternatives:**
- Quarkus config properties (`@ConfigProperty`) — deployment-wide, can't differentiate case types
- Inline on `decompositionStrategy` field (overloaded string/object) — breaks the existing string-typed field
**Rationale:** Delegate list is behavioral: case definitions with well-specified GOAP actions benefit from `["goap", "llm"]`, while those without should skip to `["llm"]`. Different cases need different cascades.
**Trade-offs:** New record type + YAML parsing (minor)
**Sources:** `AdaptationConfig`, `MonitoringConfig` patterns, `CaseDefinitionYamlMapper`
**Exploration:** quick
**Status:** captured

## D2: Failure handling — cascade vs fail

**Choice:** Catch all exceptions from delegates (AgentException, RuntimeException, TimeoutException) and cascade to next. All delegates exhausted → throw AgentException.
**Alternatives:**
- Fail on first non-AgentException — too strict, LLM timeouts and transient errors would halt decomposition
- Return empty plan instead of throwing — inconsistent with existing strategy contract (GoapDecompositionStrategy throws)
**Rationale:** Portfolio's value proposition IS resilience. Any exception from a delegate is "this strategy couldn't produce a plan" — the reason doesn't matter for cascading.
**Trade-offs:** Swallows exceptions that might indicate genuine bugs in delegate strategies — mitigated by logging each failure
**Sources:** `GoapDecompositionStrategy` (throws AgentException), `ChainedActionRiskClassifier` (most-restrictive-wins pattern)
**Exploration:** quick
**Status:** captured

## D3: Time budget enforcement mechanism

**Choice:** Virtual thread with `Future.get(timeout)` per delegate — consistent with existing engine patterns
**Alternatives:**
- Thread.interrupt() — unreliable for LLM HTTP calls
- No timeout enforcement (rely on delegates) — GOAP has no built-in timeout, could spin on complex state spaces
**Rationale:** `PatternWorkerFunctionHandler` already uses `Future.get(timeout)` for time budget enforcement. Same pattern, same module.
**Trade-offs:** Adds virtual thread per delegate attempt (negligible cost)
**Sources:** `PatternWorkerFunctionHandler`, `PlanningConstraints.timeBudget` enforcement
**Exploration:** quick
**Status:** captured
