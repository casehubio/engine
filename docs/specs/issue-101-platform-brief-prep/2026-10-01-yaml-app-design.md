# Design: 100% YAML CaseHub Applications

**Issue:** casehubio/casehub-ops#101
**Date:** 2026-10-01
**Status:** Final
**Tracked under:** casehubio/casehub-engine#1017 (master epic: zero-authored-Java deployment)

---

## 1. Problem Statement

Building a CaseHub application today requires replicating the scaffold project: a Quarkus application with 30+ Maven dependencies, `quarkus.index-dependency` entries, health checks, GraphQL resolvers, exception mappers, and a Java `CaseHub` subclass that wires YAML case definitions to workers. The YAML DSL already covers case definitions comprehensively — capabilities, workers (do/agent/mcp/a2a/react), bindings, goals, milestones, planning — but the Java infrastructure around it is mandatory and non-trivial.

The goal: a YAML author drops case definitions, agent configs, scenarios, playbooks, and a minimal `pom.xml` into a project. `mvn package` produces a deployable. No authored Java required.

Java is freely used under the hood — the entire CaseHub platform is Java. The YAML surface wraps and exposes platform capabilities without requiring the app author to write Java. When YAML doesn't cover a use case, an explicit Java escape hatch is available. The YAML authoring surface interacts with CaseHub primarily via GraphQL, scenarios, and playbooks.

---

## 2. Architecture Overview

### Assembly Model

A YAML app is a standard Maven JAR project. The `casehub-app-parent` POM (published from the scaffold repo) pre-configures:

- All engine, platform, and runtime dependencies
- Quarkus uber-jar packaging
- `quarkus.index-dependency` entries for all casehub modules
- Default `application.properties` for dev profile (H2, in-memory stores)
- `casehub-yaml-validator` Maven plugin for build-time schema validation

The user's project contains:

```
pom.xml                              # <parent> = casehub-app-parent
src/main/resources/
  casehub/                           # YAML resources (flat or directory mode)
    my-case.yaml                     # case definition
  application.properties             # app-specific config overrides (optional)
```

No Java sources required. `mvn quarkus:dev` runs the app in dev mode. `mvn package` produces a Quarkus uber-jar.

### Minimal pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>io.casehub</groupId>
    <artifactId>casehub-app-parent</artifactId>
    <version>0.2-SNAPSHOT</version>
  </parent>
  <groupId>com.example</groupId>
  <artifactId>my-casehub-app</artifactId>
  <version>1.0-SNAPSHOT</version>
</project>
```

---

## 3. App Schema — `casehub-app.yaml`

The app manifest is optional. When present, it declares app metadata, topology, and configuration. When absent, the scaffold runtime uses pure convention scanning.

```yaml
# casehub-app.yaml — app-level manifest
apiVersion: casehub/v1
kind: Application

metadata:
  name: customer-ops
  version: "1.0.0"
  description: Customer operations application

# Cases can be listed explicitly or discovered by convention
cases:
  - customer-onboarding.yaml       # relative path within casehub/cases/
  - incident-response.yaml

# Agent backend configuration (agent-config.yaml format)
agents:
  backends:
    claude:
      defaultModel: claude-sonnet-5-5
    ollama:
      host: http://localhost:11434
      defaultModel: llama3
  aliases:
    fast: { tier: FAST }
    smart: { tier: FLAGSHIP }
  defaultBackend: claude

# Scenario definitions for demo/testing
scenarios:
  - customer-onboarding-demo.yaml

# Playbook step files
playbooks:
  - compliance-check.yaml
  - escalation-protocol.yaml

