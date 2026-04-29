# ADR-0007 — Four Execution Models as First-Class Citizens

**Date:** 2026-04-29
**Status:** Accepted
**Refs:** casehubio/engine#200

---

## Context

casehub-engine was built around the Blackboard Architecture — a choreography model where workers self-organise around a shared context without explicit ordering. This is well-suited to emergent, open-ended coordination.

As the platform matures, three additional execution models are needed:

- **Orchestration (rules-based):** a rules engine (Drools, DMN) evaluates case context and prescribes which workers execute and in what sequence.
- **Orchestration (workflow-based):** a workflow DSL (Serverless Workflow / quarkus-flow) sequences workers explicitly, with control-flow primitives (branches, loops, parallel, wait).
- **Planning:** a planner — human, LLM, or algorithmic — produces an ordered list of steps (`WorkRequest`s) and hands it to the engine for execution.

Each model is appropriate for a different class of problem. No single model covers all cases well. The question is whether casehub treats the others as second-class add-ons to choreography, or as co-equal citizens.

## Decision

**All four execution models are first-class citizens in casehub-engine.**

The CaseEngine is execution-model agnostic. How the next worker is selected is a routing decision made by one of four routers. The execution path — `WorkerScheduleEvent` → `WorkerScheduleEventHandler` → Quartz → `WorkerExecutionTask` — is shared by all four. The engine does not know or care which router chose the worker.

| Model | Router | Decision maker |
|---|---|---|
| Choreography | `CaseContextChangedEventHandler` | Binding conditions evaluated against CaseContext |
| Orchestration (rules) | `WorkOrchestrator` driven by rules engine output | Rules engine (Drools/DMN) |
| Orchestration (workflow) | `FlowWorker` steps dispatching via `WorkOrchestrator` | Serverless Workflow DSL |
| Planning | `PlanExecutor` (to be designed) consuming a `Plan` | Human or LLM planner |

A single case may use all four simultaneously. There is no hierarchy: choreography is not the default that others extend; workflow is not a special case of orchestration. Each is a first-class routing strategy.

## Consequences

**Shared execution path:** All four models produce the same EventLog entries (`WORKER_SCHEDULED`, `WORKER_EXECUTION_STARTED`, `WORKER_EXECUTION_COMPLETED`), the same `CaseLedgerEntry` chain, and the same lineage graph. Observability and auditability are identical regardless of which model selected the worker.

**Composability within a case:** A case can use choreography for emergent sections (context-driven), a workflow for a complex sub-process with explicit control flow, and a plan for a prescribed sequence. Transitions between models are transparent — they all produce `WorkerScheduleEvent`.

**FlowWorker must gain WorkOrchestrator access:** For workflow-based orchestration to be first-class, a quarkus-flow step must be able to dispatch a casehub worker and receive its result. This requires a `casehub-dispatch` function bridge (design in casehubio/engine#200). Until this is built, workflow-based orchestration is not fully first-class.

**PlanExecutor must be designed:** Planning requires a `Plan` data model (ordered `WorkRequest`s, or goal-decomposed steps), a `PlanExecutor` handler, and a `PlanSource` SPI (human input, LLM generation, algorithmic). Design work required before implementation.

**Rules-based orchestration:** This model composes existing primitives (`WorkOrchestrator.submit/submitAndWait`) driven by external rules engine output. No engine changes needed — the rules engine produces a sequence and the caller dispatches it. First-class in principle; the integration layer (rules engine → WorkOrchestrator) is a consumer concern.

## Alternatives Rejected

**Choreography as the canonical model, others as extensions.** Rejected because orchestration and planning represent fundamentally different coordination strategies, not refinements of the Blackboard pattern. Treating them as extensions would cause both design pressure (forcing them to conform to binding/context semantics) and conceptual confusion.

**Separate case types per execution model.** Rejected because a single case often needs multiple models for different stages — emergent exploration followed by prescribed execution, or workflow-driven processing with choreographed exception handling.

## Lineage Constraint

**All four execution models must produce a complete, traversable causal graph.**

Every unit of work — case, sub-case, workflow run, sub-workflow invocation, plan step — must produce a `CaseLedgerEntry` node. The `causedByEntryId` field on each node points to the ledger entry that caused it. This creates a directed acyclic graph of causation that is fully traversable regardless of which execution model produced each node.

Current partial state:
- ✅ Case → SubCase: `SUBCASE_STARTED` ledger entry in parent; child's `CASE_STARTED` ledger entry carries `causedByEntryId` pointing to it
- ❌ Workflow step → dispatched worker/sub-case: quarkus-flow step emits no ledger entry; lineage breaks at the workflow boundary
- ❌ Sub-workflow invocation: has no ledger node; cannot serve as `causedByEntryId` anchor
- ❌ Plan step: no ledger node; cases started by plan steps have no recorded cause

The design implication: the `FlowWorker` ↔ `WorkOrchestrator` bridge (and sub-workflow invocations) must emit ledger entries before dispatching child work. Similarly, `PlanExecutor` must write a ledger entry for each plan step it begins.

The `traceId` on every ledger entry (already populated via `LedgerTraceIdProvider`) provides the distributed tracing connection. The `causedByEntryId` chain provides the causal lineage. Together they answer: *"show me everything that happened, in causal order, across all cases and sub-processes spawned by this workflow run."*

A sub-workflow that starts a new `CaseInstance` may or may not be treated as a `SubCase` in the CMMN sense — but it must always produce a ledger node that the child case's `CASE_STARTED` can point to. Whether to surface this in the `CasePlanModel` as a `SubCase` element is a separate decision.

## Open Questions

- What is the precise `Plan` data model? (ordered `WorkRequest`s? goal graph? capability list?)
- Should `PlanExecutor` live in the engine module or in a separate `casehub-planning` module?
- How does the `FlowWorker` ↔ `WorkOrchestrator` bridge handle case WAITING semantics — does the flow step block, or does the flow suspend and resume?
- Rules-based orchestration: should the engine provide a `RulesOrchestrator` wrapper, or is it purely a consumer concern?
- Should sub-workflow invocations be surfaced as `SubCase` elements in the Blackboard plan model, or tracked only via ledger lineage?
- Who is responsible for setting `causedByEntryId` on a child case's first ledger entry — the spawning mechanism, the engine, or the ledger capture listener?
