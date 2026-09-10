# CBR and Playbook Selection Guide

> Learn by doing: how Case-Based Reasoning turns past outcomes into better decisions.

---

## What CBR Does

Case-Based Reasoning (CBR) in CaseHub stores what happened in past cases and retrieves similar ones to inform current decisions. Instead of starting from scratch, new cases benefit from experience: which agents performed well, which playbooks resolved similar situations, and what plan traces led to success or failure.

The cycle has three phases:

1. **Retain** — when a case reaches a terminal state (COMPLETED, FAULTED, CANCELLED), `CbrCaseRetainObserver` stores a `PlanCbrCase` with the problem description, execution trace, features, outcome, and confidence.

2. **Retrieve** — when a new case needs to make a decision (routing an agent, selecting a playbook, decomposing a plan), `CbrRetrievalService` queries the CBR store for similar past cases using extracted features.

3. **Adapt** — retrieved plans are adapted to the current situation via `PlanAdapter`. Steps that are irrelevant to the current case can be REMOVED, high-value steps BOOSTED, and alternatives SUBSTITUTED.

---

## Configuring CBR

### Minimal YAML

Every case definition that wants CBR needs a `cbr:` block under `spec:` with at least a `features:` map and a `domain:`:

```yaml
dsl: "0.1.0"
namespace: soc
name: phishing-investigation
version: "1.0.0"
spec:
  cbr:
    domain: "soc-incidents"
    features:
      severity: ".alert.severity"
      category: ".alert.category"
      source_ip: ".alert.sourceIp"
```

Features are JQ expressions evaluated against the case context's working layer. Each expression extracts one value that becomes a dimension for similarity comparison.

### Full Configuration

```yaml
spec:
  cbr:
    # Required
    features:
      severity: ".alert.severity"
      category: ".alert.category"
      amount: ".transaction.amount"

    # Domain — groups related case types for retrieval
    domain: "soc-incidents"

    # Feature weights — higher weight = more influence on similarity
    weights:
      severity: 2.0
      category: 1.5
      amount: 1.0

    # Retrieval tuning
    topK: 5                          # max results (default: 5)
    minSimilarity: 0.3               # minimum similarity threshold (default: 0.0)
    vectorWeight: 0.6                # balance between vector and structural similarity (default: 0.5)

    # Retrieval timing
    timing: per-evaluation           # per-evaluation (default) or case-lifetime

    # Temporal decay — older cases contribute less
    temporalDecayHalfLifeDays: 90    # cases lose half their relevance after 90 days

    # Cross-type retrieval — compare across all case definition types
    crossType: true                  # default: false

    # Case type filter — scope retrieval to a specific type (mutually exclusive with crossType)
    # caseType: "phishing"

    # Problem description — enriches retained cases for text search
    problemDescription: '(.alert.severity + " " + .alert.category + " incident")'

    # CBR case type — which Java class to deserialize (default: "plan")
    # cbrType: "plan"

    # Learned cost samples threshold
    minCostSamples: 5
```

### Java DSL

```java
CaseDefinition.builder()
    .namespace("soc")
    .name("phishing-investigation")
    .version("1.0.0")
    .cbrConfig(CbrConfig.builder()
        .feature("severity", ".alert.severity")
        .feature("category", ".alert.category")
        .weight("severity", 2.0)
        .domain("soc-incidents")
        .topK(5)
        .minSimilarity(0.3)
        .problemDescription("(.alert.severity + \" incident: \" + .alert.category)")
        .build())
    // ... bindings, goals, etc.
    .build();
```

For lambda-based feature extraction (Java DSL only):

```java
CbrConfig.builder()
    .featureExtractor(ctx -> Map.of(
        "severity", ctx.get("alert.severity"),
        "amount", ctx.get("transaction.amount")))
    .domain("aml-cases")
    .build();
```

---

## Feature Design

Features are the dimensions CBR uses to find similar cases. Good feature design is the single most important factor in CBR effectiveness.

### Choosing Features

Pick features that distinguish cases with different outcomes:

```yaml
# Good — these features predict which playbook will succeed
features:
  attack_vector: ".alert.attackVector"     # phishing vs malware vs insider
  severity: ".alert.severity"              # HIGH vs LOW affects response urgency
  data_classification: ".asset.classification"  # PII vs public changes the playbook

# Bad — too generic, doesn't discriminate
features:
  timestamp: ".alert.timestamp"            # every case has a unique timestamp
  id: ".alert.id"                          # unique per case, zero predictive value
```

