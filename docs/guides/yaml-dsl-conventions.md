# CaseHub YAML DSL Conventions

## Field Naming

All YAML fields use **camelCase**: `inputProjection`, `contextChange`, `outcomePolicy`.

## Naming Constraints

- `namespace`, `name`: alphanumeric + hyphens, 1–63 chars, must start/end with alphanumeric (`^[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?$`)
- `dsl`, `version`: semantic versioning (`X.Y.Z`, optional pre-release/build metadata)

## Scalar-or-Object Shorthand

Several types accept a scalar shorthand that expands to a full object:

| Type | Scalar | Object |
|------|--------|--------|
| `adaptation` | `adaptation: adaptive` | `adaptation: {trigger: every-step, revision: forward-replan}` |
| `ExpressionOrOverride` | `".field"` (default lang) | `{jq: ".field"}` or `{mvel: "expr"}` |
| `CloudEventTrigger` | `"order.created"` (type match) | `{type: "order.created", source: "...", filter: "..."}` |

The scalar form always uses the most common configuration. Use the object form when you need non-default options.

## Sealed Type Discriminators

Binding targets use **exactly one** of the target-type keys:

```yaml
bindings:
  - name: worker-binding
    capability: process          # CapabilityTarget
    on: { contextChange: {} }

  - name: human-binding
    humanTask:                   # HumanTaskTarget
      title: "Review document"
    on: { contextChange: {} }

  - name: judgment-binding
    judgment:                    # JudgmentTarget
      prompt: "Assess risk"
    on: { contextChange: {} }

  - name: subcase-binding
    subCase:                     # SubCaseTarget
      namespace: billing
      name: invoice
      version: "1.0.0"
    on: { contextChange: {} }

  - name: signal-binding
    signal:                      # SignalTarget
      status: "notified"
    on: { schedule: { every: PT48H } }
```

Triggers follow the same pattern — exactly one of `contextChange`, `cloudEvent`, `schedule`, `scopeActivated`.

## Worker Extension Properties

Workers allow additional properties for plugin integration:

```yaml
workers:
  - name: custom-worker
    capabilities: [process]
    myCustomPlugin:
      endpoint: "http://localhost:8080"
```

Known extension blocks: `agent`, `a2a`, `mcp`, `react`, `do` (SWF). Custom plugin properties are passed through to `WorkerFunctionProvider` implementations.

## Map-Keyed Types

Maps with typed values use `additionalProperties`:

```yaml
routingSignalWeights:    # Map<String, Double>
  workload: 0.3
  trust: 0.3
  experience: 0.4

goapActions:             # Array of structured objects
  - name: gather-evidence
    preconditions: { hasCase: true }
    effects: { evidenceGathered: true }
    cost: 1.0
```

## Expression Language Override

The default expression language is JQ (set via `expressionLang:` at root level). Any expression field can override per-use:

```yaml
expressionLang: jq                    # definition default

spec:
  bindings:
    - name: jq-binding
      on:
        contextChange:
          filter: '.amount > 1000'    # uses default (JQ)

    - name: mvel-binding
      on:
        contextChange:
          filter:
            mvel: 'amount > 1000'     # per-expression override
```

## Template Expressions

JQ template expressions (`${...}`) in string values are evaluated at runtime. Numeric schema fields accept string values to support this pattern:

```yaml
agent:
  model:
    openai:
      apiKey: "${$secret.openai.apiKey}"
      temperature: "${$config.model.temperature | tonumber}"
```

## Completion Block

Goal kinds map to terminal statuses. Built-in kinds (`success`, `failure`) have implicit status. Custom kinds require explicit status:

```yaml
completion:
  success:
    allOf: [dataGathered, analysisComplete]
  failure:
    anyOf: [timeout, insufficientData]
  escalated:                          # custom kind — explicit status required
    allOf: [needsHumanReview]
```
