# io.casehub.api.model.coordination.SupervisorGateBehavior

**Package:** `io.casehub.api.model.coordination`

**Kind:** `interface`

Marker for components that implement `PlatformRole.SUPERVISOR` gating behavior — the
ability to pause execution, wait for approval, and resume with a decision.

<p>Implementors operate at different granularities:

<ul>
  <li>`io.casehub.api.spi.OversightGateService` — worker-action level, gates individual
      `PlannedAction`s
  <li>`ConductorInboxManager` — evolution-pipeline level, gates research pipeline stages
</ul>

<p>Classification components that decide <em>whether</em> gating is needed (such as `io.casehub.api.spi.ActionRiskClassifier`) are not gate implementations and do not carry this
marker. They are the triage step that precedes the gate.