### Feature Types

CaseHub supports several feature value types, auto-detected from JQ output:

| JQ result | FeatureValue type | Example |
|-----------|-------------------|---------|
| String | `StringVal` | `"HIGH"`, `"phishing"` |
| Number | `NumberVal` | `50000`, `0.85` |
| Boolean | `StringVal` | `"true"`, `"false"` |
| Array of strings | `StringListVal` | `["email", "web"]` |
| Array of numbers | `NumberListVal` | `[80, 443, 8080]` |
| Object | `StructVal` | `{"ip": "10.0.0.1", "port": 443}` |

### Feature Weights

Weights control how much each feature contributes to the similarity score. Higher weight = more influence:

```yaml
weights:
  attack_vector: 3.0    # most important — determines playbook type
  severity: 2.0         # important — affects urgency and agent selection
  source_ip: 0.5        # weak signal — same IP != same attack
```

Unweighted features default to equal weight. Start with equal weights and adjust based on observed retrieval quality.

---

## Playbooks and Cross-Type Retrieval

A "playbook" is a case definition — it defines the bindings, goals, and worker orchestration for handling a specific type of situation. A SOC might have:

- `phishing-investigation` — email-based threats
- `ransomware-response` — encryption-based attacks  
- `insider-threat` — credential misuse

### Same-Type Retrieval (default)

By default, CBR scopes queries to the current case definition name. A phishing case only retrieves past phishing cases:

```yaml
name: phishing-investigation
spec:
  cbr:
    domain: "soc-incidents"
    features:
      severity: ".alert.severity"
    # caseType defaults to "phishing-investigation"
```

### Cross-Type Retrieval

With `crossType: true`, CBR retrieves across all case types in the domain. Each result carries its originating `caseType`, so you can compare:

```yaml
name: alert-triage
spec:
  cbr:
    domain: "soc-incidents"
    crossType: true
    features:
      severity: ".alert.severity"
      attack_vector: ".alert.attackVector"
      indicators: ".alert.indicators"
```

Retrieved experiences will include results from phishing, ransomware, and insider-threat cases. Each `RetrievedExperience.caseType()` tells you which playbook produced that outcome.

### Playbook Selection Before startCase()

`CbrRetrievalService.retrieveForSelection()` enables playbook comparison without creating a case first. Call it at situation evaluation time with trigger event features:

```java
// At situation evaluation — before choosing which playbook to activate
List<RetrievedExperience> experiences = cbrRetrievalService.retrieveForSelection(
    tenancyId,
    "soc-incidents",                                    // domain
    Map.of("severity", FeatureValue.string("HIGH"),
           "attack_vector", FeatureValue.string("email")),  // trigger features
    10,                                                 // topK
    0.3,                                                // minSimilarity
    Map.of("attack_vector", 2.0));                      // weights

// Group by playbook type and compare outcomes
Map<String, List<RetrievedExperience>> byType = experiences.stream()
    .collect(Collectors.groupingBy(RetrievedExperience::caseType));

// Select the playbook with the best historical outcome
String bestPlaybook = byType.entrySet().stream()
    .max(Comparator.comparingDouble(e -> e.getValue().stream()
        .mapToDouble(exp -> exp.confidence() != null ? exp.confidence() : 0.0)
        .average().orElse(0.0)))
    .map(Map.Entry::getKey)
    .orElse("default-investigation");

runtime.startCase(bestPlaybook, triggerContext);
```

---

## Agent Routing with CBR

CBR experiences automatically flow to agent routing when configured. The routing pipeline uses past outcomes to prefer agents that succeeded in similar situations.

### How It Works

1. CBR retrieves similar past cases at dispatch time
2. `ExperienceAnalyser.workerSuccessRates()` computes per-agent scores from plan traces
3. Scores weight each outcome by similarity: `outcomeWeight × similarityRelevance`
4. Outcome weights: SUCCESS=+1.0, FAILURE=-1.0, DECLINED=-0.5, EXPIRED=-0.25
5. The agent with the highest weighted score is preferred

### Routing Strategy Configuration

For the composable routing architecture, CBR is one of several signal providers:

