## D1: Design-time only — no runtime snapshot changes

**Choice:** `definitionRef` is a design-time YAML convention for UI drill-down. No changes to runtime snapshot types, REST endpoints, or execution paths.
**Alternatives:**
- Runtime snapshot enrichment — add definitionType + yamlSource to DagNodeSnapshot, LeafTaskSnapshot, etc. Deferred until YAML parity (#978) makes it worthwhile.
**Rationale:** The immediate need is UI diagram navigation at design time. Runtime is a separate concern with different constraints (serialization, persistence, payload size).
**Trade-offs:** Runtime drill-down (e.g. from execution workbench) is not addressed yet.
**Sources:** PlanResource.java, ExecutionStateSnapshot.java, DagNodeSnapshot.java
**Exploration:** quick
**Status:** captured

## D2: Explicit definitionRef field + inline definitions

**Choice:** Workers carry an explicit `definitionRef:` field — either a relative file path (`workflows/research.yaml`) or an inline reference (`#name` pointing to a `definitions:` block in the same YAML file).
**Alternatives:**
- Convention-based lookup (resolve by name against a directory structure) — implicit, fragile, requires directory conventions
- Registry index file (central index.yaml mapping names to paths) — single file to maintain, coupling
**Rationale:** Explicit refs are unambiguous, work without a central index, and support both external files and inline definitions. The `#name` syntax avoids file sprawl for small embedded definitions.
**Trade-offs:** Every reference must be manually authored — no auto-discovery.
**Sources:** CaseDefinitionYamlMapper.java, Worker.java (worker-api)
**Exploration:** quick
**Status:** captured

## D3: Structural type detection — no explicit format field

**Choice:** The UI detects diagram type from YAML structure: `workers:` + `bindings:` → Case, `do:` → SWF, `decomposition:` → HTN, `nodes:` + `dependsOn:` → DAG. No explicit `format:` field on definitions.
**Alternatives:**
- Explicit type field (`format: case|swf|htn|dag`) — unambiguous but redundant with structure
**Rationale:** Each format is already self-identifying from its YAML keys. An explicit field adds maintenance burden and can drift from the actual content. Structural detection is consistent with how YAML formats are already distinguished.
**Trade-offs:** Detection logic must be maintained as formats evolve. A new format requires updating the detector.
**Sources:** CaseDefinitionYamlMapper (detects `do:`, `agent:`, `a2a:`, `mcp:`, `react:` blocks structurally)
**Exploration:** quick
**Status:** captured

## D4: Engine passes definitionRef through opaquely

**Choice:** The engine stores `definitionRef` as a plain string on the `Worker` record. No parse-time file resolution, no validation that the referenced file exists. The UI resolves refs at render time.
**Alternatives:**
- Parse-time validation (mapper checks file exists, pre-loads content) — catches errors early but couples engine to filesystem layout
**Rationale:** The engine is a runtime. File layout is a deployment/tooling concern. Coupling the mapper to filesystem resolution adds complexity and prevents YAML definitions from being used in contexts where the filesystem layout differs (e.g. bundled JARs, remote registries).
**Trade-offs:** Broken refs are only caught at design time in the UI, not at definition load time.
**Sources:** WorkerFunctionProvider.java, CaseDefinitionYamlMapper.java
**Exploration:** quick
**Status:** captured

## D5: Cross-format convention — definitionRef on any YAML node

**Choice:** `definitionRef` is a cross-format convention, not a CaseHub-specific field. Any YAML node that references an external definition can carry it:
- Case workers: `definitionRef:` on the worker
- SWF casehub steps: `definitionRef:` on the `call: casehub:dispatch` step (our callable, our extension)
- SWF third-party steps: `metadata.definitionRef:` (SWF-compliant extension point)
- HTN leaf tasks: `definitionRef:` on the task (when YAML lands via #987)
- DAG nodes: `definitionRef:` on the node (when YAML lands)
**Alternatives:**
- CaseHub-only field on Worker — simpler but breaks drill-down at SWF → Case boundaries
**Rationale:** The drill-down graph spans format boundaries (Case → SWF → Case → ...). A uniform convention means the UI has a single resolution mechanism regardless of which format it appears in. As long as a diagram follows the standard, it gets drill-down.
**Trade-offs:** Third-party formats use `metadata.definitionRef:` (a convention, not a spec requirement). The UI must check both `definitionRef` and `metadata.definitionRef`.
**Sources:** CasehubCallableTaskBuilder.java (casehub:dispatch callable), FlowWorkerFunctionProvider.java
**Exploration:** quick
**Depends on:** D2 (definitionRef field), D3 (structural detection)
**Status:** captured
