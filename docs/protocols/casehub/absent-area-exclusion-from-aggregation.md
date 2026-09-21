---
id: PP-20260921-001bc9
title: "Exclude ABSENT areas from weighted health aggregation"
type: rule
scope: repo
applies_to: "HealthScoreTracker, any weighted aggregation over CapabilityArea assessments"
severity: important
refs:
  - runtime-core/src/main/java/io/casehub/engine/internal/improvement/HealthScoreTracker.java
  - specs/issue-1131-evolution-readiness/2026-09-21-evolution-readiness-methodology-design.md
violation_hint: "Composite health score unexpectedly low despite all measured areas being healthy — neutral 0.5 scores from ABSENT areas dragging the weighted average down"
created: 2026-09-21
---

When aggregating health scores across capability areas, skip any area whose assessment returns `LandscapePosition.ABSENT`. ABSENT means the area has no data — it is present in the registry but not producing a meaningful signal. Including neutral scores (0.5) in the weighted average distorts the composite: 7 neutral areas at 0.5 drag 3 healthy areas scoring 0.9 down to ~0.66, barely above the circuit breaker's 0.6 threshold, despite every measured area being healthy.
