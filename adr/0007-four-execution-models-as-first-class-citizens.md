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

## Context Propagation Across Execution Model Boundaries

Context propagation across the chain `Case → FlowWorker → Sub-workflow → SubCase` requires distinguishing two orthogonal concerns:

**Data context** flows via explicit mapping expressions at each boundary. The developer controls what data enters and exits each layer:
```
CaseContext
  → inputSchema JQ        → FlowWorker input
    → flow internal state
      → sub-workflow inputExpressions  → sub-workflow state
        → SubCase inputMapping JQ      → child CaseContext
```

**Propagation context** (`traceId`, `causedByEntryId`, deadline) flows implicitly through every boundary with no developer mapping. Every child unit inherits the parent's `traceId` and sets its own `causedByEntryId` automatically.

**The output (up) path is the hard problem.** When a SubCase completes, its output currently flows back to the **parent case's** `CaseContext` via `outputMapping`. This is correct when the SubCase was spawned from a case-level binding. It is incorrect when the SubCase was spawned from inside a sub-workflow step — in that case the output should flow back into the **sub-workflow's working state** so subsequent steps can use it, not directly into the case context that the sub-workflow has already consumed.

**Consequence: every boundary needs both a down-mapping and an up-mapping**, and the engine must track where completed output should be routed. This is a context return-path problem: each unit of work needs a `returnTo` reference — "when I complete, route my output here." Currently only SubCaseBinding has `outputMapping`; sub-workflow steps have no defined return path for child output.

**Design principle:** the return path must be tracked at spawn time, not inferred at completion time. When a sub-workflow step starts a SubCase, it must record `returnTo: sub-workflow-step-N` alongside `SUBCASE_STARTED`. The completion listener routes output to the correct layer using this record, not by guessing from which case called which.

This principle extends to all four execution models. A plan step that starts a case must record where the output goes. A workflow step that dispatches a worker must record the same. Propagation context wires the lineage automatically; data context routing is always explicit.

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

## langchain4j-agentic Integration Constraint

**`CaseContext` must be the `AgenticScope`, not a parallel store.**

langchain4j-agentic (`quarkus-langchain4j-agentic`, preview as of 2025) introduces `AgenticScope` — the shared mutable context passed between all agents in an agentic system. Each agent declares an `outputKey` (where it writes its result), and the `Planner` reads the initial scope state, compares it with the desired goal (desired output keys), builds a dependency graph from agent preconditions/postconditions, and determines execution order.

The structural alignment with casehub is direct:

| langchain4j-agentic | casehub-engine |
|---|---|
| `AgenticScope` | `CaseContext` |
| `outputKey` | capability `outputSchema` |
| Agent preconditions | binding trigger + capability `inputSchema` |
| Agent postconditions | capability `outputSchema` keys |
| `Planner` | `LoopControl` / binding evaluator |
| Goal (desired output keys) | `Goal` expression |

**The correct integration is `CaseContext as AgenticScope`** — a `CaseContextAgenticScope` adapter wraps the case's `CaseContext` and presents it as an `AgenticScope`. Agent writes via `outputKey` flow through the adapter into the case context, producing EventLog entries. The agent invocation sequence that `AgenticScope` tracks automatically is already tracked by casehub's EventLog (WORKER_EXECUTION_STARTED / COMPLETED). One store, one audit trail, zero duplication.

If `AgenticScope` and `CaseContext` are separate stores with a bridge between them, the result is state duplication, synchronisation lag, two audit trails that can diverge, and a leaky abstraction: the developer must understand both models. This must be avoided.

**The Planner and casehub's `LoopControl` are the same computation.** The langchain4j Planner builds a dependency graph from agents' preconditions and postconditions. casehub's binding evaluator builds an execution schedule from capabilities' `inputSchema`/`outputSchema` and trigger conditions. These should be the same thing or explicitly composable — not two separate systems running over the same data.

**Sub-workflow `AgenticScope` must follow the context return-path rule.** When a quarkus-flow workflow creates a sub-workflow `AgenticScope`, that scope must be initialised from the parent `CaseContext` slice (via `inputMapping` JQ) and agent writes within the sub-scope must route to the parent flow's working state (not the root case context), following the return-path tracking described above. Quarkus Flow already does this via its `AgenticScope`-aware Global Context mapping — casehub's integration must not break this invariant.

**References:**
- [Agentic AI Patterns — Quarkus.io](https://quarkus.io/blog/agentic-ai-patterns/)
- [Quarkus Flow + LangChain4j concepts](https://docs.quarkiverse.io/quarkus-flow/dev/concepts-agentic-langchain4j.html)
- [langchain4j Agents tutorial](https://docs.langchain4j.dev/tutorials/agents/)

## Open Questions

- What is the precise `Plan` data model? (ordered `WorkRequest`s? goal graph? capability list?)
- Should `PlanExecutor` live in the engine module or in a separate `casehub-planning` module?
- How does the `FlowWorker` ↔ `WorkOrchestrator` bridge handle case WAITING semantics — does the flow step block, or does the flow suspend and resume?
- Rules-based orchestration: should the engine provide a `RulesOrchestrator` wrapper, or is it purely a consumer concern?
- Should sub-workflow invocations be surfaced as `SubCase` elements in the Blackboard plan model, or tracked only via ledger lineage?
- Who is responsible for setting `causedByEntryId` on a child case's first ledger entry — the spawning mechanism, the engine, or the ledger capture listener?
- Should `CaseContextAgenticScope` live in `casehub-engine` (as an integration bridge) or in a separate `casehub-langchain4j` module?
- Can casehub's `LoopControl` / binding evaluator serve as a langchain4j `Planner` implementation, or do the two models diverge enough to require separate implementations that are composed?
- When a langchain4j agent tool call dispatches to another casehub worker (via MCP or direct invocation), how does that tool call appear in the casehub EventLog / lineage?
