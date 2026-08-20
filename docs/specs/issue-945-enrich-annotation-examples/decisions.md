# Decisions — Issue #945: Enrich Annotation Examples

## D1: Example scope — all five domains plus enrich existing

**Choice:** 7 total examples: enrich 2 existing (banking, legal) + 5 new (incident response, search & rescue, aircraft maintenance, autonomous warehouse, wildfire response)
**Alternatives:**
- 2 new + enrich existing — minimal scope, covers gaps but limited domain diversity
- 3 new, leave existing untouched — clean separation but existing examples stay isolated
**Rationale:** The user wants compelling, diverse domains that demonstrate real-world relevance. Each domain naturally motivates different annotation compositions. No harm in overlapping coverage — redundancy reinforces patterns.
**Trade-offs:** More modules to maintain. Each example is small (~50-80 LOC interface + test + pom) so maintenance cost is low.
**Sources:** Issue #945 body, eidos#141 pattern (6 examples), existing engine CAPABILITY-MATRIX.md
**Exploration:** quick
**Status:** captured

## D2: Cross-repo annotation composition — engine + eidos + work + ledger

**Choice:** Examples compose annotations from all four repos that have annotation modules: engine (`@Case`, `@Worker`, `@Bind`, etc.), eidos (`@Identity`, `@Disposition`, `@AgentGoals`, `@AgentConstraints`), work (`@HumanApproval`, `@Escalate`, `@RequiresQuorum`), ledger (`@Audited`, `@ComplianceSupplement`)
**Alternatives:**
- Engine + eidos only — as stated in issue #945
- Engine only — just cover the 4 gaps
**Rationale:** Each upstream repo demonstrates its own annotations in isolation. The engine examples are the integration showcase — the only place all layers compose together. The user explicitly asked for this: "we don't demonstrate those annotations on their own, here the goal is to show how they compose and work together."
**Trade-offs:** Adds cross-repo dependencies to example poms. All four annotation modules are `0.2-SNAPSHOT` so version alignment is straightforward.
**Sources:** Survey of annotation modules across eidos, work, ledger, blocks, ras, desiredstate, platform
**Exploration:** quick
**Status:** captured

## D3: Banking example stays engine-only

**Choice:** `simple-case-annotated` (banking) enriched with engine gap coverage only — `@Worker(value)`, repeatable `@Bind`, cron trigger. No upstream annotation dependencies.
**Alternatives:**
- Add eidos/work/ledger annotations to banking too — full composition everywhere
**Rationale:** Banking is the "hello world" — the first thing someone reads. It should demonstrate engine annotations without requiring understanding of 3 other repos. The new examples carry the composition story.
**Trade-offs:** Banking doesn't show the full annotation composition story, but that's intentional — progressive disclosure.
**Sources:** Existing simple-case-annotated/SimpleAnnotatedCase.java
**Exploration:** quick
**Status:** captured

## D4: Layered design — blocks-ready extension points

**Choice:** Design all examples so blocks can enhance them later with orchestration patterns (`@DebateAgent`, `@OversightGate`, `@TrustRouted`, `@VotingAgent`). Each example should have workers and domain boundaries that naturally map to blocks governance/deliberation patterns.
**Alternatives:**
- Design examples as standalone, blocks adapts to whatever exists
**Rationale:** Blocks annotations don't exist yet, but the domain models should be structured so blocks patterns feel natural, not forced. A containment decision in incident response should have a clear seam where `@DebateAgent` could wrap it. An inspector sign-off in aircraft maintenance should map to `@OversightGate`.
**Trade-offs:** Slightly constrains domain model design — must consider blocks extension points during design. Worth it for ecosystem coherence.
**Sources:** Engine annotation design spec §Layer 2, blocks dependency graph (engine, eidos-api, work-api, ledger-api)
**Exploration:** quick
**Depends on:** D1, D2
**Status:** captured

## D5: RAS integration deferred to blocks examples

**Choice:** RAS (situation detection → case trigger) is out of scope for engine annotation examples. RAS has no annotations module and blocks doesn't depend on ras yet.
**Alternatives:**
- Add ras dependency to engine examples — show event detection triggering cases
**Rationale:** RAS creates cases programmatically via `CaseTrigger`, not via annotations. The annotation composition story doesn't include ras until ras gets its own annotation module. Blocks is the natural home for the full end-to-end story (event → case → orchestration).
**Trade-offs:** Engine examples don't show the event-driven case creation path. Blocks follow-on would.
**Sources:** ras/pom.xml dependencies, blocks dependency graph
**Exploration:** quick
**Status:** captured
