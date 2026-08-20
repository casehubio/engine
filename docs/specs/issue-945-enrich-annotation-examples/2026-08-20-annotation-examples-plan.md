# Annotation Examples Enrichment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use
> subagent-driven-development (recommended) or executing-plans to
> implement this plan task-by-task. Each task follows TDD
> (test-driven-development) and uses ide-tooling for structural
> editing. Steps use checkbox (`- [ ]`) syntax for tracking.

**Focal issue:** #945 — Enrich annotation examples — coverage gaps, eidos integration, domain diversity
**Issue group:** #945

**Goal:** Add 5 new annotation example modules and enrich 2 existing ones to demonstrate cross-repo annotation composition (engine + eidos + work + ledger) across compelling domains, covering all 21 engine annotation capabilities.

**Architecture:** Each example is a single `@Case` interface with inner record types, a `@QuarkusTest` verifying the generated `CaseDefinition`, and a Maven module. New examples add cross-repo annotation dependencies. Examples are designed as layered bases for future blocks enhancement.

**Tech Stack:** Java 21, Quarkus 3.32.2, engine-annotations, eidos-annotations, work-annotations, ledger-annotations

## Global Constraints

- All examples set `<maven.deploy.skip>true</maven.deploy.skip>`
- All examples include `jandex-maven-plugin` for build-time annotation scanning
- Tests use `QuarkusUnitTest` with `withApplicationRoot` — same pattern as existing examples
- No LLM runtime required — `@SystemPrompt` tests verify function type generation, not execution
- Cross-repo annotations use `${version.io.casehub}` (eidos, work) or `${version.io.casehub.ledger}` (ledger)

---

## Batch 1: Foundation — dependency management and module registration

### Task 1: Add cross-repo annotation dependencies to root pom

**Files:**
- Modify: `pom.xml` (root)

**Interfaces:**
- Consumes: nothing
- Produces: `<dependencyManagement>` entries for eidos-annotations, work-annotations, ledger-annotations (runtime + deployment for each)

- [ ] **Step 1: Add eidos, work, ledger annotation dependency management entries**

In root `pom.xml`, add after the existing `casehub-eidos-api` entry (around line 334):

```xml
<dependency>
    <groupId>io.casehub</groupId>
    <artifactId>casehub-eidos-annotations</artifactId>
    <version>${version.io.casehub}</version>
</dependency>
<dependency>
    <groupId>io.casehub</groupId>
    <artifactId>casehub-eidos-annotations-deployment</artifactId>
    <version>${version.io.casehub}</version>
</dependency>
<dependency>
    <groupId>io.casehub</groupId>
    <artifactId>casehub-work-annotations</artifactId>
    <version>${version.io.casehub}</version>
</dependency>
<dependency>
    <groupId>io.casehub</groupId>
    <artifactId>casehub-work-annotations-deployment</artifactId>
    <version>${version.io.casehub}</version>
</dependency>
<dependency>
    <groupId>io.casehub</groupId>
    <artifactId>casehub-ledger-annotations</artifactId>
    <version>${version.io.casehub.ledger}</version>
</dependency>
<dependency>
    <groupId>io.casehub</groupId>
    <artifactId>casehub-ledger-annotations-deployment</artifactId>
    <version>${version.io.casehub.ledger}</version>
</dependency>
```

- [ ] **Step 2: Add new example modules to `<modules>` section**

After the existing `examples/goap-case-annotated` entry (line 116):

```xml
<module>examples/incident-response-annotated</module>
<module>examples/search-rescue-annotated</module>
<module>examples/aircraft-maintenance-annotated</module>
<module>examples/warehouse-annotated</module>
<module>examples/wildfire-response-annotated</module>
```

- [ ] **Step 3: Verify root pom parses**

