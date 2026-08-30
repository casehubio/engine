# definitionRef — Cross-File YAML Navigation for Diagram Drill-Down

**Issue:** engine#951
**Date:** 2026-08-30

## Problem

Each diagram format (Case, SWF, HTN, DAG) can have nodes that reference
workers or executors by name. Today these references are opaque strings with
no mechanism to resolve them to the actual YAML definition file. The UI cannot
offer drill-down navigation from a worker node in one diagram to the definition
of what that worker does.

The drill-down graph spans format boundaries — a Case worker might embed a SWF
workflow, which dispatches back to another Case via `casehub:dispatch`. Without
a resolution mechanism, the UI hits a dead end at every cross-reference.

## Solution

A cross-format `definitionRef` convention. Any YAML node that references an
external definition can carry a `definitionRef` field pointing to the
definition's location. The UI follows these refs to enable orthogonal
drill-down across all diagram types.

## definitionRef Field

A nullable string field on any YAML element that references another definition.
Two forms:

| Form | Syntax | Resolves to |
|------|--------|-------------|
| File path | `definitionRef: path/to/file.yaml` | External YAML file, relative to the referencing file's directory |
| Inline ref | `definitionRef: '#name'` | Entry in the `definitions:` block of the same YAML file |

### File Path Resolution

Paths are always relative to the directory containing the referencing YAML
file. Absolute paths and URLs are not supported in v1.

```
project/
  cases/
    investigation.yaml          ← contains definitionRef: ../workflows/research.yaml
  workflows/
    research.yaml               ← resolved target
```

### Inline Definitions

A top-level `definitions:` block in a Case YAML provides a namespace for
inline definitions referenced via `#name`:

```yaml
dsl: "0.1"
name: Investigation Pipeline
namespace: security
spec:
  workers:
    - name: analyst
      capabilities: [deep-analysis]
      definitionRef: '#analysis-flow'

definitions:
  analysis-flow:
    do:
      - classify:
          set:
            category: ${ .incident.type }
      - investigate:
          call: casehub:dispatch
          with:
            capability: forensics
          definitionRef: cases/forensics-case.yaml
```

The `definitions:` block is a top-level sibling of `spec:` (not nested inside
it). It is a flat map of name → YAML definition. The definitions are not
loaded by the engine at parse time — they are opaque content for the UI to
resolve.

## Cross-Format Convention

`definitionRef` is not specific to Case YAML. It applies uniformly wherever a
node references an external definition:

| Format | Where definitionRef appears | Example |
|--------|---------------------------|---------|
| Case | `workers[].definitionRef` | `definitionRef: workflows/research.yaml` |
| SWF (casehub steps) | On `call: casehub:dispatch` steps | `definitionRef: cases/forensics.yaml` |
| SWF (third-party steps) | `metadata.definitionRef` | SWF-compliant extension |
| HTN (future, #987) | On leaf tasks | `definitionRef: workflows/extract.yaml` |
| DAG (future) | On nodes | `definitionRef: cases/validate.yaml` |

For CaseHub-owned formats (Case, casehub:dispatch, HTN, DAG), `definitionRef`
is a direct field. For third-party formats (standard SWF steps), it goes in
`metadata:` — the standard SWF extension point.

### Full Drill-Down Chain Example

```yaml
# cases/incident.yaml (Case diagram)
dsl: "0.1"
name: Incident Response
spec:
  workers:
    - name: triage-bot
      capabilities: [triage]
      definitionRef: cases/triage.yaml           # → Case diagram

    - name: investigation-flow
      capabilities: [investigate]
      definitionRef: '#investigation'             # → SWF diagram (inline)

definitions:
  investigation:
    do:
      - collect-evidence:
          call: casehub:dispatch
          with:
            capability: evidence-collection
          definitionRef: cases/evidence.yaml      # → Case diagram

      - analyse:
          call: casehub:dispatch
          with:
            capability: forensics
          definitionRef: workflows/forensics.yaml # → SWF diagram

      - report:
          call: http
          with:
            method: POST
            endpoint: https://reporting.example.com
          metadata:
            definitionRef: schemas/report-api.yaml  # → API schema
```

The UI traverses: Case → SWF (inline) → Case (evidence) → ... Each hop
follows a `definitionRef` and uses structural detection to render the right
diagram type.

## Structural Type Detection

The UI detects diagram type from the YAML structure — no explicit `format:`
field:

| Detection rule | Diagram type |
|---------------|--------------|
| Has `workers:` and `bindings:` | Case |
| Has `do:` (top-level or as the definition body) | SWF |
| Has `decomposition:` with `root:` and `methods:` | HTN |
| Has `nodes:` with entries containing `dependsOn:` | DAG |

Detection is ordered — first match wins. If none match, the UI renders the
raw YAML as a fallback.

## Engine Changes

### Worker record (worker-api)

Add a nullable `definitionRef` field to `io.casehub.worker.api.Worker`:

```java
public record Worker(String name, Set<String> capabilities,
                     WorkerFunction<?, ?> function,
                     ExecutionPolicy executionPolicy,
                     String description,
                     String definitionRef) {  // new, nullable
```

Backward-compatible constructor passes `null` for `definitionRef`. The
`Builder` gains `.definitionRef(String)`.

### CaseDefinitionYamlMapper

Parse `definitionRef` from worker YAML nodes and pass it to
`Worker.builder().definitionRef(...)`. No file resolution — the string is
stored as-is.

### Schema model (jsonschema2pojo)

Add `definitionRef` to the Worker JSON Schema so the generated
`io.casehub.model.Worker` class includes the field.

### CasehubCallableTaskBuilder (casehub-engine-flow)

Parse `definitionRef` from `call: casehub:dispatch` step properties and store
it in the `FlowWorkerFunction` or as step metadata. The UI reads it when
rendering SWF diagrams.

### definitions: block

Parse the top-level `definitions:` block in `CaseDefinitionYamlMapper` and
store it as a `Map<String, JsonNode>` on `CaseDefinition`. The engine does not
interpret the content — it is opaque YAML for UI consumption.

`CaseDefinition.getDefinitions()` returns the map. `null` or empty when no
`definitions:` block exists.

## What Is Not In Scope

- **Runtime snapshot changes** — DagNodeSnapshot, LeafTaskSnapshot,
  PrimitiveItemSnapshot, AgendaItemSnapshot, ExecutionStateSnapshot are
  unchanged. Runtime drill-down is a separate concern.
- **REST endpoint changes** — PlanResource endpoints are unchanged.
- **Parse-time validation** — the engine does not verify that referenced files
  exist or contain valid YAML.
- **YAML schemas for HTN and DAG** — tracked in #987 and #978. The
  `definitionRef` convention applies to them once their YAML surfaces exist.
- **UI rendering** — diagram type detection, drill-down pane, workbench
  composition are tracked in blocks-ui.

## Test Plan

- [ ] `CaseDefinitionYamlMapper` parses `definitionRef` on worker definitions
- [ ] `CaseDefinitionYamlMapper` parses `definitions:` block and stores as
      `Map<String, JsonNode>` on `CaseDefinition`
- [ ] `Worker.builder().definitionRef("path/to/file.yaml")` stores the value
- [ ] `definitionRef` round-trips through JSON serialization
- [ ] Inline ref (`#name`) is stored as-is — no resolution at parse time
- [ ] Missing `definitionRef` defaults to null (backward compatible)
- [ ] `CasehubCallableTaskBuilder` preserves `definitionRef` on dispatch steps

## References

- [engine#951](https://github.com/casehubio/engine/issues/951) — this issue
- [engine#978](https://github.com/casehubio/engine/issues/978) — epic: pure-YAML execution model and DSL completeness
- [engine#987](https://github.com/casehubio/engine/issues/987) — YAML HTN decomposition tree
- [DSL-STYLE-GUIDE.md §YAML/Java Parity Principle](https://raw.githubusercontent.com/casehubio/parent/main/docs/DSL-STYLE-GUIDE.md) — platform principle driving YAML coverage
- `CaseDefinitionYamlMapper.java` — YAML → CaseDefinition mapping
- `Worker.java` (worker-api) — runtime Worker record
- `CasehubCallableTaskBuilder.java` — casehub:dispatch step builder
- `FlowWorkerFunctionProvider.java` — SWF function detection