```yaml
spec:
  routingSignalWeights:
    experience: 0.4      # CBR-based agent scores
    workload: 0.3        # current agent load
    trust: 0.2           # ledger trust maturity
    personality: 0.1     # JPAF cognitive function match
```

For dedicated CBR routing (blocks layer):

```yaml
spec:
  agentRouting: "cbr"
```

### What Workers See

Retrieved experiences are available to workers via `WorkerContext`:

```java
Worker.builder()
    .name("analyst")
    .capabilityName("investigate")
    .<Map>fn().apply((input, scope) -> {
        WorkerRuntime runtime = (WorkerRuntime) scope;
        List<RetrievedExperience> experiences = runtime.context().experiences();

        // Use past outcomes to inform the current investigation
        for (RetrievedExperience exp : experiences) {
            if ("COMPLETED".equals(exp.outcome()) && exp.confidence() > 0.8) {
                // This past case succeeded with high confidence — learn from it
            }
        }
        return WorkerResult.of(Map.of("findings", results));
    })
    .build();
```

---

## Retained Case Enrichment

When a case completes, `CbrCaseRetainObserver` stores a `PlanCbrCase`. The quality of retained data directly affects future retrieval.

### Problem Description

By default, the `problem` field is the case definition name (`"phishing-investigation"`). This is useless for text-based retrieval. Enrich it:

**Option 1 — Case definition title and summary (automatic):**

```yaml
name: phishing-investigation
title: "Phishing Investigation Playbook"
summary: "Investigates email-based phishing attempts targeting corporate credentials"
spec:
  cbr:
    domain: "soc-incidents"
    features:
      severity: ".alert.severity"
```

The observer composes: `"Phishing Investigation Playbook — Investigates email-based phishing attempts targeting corporate credentials"`.

**Option 2 — JQ expression for dynamic problem text (app-specific):**

```yaml
spec:
  cbr:
    problemDescription: '(.alert.severity + " " + .alert.category + " incident targeting " + .asset.name)'
```

Evaluates to: `"HIGH phishing incident targeting mail-server-01"`. This is far more useful for hybrid (vector + text) retrieval than a bare type name.

The `problemDescription` field supports pluggable expression languages via the `{lang: expr}` map syntax:

```yaml
spec:
  cbr:
    problemDescription:
      jq: '(.alert.severity + " " + .alert.category)'
```

Fallback chain: JQ/expression result → title + summary → case type name.

### Solution Trace

The `solution` field captures the execution trace plus case definition metadata:

```
assess-risk→risk-agent(SUCCESS), review→senior-analyst(SUCCESS) [labels: soc/phishing] [types: investigation/automated]
```

Labels and types from the case definition are appended for discoverability in cross-type queries.

---

## Retrieval Timing

| Mode | When to use |
|------|-------------|
| `per-evaluation` (default) | CBR retrieves fresh results on every dispatch cycle. Use when the case context changes between dispatches and similarity should be re-evaluated. |
| `case-lifetime` | CBR retrieves once per case and caches the result. Use for stable cases where the context doesn't change significantly after the initial dispatch. Evicted on terminal state. Max 1000 cached entries. |

```yaml
spec:
  cbr:
    timing: case-lifetime    # retrieve once, cache for the case duration
```

---

## Temporal Decay

Older cases become less relevant as the environment changes (new attack patterns, updated playbooks, different agent capabilities). Temporal decay reduces the influence of old cases:

```yaml
spec:
  cbr:
    temporalDecayHalfLifeDays: 90
```

A case retained 90 days ago contributes half as much to similarity scoring as a case retained today. A case retained 180 days ago contributes one quarter. This prevents stale experiences from dominating routing decisions.

---

## Outcome Weighting

When `casehub.cbr.outcome-weighting.enabled=true`, retrieval scores are re-weighted by the case's confidence value. Cases with higher outcome confidence (closer to 1.0) rank higher. Cases with null confidence are treated as confidence 1.0.

This means a case that completed successfully with high confidence outranks a case with slightly better feature similarity but lower confidence.

---

## Learned Action Costs

When GOAP planning is active, CBR experiences feed back into action cost computation. Actions with low historical success rates become more expensive in the planner's cost model:

```
cost_factor = 1.0 / max(successRate, 1/maxCostFactor)
```

