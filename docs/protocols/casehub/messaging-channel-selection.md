---
id: PP-20261031-060886
title: "Use qhorus channels for agent messaging, platform notifications for human delivery"
type: rule
scope: platform
applies_to: "Any code that needs to communicate state changes, events, or commands to agents or humans"
severity: error
refs:
  - parent/docs/platform/messaging-vs-notifications.md
  - parent/docs/platform/notifications.md
  - parent/docs/CHANNELS.md
  - qhorus/docs/messaging-architecture.md
violation_hint: "Calling MessageDispatcher directly from engine runtime, or posting agent-targeted messages through platform notifications instead of qhorus channels"
created: 2026-10-31
---

## Rule

Engine code that needs to communicate with agents within a case MUST use `CaseChannelProvider`, never `MessageDispatcher` directly. Engine code MUST NOT use platform notifications for agent-to-agent communication.

## Rationale

The two systems serve distinct purposes:

- **Qhorus channels** — agent-to-agent structured messaging within cases. Persistent, ordered, type-enforced, with protocol-level acknowledgement via `CommitmentState` (OPEN → ACKNOWLEDGED → FULFILLED).
- **Platform notifications** — cross-cutting delivery to human/system targets. Subscription-driven with preferences, suppression, quiet hours, digest buffering. Engagement tracking only (OPENED/CLICKED), not protocol acknowledgement.

## Practical rules

1. **Agent in the same case needs to know something** → post to the appropriate qhorus channel via `CaseChannelProvider.postToChannel()`. Use the `coordination` channel for engine-level state changes.

2. **Agent needs to acknowledge or act** → use `MessageType.COMMAND` on the channel. Qhorus commitment tracking handles the obligation lifecycle.

3. **State change agents should react to** → use `MessageType.STATUS` (agent-visible). Do NOT use `MessageType.EVENT` — it has `isAgentVisible() == false`.

4. **Telemetry/observability data** → use `MessageType.EVENT` on the `observe` channel. Invisible to agents, visible to monitoring systems.

5. **Human needs to be informed** → use platform `NotificationDispatcher`. Do NOT post to qhorus channels for human delivery.

6. **Engine runtime code** → always go through `CaseChannelProvider` SPI, never inject `MessageDispatcher` or `MessageService` directly. App-level bridges (Claudony, OpenClaw) own the qhorus translation.
