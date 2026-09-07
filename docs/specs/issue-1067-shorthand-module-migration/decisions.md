## D1: AdaptationConfig → ShorthandModule

**Choice:** Register `AdaptationConfig.class` in platform's `ShorthandModule` with manually-crafted scalar (string enum) and object schemas.
**Alternatives:**
- Keep in post-processor — works but misses the generation-time interception pattern
- Auto-generate object variant from Java type — can't, because the YAML schema includes `revision` (backward-compat alias not on the Java record)
**Rationale:** ShorthandModule is the platform convention for scalar-or-object oneOf. AdaptationConfig is a clean fit — string presets ("adaptive", "conservative", "off", "progress") or full object.
**Trade-offs:** Object schema still hand-crafted (same code, different location). Value is convention alignment, not auto-generation.
**Sources:** `SchemaPostProcessor.buildAdaptation()`, `AdaptationConfig.java`, platform `ShorthandModule.java`
**Exploration:** quick
**Status:** captured

## D2: ExpressionOrOverride — remove redundant post-processor overwrite

**Choice:** Remove `buildExpressionOrOverride()` from `SchemaPostProcessor.addMissingDefs()`. `ExpressionEvaluatorModule` already produces the identical oneOf during generation.
**Alternatives:**
- Refactor ExpressionEvaluatorModule to delegate to ShorthandModule — requires handling `isAssignableFrom` (subtypes), ShorthandModule only does exact match. Not worth a platform API change.
**Rationale:** The post-processor overwrites the module's output with an identical schema. Pure dead code.
**Trade-offs:** None — removing redundancy.
**Sources:** `ExpressionEvaluatorModule.java` (line 46, `buildExpressionOrOverrideSchema`), `SchemaPostProcessor.addMissingDefs()` (line 149)
**Exploration:** quick
**Status:** captured

## D3: Trigger sub-defs → TriggerModule

**Choice:** Move `buildCloudEventTrigger()`, `buildScheduleTrigger()`, `buildScopeActivatedTrigger()`, and `buildContextChangeTrigger()` into `TriggerModule`. TriggerModule already owns trigger schema generation and references these defs via `$ref`.
**Alternatives:**
- Use ShorthandModule for CloudEventTrigger — can't, because TriggerModule's CustomDefinition for Trigger.class prevents victools from traversing sub-types
- Keep in post-processor — works but scatters trigger schema logic across two files
**Rationale:** TriggerModule already generates the Trigger discriminated union with `$ref`s to these defs. Having the module also define what those refs point to is the natural ownership boundary.
**Trade-offs:** TriggerModule grows from ~64 to ~160 lines. Acceptable — it owns the domain.
**Sources:** `TriggerModule.java`, `SchemaPostProcessor.addMissingDefs()`, `CloudEventTrigger.java`
**Exploration:** quick
**Status:** captured
