# io.casehub.api.model.WatchdogResponseAction

**Package:** `io.casehub.api.model`

**Kind:** `enum`

Engine response action for a watchdog alert condition.

<p>`CANCEL_AFFECTED` cancels hung workers via synthetic `Expired` outcome, routing
them through the existing failure pipeline (retry, reroute, RecoveryCoordinator).

<p>`IGNORE` takes no engine-side action — the watchdog alert is observed but not acted on.

## Enum Constants

### `CANCEL_AFFECTED` (`io.casehub.api.model.WatchdogResponseAction`)

### `IGNORE` (`io.casehub.api.model.WatchdogResponseAction`)

## Constructors

### `private WatchdogResponseAction()`

## Methods

### `public static io.casehub.api.model.WatchdogResponseAction valueOf(java.lang.String name)`

#### Parameters

- `name` (`java.lang.String`)

### `public static io.casehub.api.model.WatchdogResponseAction[] values()`
