---
layout: post
title: "What the GOAP Planner Can't See"
date: 2026-08-22
entry_type: note
subtype: diary
projects: [casehub-engine]
tags: [goap, planning, integration-testing, adaptive]
---

The GOAP planning strategy had comprehensive unit tests against mocks — every A* path, every cost calculation, every precondition chain. What it didn't have was a single test that started a real case and watched it complete through the engine's dispatch pipeline. That gap is the kind that lets you ship a planner that works perfectly in isolation and fails at the boundary.

We wrote six integration tests covering the core scenarios: dependency-ordered dispatch, adaptive replanning when context changes mid-execution, guard-filtered bindings, failure reroute with backup agents, empty plans that wait for signals, and cost-optimal path selection. All follow the established `SequentialStrategyIntegrationTest` pattern — inner `CaseHub` subclass, `Awaitility`, assert on `CaseInstanceCache`.

The interesting finding came from test 3. The original design had a binding with a trigger guard (`.ready == true`) that would become eligible after an earlier step wrote `ready` to context. The GOAP strategy should handle this naturally — plan with what's available, replan when new bindings appear.

It can't. The A* planner requires all actions needed for a complete goal path to be in the eligible set *at planning time*. When the trigger guard removes a binding from eligibility, the planner can't bridge the gap. It doesn't do partial planning — it either finds a complete path or returns empty. No dispatch, no progress, deadlock.

This is a fundamental property of A* forward search, not a bug. GOAP works on the assumption that the action space is fully known. In a case engine where binding eligibility is dynamic — controlled by trigger conditions that evaluate against evolving context — that assumption breaks. The planner and the trigger system operate on different information horizons: the planner sees only currently-eligible actions, but the goal may require actions that don't become eligible until earlier actions complete.

We redesigned the test to verify what actually works: GOAP correctly ignores trigger-guarded bindings and plans with the eligible subset. The adaptive strategy handles the replanning naturally — after each step completes and the world state evolves, it replans from the actual state rather than the declared effects, picking up cheaper paths that emerge from undeclared worker output. That's the adaptive strategy earning its name.

The gap — dynamic eligibility with GOAP — isn't something to paper over with a workaround. It's a design constraint that shapes how case definitions should be structured: either all GOAP-planned bindings share the same trigger (so they're all eligible simultaneously), or the goal conditions match what the currently-eligible actions can achieve. Mixing trigger-based gating with GOAP-based planning creates an impedance mismatch that the planner can't resolve.
