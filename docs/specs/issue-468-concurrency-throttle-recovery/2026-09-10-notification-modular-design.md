# Platform Notification Service — Modular Message Types and Delivery Routes

**Issue:** casehubio/parent#471
**Date:** 2026-09-10

## Overview

Make the platform notification service modular along two axes: message types (human, agent, operational) and delivery routes (inbox, CDI event, CloudEvent, webhook). Then migrate 6 bespoke notification mechanisms to the unified pipeline.

## Foundation Changes (casehub/platform)

### 1. TargetKind enum

```java
// platform-api
public enum TargetKind {
  USER,     // human — full suppression, preferences, inbox persistence
  NON_USER  // agent or system — skip suppression, fire-and-forget delivery
}
```

`TargetType` gains `AGENT` and `SYSTEM` values alongside existing `USER`, `GROUP`, `EVENT_FIELD`.

### 2. ResolvedTarget record

```java
// platform-api
public record ResolvedTarget(String targetId, TargetKind kind) {}
```

`TargetResolver.resolve()` changes return type from `Set<String>` to `Set<ResolvedTarget>`. Existing `USER`/`GROUP`/`EVENT_FIELD` targets resolve with `kind = USER`. New `AGENT`/`SYSTEM` targets resolve with `kind = NON_USER`.

### 3. NotificationDispatcher — conditional pipeline

Rename `dispatchToUser()` to `dispatchToTarget()`. Dispatch based on `TargetKind`:

- **USER**: full pipeline (preferences → suppression → template → channel routing → delivery + tracking)
- **NON_USER**: minimal pipeline (template → delivery). No preferences, no suppression, no digest, no quiet hours.

### 4. CdiEventDeliverer

New `NotificationDeliverer` implementation:

```java
@ApplicationScoped
public class CdiEventDeliverer implements NotificationDeliverer {
    @Inject Event<NotificationInput> notificationEvents;

    @Override public String channelId() { return "cdi-event"; }

    @Override public DeliveryResult deliver(NotificationInput notification) {
        notificationEvents.fireAsync(notification);
        return new DeliveryResult(true, null);
    }
}
```

Agents and systems observe `@ObservesAsync NotificationInput` filtered by category. Fire-and-forget — no persistence.

### 5. ChannelRouter extension

When routing a NON_USER target, skip preference-based routing. Use a default channel mapping:
- NON_USER targets → `CdiEventDeliverer` (unless subscription specifies a different channel)

## Migration Batches

### Batch 1: Foundation + Watchdog alerts (proof-of-concept)

**Platform changes:** TargetKind, ResolvedTarget, dispatcher conditional, CdiEventDeliverer.
**Migration:** Register `WatchdogAlertEvent` as subscribable. Default subscription for operational alerts → SYSTEM target → CdiEventDeliverer. Engine#1066 observes the CDI notification instead of bespoke qhorus channel posting.

### Batch 2: CapacityPressureMonitor (platform)

Register `CapacityPressureEvent` as subscribable. Replace bespoke `fireAsync(CapacityPressureEvent)` with subscription-matched notification.

### Batch 3: CertificateExpiryScheduler (platform)

Register certificate expiry events. Replace bespoke CDI path.

### Batch 4: PathologyCondition (engine)

Register engine pathology detection as subscribable. Replace bespoke `runtime.signal()` context bridge with subscription-matched notification. Case definitions can subscribe for case-specific pathology alerts.

### Batch 5: CaseLifecycleEvent (engine)

Register case lifecycle transitions as subscribable. 6+ existing CDI observers remain (they're internal), but user/agent-facing notifications go through subscriptions instead of bespoke observers.

### Batch 6: RAS SituationChangeEvent (platform)

Register RAS situation changes. Replace bespoke event→CloudEvent bridge with subscription-matched notification through a CloudEvent deliverer.

## Testing Strategy

- Foundation: unit tests for `TargetResolver` with AGENT/SYSTEM targets, `NotificationDispatcher` conditional pipeline (suppression skipped for NON_USER), `CdiEventDeliverer`
- Each migration: verify the subscribable event fires through the pipeline, existing human notification path unchanged

## References

- `NotificationDispatcher` (platform, notification-dispatch) — existing pipeline
- `TargetResolver` / `TargetType` / `NotificationTarget` (platform-api) — target resolution
- `NotificationDeliverer` / `InAppNotificationDeliverer` (platform-api/notification-dispatch) — delivery SPI
- `SubscriptionEngine` / `Subscription` / `SubscriptionMatched` (platform-api/subscriptions) — event matching
- `WatchdogEvaluationService` (qhorus runtime) — first migration target
- engine#1066 — watchdog notification migration (unblocked by Batch 1)
- parent#468 — parent epic (surfaced this requirement)
