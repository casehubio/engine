---
id: PP-20260915-aa504e
title: "YAML, DSL, and Annotation pathways must expose identical API surface"
type: rule
scope: platform
applies_to: "engine-api, annotations, schema — any new CaseDefinition field or capability"
severity: important
refs:
  - docs/specs/issue-1097-cbr-ensemble-retrieve-for-selection/2026-09-15-cbr-ensemble-gaps-design.md
violation_hint: "A CaseDefinition field (e.g. CbrConfig) is available via Java builder but missing from the YAML schema or @Case annotations — or vice versa."
created: 2026-09-15
---

When a feature is added to any of the three case definition pathways (YAML schema, Java DSL builder, or `@Case` annotations), it must be available in all three. Gaps between pathways are bugs, not deferred work. The three pathways are peer representations of the same model — a capability missing from one pathway means that pathway's users cannot access a platform feature. Example: `crossType` and `problemDescription` were on `CbrConfig` (Java) and handled by `CbrConfigDeserializer` (runtime) but missing from the YAML JSON Schema (validation) and had no `@Cbr` annotation — fixed in engine#1097/#1098.