Run: `mvn -f pom.xml validate -N -q`
Expected: clean exit (no module resolution yet — modules don't exist)

- [ ] **Step 4: Commit**

```
wip: add cross-repo annotation dependencies and module entries

Refs #945
```

---

## Batch 2: Enrich existing examples — banking and legal

### Task 2: Enrich simple-case-annotated (banking) — @Worker(value), repeatable @Bind, cron

**Files:**
- Modify: `examples/simple-case-annotated/src/main/java/io/casehub/examples/SimpleAnnotatedCase.java`
- Modify: `examples/simple-case-annotated/src/test/java/io/casehub/examples/SimpleAnnotatedCaseTest.java`

**Interfaces:**
- Consumes: nothing
- Produces: banking example demonstrating `@Worker(value = "kycChecker")`, repeatable `@Bind` with cron trigger

- [ ] **Step 1: Write failing tests for new capabilities**

Add to `SimpleAnnotatedCaseTest.java`:

```java
@Test
void worker_name_override() {
    var kycWorker = definition.getWorkers().stream()
        .filter(w -> w.name().equals("kycChecker"))
        .findFirst();
    assertThat(kycWorker).isPresent();
    assertThat(kycWorker.get().capabilityNames()).contains("complianceCheck");
}

@Test
void repeatable_bind_on_compliance_worker() {
    var complianceBindings = definition.getBindings().stream()
        .filter(b -> b.getName().equals("kycChecker") || b.getName().equals("checkCompliance"))
        .toList();
    assertThat(complianceBindings.size()).isGreaterThanOrEqualTo(2);
}
```

Also update the existing `three_workers_with_descriptions` test — replace `"checkCompliance"` with `"kycChecker"` in the expected names list.

- [ ] **Step 2: Run tests to verify they fail**

Run: `TESTCONTAINERS_RYUK_DISABLED=true mvn install -DskipTests -q && TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl examples/simple-case-annotated -q`
Expected: failures on the new/updated tests

- [ ] **Step 3: Update SimpleAnnotatedCase.java**

Change the `checkCompliance` worker:

```java
@Worker(value = "kycChecker", capability = "complianceCheck",
        description = "Runs KYC/AML compliance screening")
@Bind(contextChange = ".identityResult != null", when = ".identityResult.verified == true")
@Bind(cron = "0 0 * * * ?")
default ComplianceResult checkCompliance(IdentityResult identityResult) {
    return new ComplianceResult("PASS", identityResult.referenceId());
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl examples/simple-case-annotated -q`
Expected: all tests pass

- [ ] **Step 5: Commit**

```
feat(#945): enrich banking example — @Worker(value), repeatable @Bind, cron trigger

Refs #945
```

### Task 3: Enrich goap-case-annotated (legal) — @Identity, @Disposition, standalone @Capability

**Files:**
- Modify: `examples/goap-case-annotated/pom.xml`
- Modify: `examples/goap-case-annotated/src/main/java/io/casehub/examples/GoapAnnotatedCase.java`
- Modify: `examples/goap-case-annotated/src/test/java/io/casehub/examples/GoapAnnotatedCaseTest.java`

**Interfaces:**
- Consumes: eidos-annotations dependency from Task 1
- Produces: legal example demonstrating `@Identity`, `@Disposition`, standalone `@Capability`

- [ ] **Step 1: Add eidos-annotations dependencies to goap pom.xml**

Add to `examples/goap-case-annotated/pom.xml` dependencies:

```xml
<dependency>
    <groupId>io.casehub</groupId>
    <artifactId>casehub-eidos-annotations</artifactId>
</dependency>
<dependency>
    <groupId>io.casehub</groupId>
    <artifactId>casehub-eidos-annotations-deployment</artifactId>
</dependency>
```

- [ ] **Step 2: Write failing tests**

Add to `GoapAnnotatedCaseTest.java`:

```java
@Test
void standalone_capability_declared() {
    assertThat(definition.getCapabilities().stream().map(c -> c.name()).toList())
        .contains("externalLegalOpinion");
}

@Test
void four_capabilities_including_standalone() {
    assertThat(definition.getCapabilities()).hasSize(4);
}
```

Update `withApplicationRoot` to include the new `LegalOpinion` record class.

- [ ] **Step 3: Run tests to verify they fail**

Run: `TESTCONTAINERS_RYUK_DISABLED=true mvn install -DskipTests -q && TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl examples/goap-case-annotated -q`
Expected: failures on standalone capability tests

- [ ] **Step 4: Update GoapAnnotatedCase.java**

Add imports:

```java
import io.casehub.eidos.annotations.Disposition;
import io.casehub.eidos.annotations.Identity;
import io.casehub.engine.annotations.Capability;
```

Add type-level eidos annotations:

```java
@Identity(slot = "contract-reviewer", provider = "casehub",
          modelFamily = "claude", jurisdiction = "EU")
@Disposition(socialOrient = "collaborative", ruleFollowing = "strict",
             riskAppetite = "cautious")
@Case(namespace = "legal", name = "ContractReview", version = "1.0.0",
      title = "Contract Review", planning = PlanningMode.GOAP)
public interface GoapAnnotatedCase {
```

Add standalone capability and its record:

```java
@Capability(name = "externalLegalOpinion",
            description = "External legal expert opinion — satisfied by MCP, A2A, or builder")
LegalOpinion externalOpinion(AnalysisResult analysisResult);
```

Add `@SoftDependency LegalOpinion legalOpinion` parameter to `assessRisk` and update the body to use it. Add `LegalOpinion` record:

```java
record LegalOpinion(String opinion, String source) {}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl examples/goap-case-annotated -q`
Expected: all tests pass

- [ ] **Step 6: Commit**

```
feat(#945): enrich legal example — @Identity, @Disposition, standalone @Capability

Refs #945
```

---

## Batch 3: Incident Response example (cybersecurity, EXPLICIT, all 4 layers)

### Task 4: Create incident-response-annotated module

**Files:**
- Create: `examples/incident-response-annotated/pom.xml`
- Create: `examples/incident-response-annotated/src/main/java/io/casehub/examples/IncidentResponseCase.java`
- Create: `examples/incident-response-annotated/src/test/java/io/casehub/examples/IncidentResponseCaseTest.java`

**Interfaces:**
- Consumes: all 4 annotation dependency sets from Task 1
- Produces: cybersecurity incident response case with `@SystemPrompt`, `@Bind(cron)`, `@Identity`, `@Disposition`, `@AgentGoals`, `@HumanApproval`, `@Escalate`, `@Audited`, `@ComplianceSupplement`

- [ ] **Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>io.casehub</groupId>
        <artifactId>casehub-engine-parent</artifactId>
        <version>0.2-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>casehub-examples-incident-response-annotated</artifactId>
    <name>Case Hub :: Examples :: Incident Response (Annotated)</name>
    <description>Cybersecurity incident response demonstrating all four annotation layers</description>

    <properties>
        <maven.deploy.skip>true</maven.deploy.skip>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.casehub</groupId>
            <artifactId>casehub-engine-annotations</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>io.casehub</groupId>
            <artifactId>casehub-engine-annotations-deployment</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>io.casehub</groupId>
            <artifactId>casehub-eidos-annotations</artifactId>
        </dependency>
        <dependency>
            <groupId>io.casehub</groupId>
            <artifactId>casehub-eidos-annotations-deployment</artifactId>
        </dependency>
        <dependency>
            <groupId>io.casehub</groupId>
            <artifactId>casehub-work-annotations</artifactId>
        </dependency>
        <dependency>
            <groupId>io.casehub</groupId>
            <artifactId>casehub-work-annotations-deployment</artifactId>
        </dependency>
        <dependency>
            <groupId>io.casehub</groupId>
            <artifactId>casehub-ledger-annotations</artifactId>
        </dependency>
        <dependency>
            <groupId>io.casehub</groupId>
            <artifactId>casehub-ledger-annotations-deployment</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-arc</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-junit5-internal</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>io.smallrye</groupId>
                <artifactId>jandex-maven-plugin</artifactId>
                <executions>
                    <execution>
                        <id>make-index</id>
                        <goals>
                            <goal>jandex</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <systemPropertyVariables>
                        <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
                        <maven.home>${maven.home}</maven.home>
                    </systemPropertyVariables>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Write the test class**

Create `IncidentResponseCaseTest.java` with assertions for: namespace/name, 5 workers, `@SystemPrompt` workers produce `AgentWorkerFunction`, bindings include cron trigger, goal wiring, milestone wiring. Follow the `QuarkusUnitTest` + `withApplicationRoot` pattern from existing examples.

- [ ] **Step 3: Write IncidentResponseCase.java**

Create the interface with all annotations as specified in the design spec's Example 3 section. Include all inner record types.

- [ ] **Step 4: Run tests**

Run: `TESTCONTAINERS_RYUK_DISABLED=true mvn install -DskipTests -q && TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl examples/incident-response-annotated -q`
Expected: all tests pass

- [ ] **Step 5: Commit**

```
feat(#945): add incident response example — 4-layer annotation composition

Refs #945
```

---

## Batch 4: Search & Rescue example (emergency, GOAP)

### Task 5: Create search-rescue-annotated module

**Files:**
- Create: `examples/search-rescue-annotated/pom.xml`
- Create: `examples/search-rescue-annotated/src/main/java/io/casehub/examples/SearchRescueCase.java`
- Create: `examples/search-rescue-annotated/src/test/java/io/casehub/examples/SearchRescueCaseTest.java`

**Interfaces:**
- Consumes: all 4 annotation dependency sets
- Produces: GOAP search & rescue case with `@SystemPrompt`, standalone `@Capability`, `@AgentGoals`, `@AgentConstraints`, `@HumanApproval`, `@Escalate`, `@Audited`

Same pom.xml pattern as Task 4. Interface and test as specified in design spec Example 4. GOAP-specific test assertions: `getPlanningStrategy()` is `"goap"`, `getGoapActions()` verify dependency chain (assessConditions → deployDrones → assessMedical → authoriseEvacuation), standalone `fieldRescueTeam` capability exists.

- [ ] **Steps 1-5: Create pom, write test, write interface, run tests, commit**

Follow the same 5-step pattern as Task 4.

Commit message:
```
feat(#945): add search & rescue example — GOAP with safety constraints

Refs #945
```

---

## Batch 5: Aircraft Maintenance example (aviation MRO, EXPLICIT)

### Task 6: Create aircraft-maintenance-annotated module

**Files:**
- Create: `examples/aircraft-maintenance-annotated/pom.xml`
- Create: `examples/aircraft-maintenance-annotated/src/main/java/io/casehub/examples/AircraftMaintenanceCase.java`
- Create: `examples/aircraft-maintenance-annotated/src/test/java/io/casehub/examples/AircraftMaintenanceCaseTest.java`

**Interfaces:**
- Consumes: all 4 annotation dependency sets
- Produces: aviation MRO case with `@SystemPrompt`, `@HumanApproval`, `@RequiresQuorum`, `@Escalate`, `@Audited`, `@ComplianceSupplement`

Same pom.xml pattern. Interface and test as specified in design spec Example 5. Key test assertions: `@RequiresQuorum(instances = 2, required = 2)` on certify worker, `@ComplianceSupplement` on assess and certify workers.

- [ ] **Steps 1-5: Create pom, write test, write interface, run tests, commit**

Commit message:
```
feat(#945): add aircraft maintenance example — compliance and quorum approval

Refs #945
```

---

## Batch 6: Warehouse example (logistics, GOAP)

### Task 7: Create warehouse-annotated module

**Files:**
- Create: `examples/warehouse-annotated/pom.xml`
- Create: `examples/warehouse-annotated/src/main/java/io/casehub/examples/WarehouseCase.java`
- Create: `examples/warehouse-annotated/src/test/java/io/casehub/examples/WarehouseCaseTest.java`

**Interfaces:**
- Consumes: all 4 annotation dependency sets
- Produces: GOAP warehouse fulfillment case with `@SystemPrompt`, `@SoftDependency`, `@HumanApproval`, `@Audited`

Same pom.xml pattern. Interface and test as specified in design spec Example 6. GOAP test assertions: `@SoftDependency HazmatClearance` on packAndDispatch, GOAP actions verify pick route → pick items → quality check → pack chain.

- [ ] **Steps 1-5: Create pom, write test, write interface, run tests, commit**

Commit message:
```
feat(#945): add warehouse example — GOAP fulfillment with hazmat approval

Refs #945
```

---

## Batch 7: Wildfire Response example (disaster management, GOAP)

### Task 8: Create wildfire-response-annotated module

**Files:**
- Create: `examples/wildfire-response-annotated/pom.xml`
- Create: `examples/wildfire-response-annotated/src/main/java/io/casehub/examples/WildfireResponseCase.java`
- Create: `examples/wildfire-response-annotated/src/test/java/io/casehub/examples/WildfireResponseCaseTest.java`

**Interfaces:**
- Consumes: all 4 annotation dependency sets
- Produces: GOAP wildfire response case with `@SystemPrompt`, standalone `@Capability`, `@AgentGoals`, `@AgentConstraints`, `@HumanApproval`, `@Escalate`, `@Audited`, `@ComplianceSupplement`

Same pom.xml pattern. Interface and test as specified in design spec Example 7.

- [ ] **Steps 1-5: Create pom, write test, write interface, run tests, commit**

Commit message:
```
feat(#945): add wildfire response example — multi-agency GOAP with constraints

Refs #945
```

---

## Batch 8: Capability matrix update and full verification

### Task 9: Update CAPABILITY-MATRIX.md

**Files:**
- Modify: `annotations/CAPABILITY-MATRIX.md`

**Interfaces:**
- Consumes: all 7 completed examples
- Produces: updated matrix showing 21/21 engine capabilities covered by examples

- [ ] **Step 1: Update the examples table**

Add columns for all 5 new examples. Update existing example column names if needed.

- [ ] **Step 2: Update the capability rows**

Mark all previously deployment-test-only capabilities with their new example coverage:
- `@Worker(value = "name")` → Customer Onboarding ✓
- `@SystemPrompt("prompt")` → all 5 new examples ✓
- Standalone `@Capability` → Contract Review ✓, Search & Rescue ✓, Wildfire Response ✓
- Repeatable `@Bind` / cron trigger → Customer Onboarding ✓, Incident Response ✓

- [ ] **Step 3: Add cross-repo composition section**

```markdown
## Cross-Repo Annotation Composition

| Upstream Module | Annotation | Incident Response | Search & Rescue | Aircraft Maint. | Warehouse | Wildfire |
|-----------------|------------|:-----------------:|:---------------:|:---------------:|:---------:|:-------:|
| eidos | `@Identity` | ✓ | ✓ | ✓ | ✓ | ✓ |
| eidos | `@Disposition` | ✓ | ✓ | ✓ | ✓ | ✓ |
| eidos | `@AgentGoals` | ✓ | ✓ | — | — | ✓ |
| eidos | `@AgentConstraints` | — | ✓ | — | — | ✓ |
| work | `@HumanApproval` | ✓ | ✓ | ✓ | ✓ | ✓ |
| work | `@Escalate` | ✓ | ✓ | ✓ | — | ✓ |
| work | `@RequiresQuorum` | — | — | ✓ | — | — |
| ledger | `@Audited` | ✓ | ✓ | ✓ | ✓ | ✓ |
| ledger | `@ComplianceSupplement` | ✓ | — | ✓ | — | ✓ |
```

- [ ] **Step 4: Update coverage summary**

```markdown
| Category | Total | In Examples | In Deployment Tests Only |
|----------|-------|-------------|--------------------------|
| Annotations | 15 | 15 | 0 |
| Inference | 3 | 3 | — |
| Build pipeline | 3 | 2 | 1 (validation) |
| **Total** | **21** | **20** | **1** |
```

- [ ] **Step 5: Run all example tests together**

Run: `TESTCONTAINERS_RYUK_DISABLED=true mvn install -DskipTests -q && TESTCONTAINERS_RYUK_DISABLED=true mvn test -pl examples/simple-case-annotated,examples/goap-case-annotated,examples/incident-response-annotated,examples/search-rescue-annotated,examples/aircraft-maintenance-annotated,examples/warehouse-annotated,examples/wildfire-response-annotated -q`
Expected: all tests pass across all 7 examples

- [ ] **Step 6: Commit**

```
feat(#945): update CAPABILITY-MATRIX.md — 21/21 coverage with cross-repo composition

Closes #945
```

## References

- `specs/issue-945-enrich-annotation-examples/2026-08-20-annotation-examples-enrichment-design.md` — design spec
- `docs/specs/issue-909-engine-annotations/2026-08-16-annotation-driven-programming-model-design.md` — annotation architecture
- `annotations/CAPABILITY-MATRIX.md` — current coverage matrix
- `examples/simple-case-annotated/` — existing banking example pattern
- `examples/goap-case-annotated/` — existing legal GOAP example pattern
- GE-20260813 — Quarkus `@Scheduled` duration format (cron strings are fine)
- casehubio/engine#945, casehubio/eidos#141, casehubio/eidos#139
