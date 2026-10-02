## D1: App assembly model

**Choice:** Hybrid parent POM with Quarkus uber-jar packaging
**Alternatives:**
- Custom `casehub` Maven packaging type — cleanest abstraction but complex lifecycle extension to maintain and potential conflicts with Quarkus packaging
- Parent POM + classpath only — simplest but no standalone deployable, must run via scaffold directly
**Rationale:** Standard jar packaging with a parent POM that pre-configures Quarkus uber-jar and all dependencies. Quarkus dev mode and continuous testing work unchanged. A separate `casehub-yaml-validator` Maven plugin handles schema validation at build time. Familiar Maven conventions with no custom lifecycle maintenance burden.
**Trade-offs:** The user still needs a pom.xml with `<parent>` — not fully zero-config. But this is standard Maven and well-understood.
**Sources:** scaffold/pom.xml, scaffold/scaffold-backend/pom.xml, Quarkus uber-jar documentation
**Exploration:** quick
**Status:** captured

## D2: App manifest and project structure

**Choice:** Manifest + convention-based discovery
**Alternatives:**
- Single manifest file — everything in one casehub-app.yaml. Simple but doesn't scale for non-trivial apps.
- Pure convention, no manifest — directory structure IS the app. Simplest authoring but no central config or composition control.
**Rationale:** A `casehub-app.yaml` declares app metadata, topology, and configuration. Case definitions, agent configs, and situation definitions live in their own files under conventional directories (`cases/`, `agents/`, `situations/`). Convention scanning auto-discovers resources; the manifest can explicitly list or override discovered resources. Manifest is optional for simple apps (pure convention fallback).
**Trade-offs:** Slightly more to learn (directory conventions) compared to single-file. But the progressive complexity is a feature — simple apps ignore the manifest entirely.
**Sources:** Kubernetes resource model, Quarkus extension discovery, scaffold YamlCaseDefinitionLoader classpath scanning
**Exploration:** quick
**Status:** captured

## D3: Project directory conventions

**Choice:** Dual-mode with auto-detection
**Alternatives:**
- Directory mode only — always use subdirectories. Consistent but overkill for simple demos.
- Flat mode only — everything in one directory, type by content. Simplest but doesn't scale.
**Rationale:** Flat mode (all YAML in `casehub/`) for simple apps. Directory mode (`cases/`, `agents/`, `situations/`, `playbooks/`, `topology/`, `config/`) for complex ones. Auto-detected by presence of `casehub-app.yaml` manifest or conventional subdirectories. Simple apps stay simple; complex apps get structure.
**Trade-offs:** Two modes means two code paths for resource discovery. But the detection is straightforward (subdirectory presence) and the convention is self-documenting.
**Depends on:** D2 (manifest structure determines when directory mode activates)
**Sources:** Quarkus convention directories, Spring Boot auto-configuration
**Exploration:** quick
**Status:** captured

## D4: Topology schema

**Choice:** Manifest topology section with directory mode fallback
**Alternatives:**
- Separate topology files only — cleaner separation but requires directory mode even for simple apps
- Inline in case definitions — each case declares its topology. Simple but creates duplication across cases.
**Rationale:** `casehub-app.yaml` has a `topology:` section for endpoints, streams, agents, and deployment requirements. Maps directly onto existing platform SPIs (EndpointRegistry, agent-config manifest, streams-*). In directory mode, topology can also live in `topology/*.yaml` files. The runtime reads the topology section and registers the appropriate beans.
**Trade-offs:** Topology is app-level, not per-case — cases that need specific endpoints must reference app-level topology names rather than declaring their own.
**Depends on:** D2 (manifest is the primary host for topology), D3 (directory mode provides alternative file layout)
**Sources:** platform/agent-config-core ManifestLoader, platform/endpoints-config, platform/streams-*, EndpointDescriptor
**Exploration:** quick
**Status:** captured

## D5: Java escape hatch

**Choice:** Two documented tiers — extension directory + plugin JARs
**Alternatives:**
- Extension directory only — simpler but no reuse story for cross-app libraries
- Plugin JAR dependencies only — maximum separation but heavyweight for app-specific one-offs
- Inline scripting (GraalJS/Groovy) — mixes languages, debugging nightmare, not considered
**Rationale:** Tier 1 (extension directory): optional `src/main/java/` with SPI implementations (WorkerFunctionProvider, CaseLifecycleListener, etc.). CDI discovers them. YAML references by name. Standard Maven convention. Tier 2 (plugin JARs): reusable cross-app libraries packaged as separate JARs, added as Maven dependencies. Both tiers are documented; the YAML author only encounters Java when the YAML surface doesn't cover their need.
**Trade-offs:** Two tiers means documenting two patterns. But they serve different use cases (app-specific vs reusable) and the boundary is clear.
**Sources:** scaffold YamlCaseHub augment() pattern, CDI @ApplicationScoped discovery, yaml-plugin-api @Plugin system
**Exploration:** quick
**Status:** captured

