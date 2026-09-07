# Shorthand Module Migration — Design Spec

**Date:** 2026-09-07
**Issue:** engine#1067
**Parent epic:** engine#1058 (schema DSL modernization)

## Problem

`SchemaPostProcessor` (1728 lines) builds shorthand schemas (scalar-or-object `oneOf` patterns) as raw JSON during post-processing. Platform now provides `ShorthandModule` for this pattern. Three shorthand methods should migrate from post-processing to generation-time.

## Scope

Three migrations, each moving schema logic from post-processing to generation-time:

### 1. AdaptationConfig → ShorthandModule

Register `AdaptationConfig.class` with `ShorthandDefinition` in `CaseHubSchemaGenerator`:
- **Scalar:** string enum (`adaptive`, `conservative`, `off`, `progress`)
- **Object:** full config with `trigger`, `optimization`, `revision`, `threshold`, `metaReasoner`, `repair`, `contingencyThreshold`

Changes:
- Remove `"AdaptationConfig"` from `UNWANTED_DEFS`
- Remove `buildAdaptation()` method
- Remove `specProps.set("adaptation", buildAdaptation())` from `expandSpecProperties()` (keep the property rename `adaptationConfig` → `adaptation`)
- Add `ShorthandModule` with `AdaptationConfig.class` registration to `CaseHubSchemaGenerator`

### 2. ExpressionOrOverride — remove redundancy

`ExpressionEvaluatorModule` already generates the correct `oneOf: [string, {lang: expr} map]` schema during generation. The post-processor's `buildExpressionOrOverride()` overwrites it with an identical schema.

Changes:
- Remove `defs.set("ExpressionOrOverride", buildExpressionOrOverride())` from `addMissingDefs()`
- Delete `buildExpressionOrOverride()` method
- Keep `renameDef("ExpressionEvaluator", "ExpressionOrOverride")` — the naming convention is YAML-facing

### 3. Trigger sub-defs → TriggerModule

`TriggerModule` generates the `Trigger` discriminated union referencing `#/$defs/CloudEventTrigger` etc., but relies on `SchemaPostProcessor.addMissingDefs()` to create those defs. Move all four trigger sub-type schemas into TriggerModule.

Changes:
- Extend `TriggerModule.applyToConfigBuilder()` to register `CustomDefinitionProvider` entries for `CloudEventTrigger.class`, `ScheduleTrigger.class`, `ScopeActivatedTrigger.class`, `ContextChangeTrigger.class`
- `CloudEventTrigger` keeps its shorthand pattern: `oneOf: [string (type match), object (full spec)]`
- Remove all four `build*Trigger()` methods and their calls from `addMissingDefs()`

## What stays in SchemaPostProcessor

Everything non-shorthand: property renames, structural fixes, required/default annotations, string validation, codegen directives, LLM provider constraints, root metadata, and all pure-object `build*()` methods (Reflection, Monitoring, PlanningConstraints, RecoveryPolicy, etc.).

## Verification

`SchemaDriftTest` compares committed `CaseDefinition.yaml` against generator output. After migration:
1. Run generator — if structural differences exist (e.g., `$ref` to `$defs/AdaptationConfig` vs inline), update the committed schema
2. Run `SchemaDriftTest` — must pass

## References

- `generator/src/main/java/io/casehub/generator/SchemaPostProcessor.java` — source of shorthand methods
- `generator/src/main/java/io/casehub/generator/CaseHubSchemaGenerator.java` — module registration
- `generator/src/main/java/io/casehub/generator/module/ExpressionEvaluatorModule.java` — existing shorthand module
- `generator/src/main/java/io/casehub/generator/module/TriggerModule.java` — trigger schema owner
- `platform/schema-generator/.../ShorthandModule.java` — platform shared module
- `platform/schema-generator/.../ShorthandDefinition.java` — shorthand SPI
- `api/src/main/java/io/casehub/api/model/AdaptationConfig.java` — Java record
- `docs/specs/2026-09-07-schema-dsl-modernization-design.md` — parent epic design