An action with 50% success rate has cost factor 2.0. An action with 10% success rate has cost factor 10.0 (capped). This steers the planner away from historically unreliable actions.

```yaml
spec:
  cbr:
    minCostSamples: 5    # need at least 5 samples before learned costs apply
```

---

## Complete Example: SOC Alert Triage

```yaml
dsl: "0.1.0"
namespace: soc
name: alert-triage
version: "1.0.0"
title: "SOC Alert Triage and Investigation"
summary: "Triages security alerts, selects the appropriate investigation playbook, and coordinates response"

spec:
  cbr:
    domain: "soc-incidents"
    crossType: true
    features:
      severity: ".alert.severity"
      category: ".alert.category"
      attack_vector: ".alert.attackVector"
      asset_criticality: ".asset.criticality"
    weights:
      attack_vector: 3.0
      severity: 2.0
      category: 1.5
      asset_criticality: 1.0
    topK: 10
    minSimilarity: 0.3
    temporalDecayHalfLifeDays: 60
    problemDescription: '(.alert.severity + " " + .alert.category + " on " + .asset.name)'

  capabilities:
    - name: triage
      inputProjection: "{severity: .alert.severity, category: .alert.category, indicators: .alert.indicators}"
      outputProjection: "{recommendation: .recommendation, confidence: .confidence}"

    - name: investigate
      inputProjection: "{alert: .alert, asset: .asset, triage: .recommendation}"
      outputProjection: "{findings: .findings, verdict: .verdict}"

  routingSignalWeights:
    experience: 0.5
    workload: 0.3
    trust: 0.2

  completion:
    success:
      allOf: [triaged, investigated]
    failure:
      anyOf: [triage-failed]

  goals:
    - name: triaged
      condition: ".recommendation != null"
    - name: investigated
      condition: ".verdict != null"
    - name: triage-failed
      condition: "._diagnostics.triage.status == \"REROUTES_EXHAUSTED\""

  bindings:
    - name: triage-alert
      capability: triage
      on: ".alert != null and .recommendation == null"
      producedKeys: [recommendation, confidence]

    - name: investigate-alert
      capability: investigate
      on: ".recommendation != null and .verdict == null"
      producedKeys: [findings, verdict]

workers:
  - name: triage-agent
    capabilities: [triage]
    agent:
      model: anthropic
      modelName: claude-sonnet-4-20250514
      systemPrompt: |
        You are a SOC triage analyst. Classify the alert severity and recommend
        an investigation playbook based on the indicators provided.

  - name: investigation-agent
    capabilities: [investigate]
    agent:
      model: anthropic
      modelName: claude-sonnet-4-20250514
      systemPrompt: |
        You are a SOC investigator. Analyze the alert and asset context to
        determine if the alert is a true positive, false positive, or requires
        escalation.
```

---

## Best Practices

1. **Start with 3-5 features** that discriminate between case outcomes. Add more only when retrieval quality is insufficient.

2. **Weight the features that matter most.** If `attack_vector` determines which playbook succeeds, give it 3x weight. Equal weights waste discriminative power.

3. **Set `minSimilarity` to 0.3** as a starting point. Below 0.3, retrieved cases are often noise. Tune based on your domain.

4. **Use `crossType: true` for triage** case definitions that need to compare across playbooks. Use same-type (default) for investigation case definitions that only need within-type history.

5. **Always set `problemDescription`** when using hybrid retrieval. The default (bare case type name) produces identical embeddings for all cases of the same type — zero text search value.

6. **Use `case-lifetime` timing** for cases where the trigger context is stable. Use `per-evaluation` (default) for cases where context evolves between dispatch cycles.

7. **Set `temporalDecayHalfLifeDays`** in fast-moving environments (SOC: 60-90 days, AML: 180-365 days). Omit for stable domains where historical cases stay relevant indefinitely.

8. **Enrich case definitions with `title`, `summary`, `labels`, and `types`** — these all flow into the retained CBR case for better discoverability.

9. **Use `minCostSamples: 5`** or higher for GOAP cost learning. With fewer samples, learned costs are unreliable and can steer the planner badly.

10. **Monitor retrieval quality** via EventLog metadata — every retrieval logs `retrievedMemoryCount` and per-step outcomes. Low match counts or consistently irrelevant results signal feature redesign.