## D6: Scenario/playbook integration in app schema

**Choice:** First-class directories with auto-discovery
**Rationale:** Scenarios (YAML-defined scripted demo/test harnesses, currently in Pages) and playbooks (YAML step files loaded by StepFileCallableDispatcher) both become first-class in the YAML app schema. Convention directories: `scenarios/` and `playbooks/`. The scaffold runtime auto-discovers both at startup — scenarios are registered with the scenario engine (Pages), playbooks are pre-registered for StepFileCallableDispatcher. The app manifest can reference them explicitly or rely on convention scanning.
**Trade-offs:** Pulling scenarios into the casehub app structure creates a dependency on Pages scenario-runtime. This must be an optional dependency that degrades gracefully when Pages is not on the classpath.
**Depends on:** D2 (manifest references), D3 (directory conventions)
**Sources:** helpdesk scenario YAML, StepFileCallableDispatcher (engine commit 267d46246), scaffold demo/ directory
**Exploration:** quick
**Status:** captured

## D7: Progressive demo sequence

**Choice:** Three progressive demos
**Rationale:**
- **Demo 1 — "Hello Case":** Single YAML file, flat mode. One capability, one `do`-block worker (set-result), one binding, one goal. No manifest, no subdirectories. Demonstrates: YAML case definition, auto-discovery, goal completion.
- **Demo 2 — "Agent Helpdesk":** Directory mode with multiple files. Case definition with agent and MCP workers, agent config, scenario for demoing. Demonstrates: multi-worker coordination, agent integration, scenario-driven demo, topology (agent backend config).
- **Demo 3 — "Coordinated Operations":** Full app manifest with multiple cases, playbooks, situations, topology (endpoints + streams + agents), deployment requirements. Demonstrates: cross-case coordination, playbook dispatch, event-driven streams, complete YAML surface.
**Trade-offs:** Three demos is a significant amount of example content to create and maintain. But the progressive complexity is essential for onboarding — jumping straight to Demo 3 would be overwhelming.
**Depends on:** D1 (packaging), D2 (manifest), D3 (directory conventions), D4 (topology), D6 (scenarios/playbooks)
**Sources:** scaffold demo/ (customer-onboarding.yaml, incident-response.yaml), helpdesk example, examples/getting-started (does not exist yet)
**Exploration:** quick
**Status:** captured

## D8: Agent-config composition with app manifest

**Choice:** App manifest topology.agents generates agent-config format
**Rationale:** The app manifest's `topology.agents:` section uses the existing agent-config.yaml schema (the format ManifestLoader already parses). The scaffold runtime generates or merges this into the location ManifestLoader expects. A raw `agent-config.yaml` in the app's config directory takes precedence over the manifest's topology.agents section. No new format — just a new source location.
**Trade-offs:** The manifest topology.agents section is a subset/wrapper of agent-config.yaml. App authors who already know agent-config.yaml can use it directly. The manifest provides a gentler on-ramp.
**Depends on:** D4 (topology section hosts agents)
**Sources:** platform/agent-config-core ManifestLoader, AgentConfigBeans @Startup
**Exploration:** quick (surfaced by manual decision review)
**Status:** captured

## D9: Deployment profiles

**Choice:** Quarkus profile-based dev/prod separation
**Rationale:** The parent POM and scaffold provide default `dev` profile configuration: H2, in-memory stores, local/mock agents. The app author provides `prod` profile overrides: PostgreSQL, JPA stores, real agent backends, real streams. The same YAML app definition works in both modes — the difference is `application.properties` profile sections. Quarkus already has profile support (`%dev.*`, `%prod.*`), so no new mechanism needed. Examples ship with both profiles working out of the box.
**Trade-offs:** Default dev config must be comprehensive enough that examples work with zero additional configuration. This means the parent POM needs sensible defaults for all platform subsystems.
**Sources:** Quarkus profile documentation, scaffold application.properties
**Exploration:** quick (surfaced by manual decision review and user feedback)
**Status:** captured

## D10: Scenario optional module

**Choice:** Optional Pages dependency with classpath detection
**Rationale:** Scenario support (Pages scenario-runtime) is an optional module in the parent POM. When present on the classpath, the scaffold runtime auto-discovers scenario YAML files from `scenarios/` and registers them with the scenario engine. When absent, the directory is silently ignored. This is standard Quarkus optional dependency handling — CDI beans conditional on class presence (`@IfBuildProfile` or `Arc.container()` class check). Playbooks (engine-native StepFileCallableDispatcher) work without Pages and are always available.
**Trade-offs:** Scenarios are not available in the most minimal app configuration unless Pages is explicitly added. But for Demo 1 (Hello Case), scenarios are not needed.
**Depends on:** D6 (scenario directory convention), D9 (scenarios may only work in dev profile for demo apps)
**Sources:** Pages scenario-runtime, Quarkus optional dependency handling, CDI @IfBuildProfile
**Exploration:** quick (surfaced by manual decision review)
**Status:** captured
