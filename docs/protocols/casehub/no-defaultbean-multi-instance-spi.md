---
id: PP-20260921-b7c277
title: "Never use @DefaultBean on multi-instance SPIs"
type: rule
scope: platform
applies_to: "Any SPI where multiple implementations coexist (CapabilityArea, EnvironmentObserver)"
severity: critical
refs:
  - docs/guides/contributor-guide.md
  - runtime-core/src/main/java/io/casehub/engine/internal/improvement/area/
violation_hint: "@DefaultBean on a CapabilityArea implementation — all 9 other default areas silently disappear when one custom bean is registered"
created: 2026-09-21
---

`@DefaultBean` is correct for 1:1 SPI replacement (e.g. `InMemoryResearchCorpus` → custom `ResearchCorpus`). It is wrong for multi-instance SPIs where multiple implementations coexist. In Quarkus Arc, `@DefaultBean` suppresses ALL default beans of the same type when ANY non-default bean exists — a consumer providing one custom `CapabilityArea` without `@DefaultBean` silently removes all other default areas from CDI resolution. Use plain `@ApplicationScoped` and override via the registry's `register()` method with a higher-priority startup observer instead.