# Topology — runtime wiring and deployment requirements
topology:
  endpoints:
    - name: payment-gateway
      path: /services/payment
      type: SERVICE
      protocol: HTTP
      properties:
        url: ${PAYMENT_API_URL:http://localhost:9090}
        credentialRef: payment-api-key

  streams:
    - name: order-events
      type: KAFKA
      properties:
        topic: orders
        eventType: io.casehub.order.created

  deployment:
    replicas: 1
    resources:
      memory: 512Mi
      cpu: "0.5"
    healthCheck:
      path: /q/health
      interval: 30s

# App-level configuration defaults
config:
  expressionLang: jq
  persistence:
    strategy: drop-and-create      # dev default, override in prod
```

### Schema Fields

| Field | Required | Description |
|-------|----------|-------------|
| `apiVersion` | yes | Schema version (`casehub/v1`) |
| `kind` | yes | Always `Application` |
| `metadata` | yes | App name, version, description |
| `cases` | no | Explicit case file list. If absent, convention-scans `cases/` or root |
| `agents` | no | Agent backend config (agent-config.yaml format subset) |
| `scenarios` | no | Scenario file list. If absent, convention-scans `scenarios/` |
| `playbooks` | no | Playbook file list. If absent, convention-scans `playbooks/` |
| `topology` | no | Endpoints, streams, deployment requirements |
| `config` | no | App-level configuration defaults |

---

## 4. Project Structure

### Flat Mode

For simple apps — all YAML files in `src/main/resources/casehub/`:

```
src/main/resources/
  casehub/
    my-case.yaml                   # case definition (auto-discovered)
    application.properties         # optional app config
```

No manifest needed. The `YamlCaseDefinitionLoader` scans `casehub/` and registers all `.yaml`/`.yml` files as case definitions.

**Detection rule:** Flat mode is active when:
1. No `casehub-app.yaml` exists in `casehub/`
2. No conventional subdirectories (`cases/`, `agents/`, `scenarios/`, `playbooks/`, `topology/`) exist in `casehub/`

### Directory Mode

For complex apps — conventional subdirectories under `src/main/resources/casehub/`:

```
src/main/resources/
  casehub/
    casehub-app.yaml               # app manifest
    cases/
      customer-onboarding.yaml
      incident-response.yaml
    agents/
      agent-config.yaml            # or declared in manifest topology.agents
    scenarios/
      onboarding-demo.yaml
      onboarding-live.yaml
    playbooks/
      compliance-check.yaml
      escalation-protocol.yaml
    topology/
      endpoints.yaml               # alternative to manifest topology section
      streams.yaml
    config/
      application-prod.properties  # profile-specific config
```

**Detection rule:** Directory mode is active when:
1. `casehub-app.yaml` exists in `casehub/`, OR
2. Any conventional subdirectory exists in `casehub/`

### Precedence Rules

1. Explicit manifest file lists override convention scanning for that resource type
2. A raw `agent-config.yaml` in `agents/` or `config/` takes precedence over manifest `agents:` section
3. Profile-specific properties (`application-{profile}.properties`) in `config/` supplement `application.properties`

---

## 5. Maven Packaging — `casehub-app-parent`

### Location

Published from the scaffold repo. Scaffold already provides the runtime application and `YamlCaseDefinitionLoader` — the parent POM is a natural extension.

### What the Parent POM Configures

**Dependencies (managed, always included):**

| Dependency | Purpose |
|------------|---------|
| `casehub-engine` | Core engine runtime |
| `casehub-engine-api` | Engine API types |
| `casehub-engine-schema` | YAML schema and records |
| `casehub-engine-planning` | Planning module |
| `casehub-platform` | Platform implementations |
| `casehub-platform-api` | Platform SPIs |
| `casehub-platform-expression` | JQ/MVEL expression engines |
| `casehub-platform-agent-*` | Agent backends (claude, openai, ollama, etc.) |
| `casehub-platform-agent-config` | Agent configuration |
| `casehub-platform-agent-router` | Model routing |
| `casehub-platform-agent-gate` | Rate limiting |
| `casehub-platform-memory-inmem` | In-memory case memory (dev default) |
| `casehub-platform-notifications` | Notification pipeline |
| `casehub-platform-mcp` | MCP tool infrastructure |
| `casehub-platform-simulation-starter` | Simulation support |
| `scaffold-runtime` | Bootstrap, YAML loading, health, GraphQL |
| `quarkus-hibernate-orm-panache` | Persistence |
| `quarkus-jdbc-h2` | Dev database (test scope) |
| `quarkus-jdbc-postgresql` | Production database |

**Optional dependencies (activated by classpath or profile):**

| Dependency | Activation | Purpose |
|------------|-----------|---------|
| `casehub-pages-scenario-runtime` | Present on classpath | Scenario engine |
| `casehub-engine-ledger` | Present on classpath | Audit ledger |
| `casehub-platform-persistence-jpa` | `prod` profile | JPA preference store |

**Plugin configuration:**

| Plugin | Configuration |
|--------|---------------|
| `quarkus-maven-plugin` | uber-jar packaging, native-image ready |
| `casehub-yaml-validator` | Validates `casehub/` YAML against CaseDefinition schema at build time |
| `maven-compiler-plugin` | Java 21+, `-parameters` for CDI |

**Index dependencies (in default `application.properties` packaged with scaffold-runtime):**

The parent POM must ensure all casehub modules are indexed for CDI bean discovery. Without these, CDI silently fails to find beans. The scaffold currently requires 9+ index-dependency entries:

```properties
quarkus.index-dependency.engine-common.group-id=io.casehub
quarkus.index-dependency.engine-common.artifact-id=casehub-engine-common
quarkus.index-dependency.engine-rest.group-id=io.casehub
quarkus.index-dependency.engine-rest.artifact-id=casehub-engine-rest
quarkus.index-dependency.planning.group-id=io.casehub
quarkus.index-dependency.planning.artifact-id=casehub-engine-planning
# ... (full list from scaffold-backend/application.properties)
```

These entries are packaged in scaffold-runtime's `application.properties` so YAML app authors never see them. The parent POM does not need to repeat them — Quarkus merges classpath `application.properties` files.

**Properties:**

| Property | Value | Purpose |
|----------|-------|---------|
| `version.quarkus.platform` | `3.32.2` | Quarkus version |
| `version.io.casehub` | `0.2-SNAPSHOT` | CaseHub platform version |

### `scaffold-runtime` Module

A new module extracted from `scaffold-backend` containing only the reusable runtime infrastructure:

- `YamlCaseDefinitionLoader` — classpath scanning and registration
- `YamlAppManifestLoader` — new: parses `casehub-app.yaml`, registers topology, agents, scenarios
- `CaseHubClassPathLoader` — Java CaseHub subclass discovery (for escape hatch)
- Health checks (liveness + readiness)
- ACL request filter, exception mapper
- GraphQL platform info resolver
- Module registry endpoint

The existing `scaffold-backend` becomes a thin shell that depends on `scaffold-runtime` — it's the deployable app for scaffold itself. The `casehub-app-parent` depends on `scaffold-runtime`, not `scaffold-backend`.

---

## 6. Runtime Bootstrap

### Startup Sequence

When a YAML app starts, the scaffold runtime executes this sequence:

1. **Scan `casehub/` classpath directory** — detect flat vs directory mode
2. **Load manifest** (if present) — parse `casehub-app.yaml`, validate schema
3. **Register topology** — process `topology.endpoints` → `EndpointRegistry`, `topology.agents` → agent-config manifest, `topology.streams` → stream channel registration
4. **Discover case definitions** — scan `cases/` (directory mode) or `casehub/` root (flat mode), parse YAML, register with `CaseDefinitionRegistry`
5. **Discover playbooks** — scan `playbooks/` if present, pre-register for `StepFileCallableDispatcher`
6. **Discover scenarios** (if Pages on classpath) — scan `scenarios/` if present, register with scenario engine
7. **Discover Java extensions** (if present) — CDI discovers `WorkerFunctionProvider`, `CaseLifecycleListener`, and other SPI implementations from `src/main/java/`

### Discovery Rules

- All `.yaml` and `.yml` files in scanned directories are candidates
- Files are identified by content: `dsl:` field → case definition, `apiVersion: casehub/v1` → manifest, `steps:` field → playbook
- Malformed YAML fails fast at startup with a clear error message identifying the file and parse error
- Duplicate case names (same namespace + name + version) fail fast

---

## 7. Topology

### Endpoints

Maps to `EndpointRegistry.register()`:

```yaml
topology:
  endpoints:
    - name: payment-gateway
      path: /services/payment
      type: SERVICE           # SYSTEM | SERVICE | WORKER | AGENT
      protocol: HTTP          # HTTP | GRPC | KAFKA | MCP | CAMEL | QHORUS | AMQP
      properties:
        url: ${PAYMENT_API_URL:http://localhost:9090}
        credentialRef: payment-api-key
      capabilities: [SEND, QUERY]  # SEND | RECEIVE | QUERY | DISPATCH
```

Variable substitution uses the existing `VariableResolver` chain — environment variables, system properties, config properties.

### Agent Configuration

The `agents:` section in the manifest follows the existing `agent-config.yaml` schema from `agent-config-core/ManifestLoader`:

```yaml
agents:
  backends:
    claude:
      defaultModel: claude-sonnet-5-5
      maxConcurrentSessions: 5
    ollama:
      host: http://localhost:11434
      defaultModel: llama3
  aliases:
    fast: { tier: FAST }
    smart: { tier: FLAGSHIP }
    coding: { tier: FLAGSHIP, requiredCapabilities: [CODE, REASONING] }
  defaultBackend: claude
```

**Composition with raw agent-config.yaml:** If `agents/agent-config.yaml` or `config/agent-config.yaml` exists, it fully replaces (not merges with) the manifest's `agents:` section. The `ManifestLoader` discovers it via its existing directory hierarchy search. This is full replacement, not deep merge — the raw file is the single source of truth for agent configuration when present.

### Streams

Maps to stream channel registration:

```yaml
topology:
  streams:
    - name: order-events
      type: KAFKA              # KAFKA | AMQP | WEBHOOK | POLL | CAMEL
      properties:
        topic: orders
        eventType: io.casehub.order.created
        dataContentType: application/json
```

### Deployment Requirements

Declarative resource requirements for deployment tools:

```yaml
topology:
  deployment:
    replicas: 1
    resources:
      memory: 512Mi
      cpu: "0.5"
    healthCheck:
      path: /q/health
      interval: 30s
    env:
      JAVA_OPTS: "-Xmx384m"
```

This section is informational for deployment tooling — the scaffold runtime does not enforce resource limits or replica counts. However, the runtime does read `healthCheck.path` and `healthCheck.interval` to configure Quarkus health check endpoints. Future integration with Kubernetes manifests, Docker Compose, or cloud deployment scripts can consume the full section declaratively.

---

## 8. Scenarios and Playbooks

### Playbooks

Playbooks are YAML step files loaded by `StepFileCallableDispatcher` (registered as `casehub:step-file`). They are engine-native and always available.

```yaml
# playbooks/compliance-check.yaml
steps:
  - name: verify-identity
    plugin: identity-check
    inputs:
      documentType: ${context.documentType}
  - name: risk-assessment
    plugin: risk-score
    inputs:
      applicantData: ${context.applicantData}
    when: ${steps.verify-identity.result.verified}
```

**Discovery:** Files in `playbooks/` are pre-registered at startup. Case definitions reference them via `do: { stepFile: compliance-check }` in worker blocks.

### Scenarios

Scenarios are YAML-defined scripted demo/test harnesses from the Pages scenario engine.

```yaml
# scenarios/onboarding-demo.yaml
name: Customer Onboarding Demo
sections:
  - label: "Create Application"
    steps:
      - action: startCase
        target: customer-onboarding
        data:
          applicantName: "Jane Doe"
          documentType: "passport"
      - await:
          match: { status: "ACTIVE" }
          timeout: 5s
```

**Dependency:** Scenarios require `casehub-pages-scenario-runtime` on the classpath. The parent POM includes it as an optional dependency. When absent, scenario files in `scenarios/` are silently ignored at startup (with a `DEBUG`-level log message).

**Integration:** The scenario engine is a separate CDI bean. A `@IfBuildProfile("demo")` guard can restrict scenario REST endpoints to demo/dev profiles.

---

## 9. Java Extension Model

### Tier 1: Extension Directory

For app-specific custom logic. Add Java sources to the standard `src/main/java/`:

```
src/main/java/
  com/example/
    MyCustomWorker.java            # implements WorkerFunctionProvider
    MyLifecycleListener.java       # implements CaseLifecycleListener
```

CDI discovers these automatically. The YAML case definition references them by name:

```yaml
workers:
  - name: custom-processor
    capabilities: [process]
    # CDI discovers the WorkerFunctionProvider by capability name
```

**No special configuration needed** — the parent POM already configures Maven compiler and Quarkus augmentation for Java sources.

### Tier 2: Plugin JARs

For reusable cross-app libraries. Package custom workers/SPIs as a separate Maven artifact:

```xml
<!-- in the app's pom.xml -->
<dependency>
  <groupId>com.example</groupId>
  <artifactId>my-casehub-plugins</artifactId>
  <version>1.0.0</version>
</dependency>
```

The plugin JAR's CDI beans are discovered like any Quarkus dependency. The parent POM's `quarkus.index-dependency` configuration handles bean discovery.

### Extension Points Available

| SPI | Purpose | Package |
|-----|---------|---------|
| `WorkerFunctionProvider` | Custom worker execution logic | `io.casehub.engine.worker` |
| `CaseLifecycleListener` | Case lifecycle callbacks | `io.casehub.engine.api` |
| `GoalFormationStrategy` | Custom goal formation | `io.casehub.engine.api` |
| `GoalRevisionStrategy` | Custom goal revision | `io.casehub.engine.api` |
| `CandidateMatchStrategy` | Custom worker routing | `io.casehub.engine.api` |
| `DecompositionStrategy` | Custom planning decomposition | `io.casehub.engine.api` |

---

## 10. Deployment Profiles

### Dev Profile (Default)

The parent POM and scaffold-runtime provide default dev configuration:

```properties
# Provided by casehub-app-parent (default profile)
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:casehub;DB_CLOSE_DELAY=-1
quarkus.hibernate-orm.schema-management.strategy=drop-and-create
casehub.platform.agent.default-backend=ollama
casehub.memory.provider=inmem
```

**What works out of the box in dev:**
- Case definitions loaded from YAML
- In-memory persistence (H2)
- Local agent backends (Ollama, if running)
- In-memory case memory, notifications, ACL
- Scenario endpoints (if Pages on classpath)
- Quarkus dev mode with live reload

### Prod Profile

The app author provides production overrides in `application.properties`:

```properties
# In the app's application.properties (prod profile)
%prod.quarkus.datasource.db-kind=postgresql
%prod.quarkus.datasource.jdbc.url=${DATABASE_URL}
%prod.quarkus.datasource.username=${DATABASE_USER}
%prod.quarkus.datasource.password=${DATABASE_PASSWORD}
%prod.quarkus.hibernate-orm.schema-management.strategy=update
%prod.casehub.platform.agent.default-backend=claude
```

### Test Profile

For automated testing:

```properties
%test.quarkus.datasource.db-kind=h2
%test.quarkus.datasource.jdbc.url=jdbc:h2:mem:casehub-test
%test.quarkus.hibernate-orm.schema-management.strategy=drop-and-create
```

---

## 11. Cross-Repo Gap Analysis

### What Exists Today

| Capability | Repo | Status |
|-----------|------|--------|
| YAML case definition schema | engine (CaseDefinition.yaml) | Complete |
| YAML record codegen | engine (CasehubRecordCodegen) | Complete |
| YAML case definition loader | scaffold (YamlCaseDefinitionLoader) | Complete |
| Worker backends (do, agent, mcp, a2a, react) | engine | Complete |
| Agent configuration (agent-config.yaml) | platform (agent-config-core) | Complete |
| Endpoint registry + YAML populator | platform (endpoints-config) | Complete |
| Stream channels (Kafka, AMQP, webhook, poll, Camel) | platform (streams-*) | Complete |
| Orchestration primitives (semaphore, latch, signal, channel, FSM) | platform (yaml-core) | Complete |
| Module system (imports, parameters, forEach, variables) | platform (yaml-core) | Complete |
| Plugin system (@Plugin APT) | platform (yaml-plugin-api/processor) | Complete |
| Scenario engine | pages (scenario-runtime) | Complete |
| Playbook dispatch (StepFileCallableDispatcher) | engine | Complete |
| Expression engines (JQ, MVEL) | platform (expression) | Complete |
| Simulation framework | platform (simulation-*) | Complete |

### What Must Be Built

| Gap | Repo | Effort | Blocks |
|-----|------|--------|--------|
| `casehub-app-parent` POM | scaffold | S | Demo 1 |
| `scaffold-runtime` module extraction | scaffold | M | Demo 1 |
| `YamlAppManifestLoader` | scaffold (scaffold-runtime) | M | Demo 2 |
| App manifest JSON Schema | scaffold | S | Demo 2 |
| `casehub-yaml-validator` Maven plugin | scaffold or engine | S | Demo 1 (nice-to-have) |
| Playbook auto-discovery from `playbooks/` | engine | S | Demo 3 |
| Scenario auto-discovery from `scenarios/` | scaffold | S | Demo 2 |
| Default dev-profile `application.properties` | scaffold (parent POM) | S | Demo 1 |
| Getting-started examples | examples | M | All demos |
| Convention directory scanner (flat vs directory mode) | scaffold (scaffold-runtime) | S | Demo 2 |

### Per-Repo Changes

**scaffold:**
- Extract `scaffold-runtime` module from `scaffold-backend`
- Create `casehub-app-parent` POM
- Implement `YamlAppManifestLoader` in scaffold-runtime
- Convention directory scanner
- Scenario auto-discovery (optional Pages dependency)
- Default dev-profile configuration

**engine:**
- Playbook auto-discovery from `playbooks/` directory (extend `StepFileCallableDispatcher`)
- App manifest JSON Schema (extends CaseDefinition.yaml)

**examples:**
- Create `getting-started/` directory
- Demo 1: hello-case
- Demo 2: agent-helpdesk (connect to existing helpdesk scenario work)
- Demo 3: coordinated-ops

**platform:**
- No changes required — existing infrastructure (agent-config, endpoints-config, streams, expression, simulation) is consumed as-is

---

## 12. Critical Path

### Phase 1 → Demo 1 ("Hello Case")

**Prerequisite:** None — minimal viable path.

1. Extract `scaffold-runtime` from `scaffold-backend` (scaffold repo)
2. Create `casehub-app-parent` POM with managed dependencies (scaffold repo)
3. Configure default dev-profile `application.properties` (parent POM resources)
4. Create `getting-started/hello-case/` example (examples repo)
5. Verify: `mvn quarkus:dev` runs the hello-case with zero Java sources

**Demo 1 shows:** A single YAML case definition, auto-discovered, running in dev mode. Case has one capability, one `do`-block worker, one binding, one goal. GraphQL endpoint accepts mutations to start a case and query its state.

### Phase 2 → Demo 2 ("Agent Helpdesk")

**Prerequisite:** Phase 1 complete.

1. Implement `YamlAppManifestLoader` — parse `casehub-app.yaml` (scaffold-runtime)
2. Convention directory scanner — flat vs directory mode detection (scaffold-runtime)
3. Implement manifest `agents:` → agent-config integration (scaffold-runtime)
4. Scenario auto-discovery from `scenarios/` (scaffold-runtime, optional Pages dep)
5. Create `getting-started/agent-helpdesk/` example (examples repo)
6. Connect to existing helpdesk scenario/slides work

**Demo 2 shows:** Directory-mode project with multiple YAML files. Agent workers calling Claude/Ollama. MCP tool integration. Scenario-driven demo walkthrough. All via YAML — no Java.

### Phase 3 → Demo 3 ("Coordinated Operations")

**Prerequisite:** Phase 2 complete.

1. Playbook auto-discovery from `playbooks/` (engine)
2. Manifest `topology.endpoints` → EndpointRegistry (scaffold-runtime)
3. Manifest `topology.streams` → stream channel registration (scaffold-runtime)
4. Create `getting-started/coordinated-ops/` example (examples repo)
5. Validate full YAML surface coverage

**Demo 3 shows:** Full app manifest. Multiple cases coordinating via signals and channels. Playbook dispatch from case bindings. Event streams (Kafka or webhook). External endpoint integration. Topology declaration with deployment requirements. Prod-profile configuration for real deployment.

### Dependency Graph

```
Phase 1: scaffold-runtime extraction → parent POM → hello-case example
              ↓
Phase 2: manifest loader → directory scanner → agent integration → scenario discovery → agent-helpdesk example
              ↓
Phase 3: playbook discovery → endpoint registration → stream registration → coordinated-ops example
```

---

## 13. Progressive Demo Definitions

### Demo 1 — "Hello Case"

**Project structure:**
```
hello-case/
  pom.xml
  src/main/resources/
    casehub/
      hello-case.yaml
```

**pom.xml:**
```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>io.casehub</groupId>
    <artifactId>casehub-app-parent</artifactId>
    <version>0.2-SNAPSHOT</version>
  </parent>
  <artifactId>hello-case</artifactId>
  <version>1.0-SNAPSHOT</version>
</project>
```

**hello-case.yaml:**
```yaml
dsl: "0.1"
namespace: getting-started
name: hello-case
version: "1.0.0"
title: Hello Case

spec:
  capabilities:
    - name: greet
      inputSchema: "{ name: .input.name }"
      outputSchema: "{ greeting: .greeting }"

  workers:
    - name: greeter
      capabilities: [greet]
      do:
        - setResult:
            set:
              greeting: "Hello from CaseHub!"

  bindings:
    - name: auto-greet
      capability: greet
      on:
        contextChange:
          filter: ".input.name != null and .greeting == null"

  goals:
    - name: greeted
      condition: ".greeting != null"

  completion:
    completed:
      allOf: [greeted]
```

**How to run:**
```bash
mvn quarkus:dev
# In another terminal:
curl -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query": "mutation { startCase(namespace: \"getting-started\", name: \"hello-case\", context: {input: {name: \"World\"}}) { id status } }"}'
```

### Demo 2 — "Agent Helpdesk"

**Project structure:**
```
agent-helpdesk/
  pom.xml
  src/main/resources/
    casehub/
      casehub-app.yaml
      cases/
        helpdesk-ticket.yaml
      agents/
        agent-config.yaml
      scenarios/
        helpdesk-demo.yaml
```

**casehub-app.yaml:**
```yaml
apiVersion: casehub/v1
kind: Application

metadata:
  name: agent-helpdesk
  version: "1.0.0"
  description: AI-powered helpdesk with agent workers

agents:
  backends:
    ollama:
      host: http://localhost:11434
      defaultModel: llama3
  defaultBackend: ollama
```

**helpdesk-ticket.yaml:**
```yaml
dsl: "0.1"
namespace: helpdesk
name: ticket
version: "1.0.0"
title: AI-Assisted Helpdesk Ticket

spec:
  capabilities:
    - name: classify
      inputSchema: "{ description: .ticket.description }"
      outputSchema: "{ category: .classification.category, priority: .classification.priority }"
    - name: resolve
      inputSchema: "{ ticket: .ticket, classification: .classification }"
      outputSchema: "{ resolution: .resolution }"

  workers:
    - name: classifier
      capabilities: [classify]
      agent:
        model: fast
        prompt: |
          Classify this support ticket into one of: billing, technical, general.
          Ticket: ${context.ticket.description}
          Respond with JSON: {"category": "...", "priority": "low|medium|high"}

    - name: resolver
      capabilities: [resolve]
      agent:
        model: smart
        prompt: |
          You are a helpful support agent. Resolve this ticket.
          Category: ${context.classification.category}
          Priority: ${context.classification.priority}
          Description: ${context.ticket.description}
          Provide a clear resolution message.

  bindings:
    - name: auto-classify
      capability: classify
      on:
        contextChange:
          filter: ".ticket.description != null and .classification == null"

    - name: auto-resolve
      capability: resolve
      on:
        contextChange:
          filter: ".classification != null and .resolution == null"

  goals:
    - name: classified
      condition: ".classification != null"
    - name: resolved
      condition: ".resolution != null"

  completion:
    completed:
      allOf: [classified, resolved]
```

### Demo 3 — "Coordinated Operations"

**Project structure:**
```
coordinated-ops/
  pom.xml
  src/main/resources/
    casehub/
      casehub-app.yaml
      cases/
        order-processing.yaml
        fraud-detection.yaml
      playbooks/
        fraud-escalation.yaml
      scenarios/
        full-order-cycle.yaml
      config/
        application-prod.properties
```

**casehub-app.yaml:**
```yaml
apiVersion: casehub/v1
kind: Application

metadata:
  name: coordinated-ops
  version: "1.0.0"
  description: Multi-case coordination with playbooks and streams

agents:
  backends:
    claude:
      defaultModel: claude-sonnet-5-5
  defaultBackend: claude

topology:
  endpoints:
    - name: payment-api
      path: /services/payment
      type: SERVICE
      protocol: HTTP
      properties:
        url: ${PAYMENT_API_URL:http://localhost:9090}

  streams:
    - name: order-events
      type: WEBHOOK
      properties:
        eventType: io.casehub.order.created

  deployment:
    replicas: 2
    resources:
      memory: 1Gi
      cpu: "1.0"
```

Full case definitions for Demo 3 would include:
- Cross-case signals (order-processing signals fraud-detection)
- Playbook dispatch from fraud-detection bindings
- Stream-triggered case creation
- Channel-based data flow between workers
- Planning with GOAP actions

---

## References

- `scaffold/scaffold-backend/src/main/java/.../YamlCaseDefinitionLoader.java` — existing YAML discovery
- `scaffold/demo/customer-onboarding.yaml` — existing demo case definition
- `engine/schema/src/main/resources/schema/CaseDefinition.yaml` — case definition schema
- `engine/api/src/main/java/.../StepFileCallableDispatcher.java` — playbook dispatch
- `platform/agent-config-core/` — ManifestLoader, agent configuration
- `platform/endpoints-config/` — YAML endpoint populator
- `platform/yaml-core/` — module system, orchestration primitives
- `platform/yaml-plugin-api/` — plugin system
- `examples/helpdesk/` — existing helpdesk example with Java
- `examples/helpdesk/src/main/resources/cases/helpdesk-ticket.yaml` — existing case YAML
- `pages/backend/scenario*/` — scenario engine
- casehubio/casehub-ops#101 — tracking issue
- casehubio/casehub-engine#1017 — master epic
