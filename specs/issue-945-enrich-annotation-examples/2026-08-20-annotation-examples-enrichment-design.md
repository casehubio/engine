# Annotation Examples Enrichment

**Date:** 2026-08-20
**Issue:** casehubio/engine#945
**Status:** Draft

## Motivation

The engine annotation examples cover 17 of 21 capabilities and pre-date the eidos, work, and ledger annotation modules. Four capabilities are only exercised in deployment tests. No example anywhere in the ecosystem composes annotations from multiple repos — the full declarative agent story (`@Case` + `@Identity` + `@HumanApproval` + `@Audited`) has never been demonstrated.

This enriches the 2 existing examples and adds 5 new domain-grounded examples that show cross-repo annotation composition across all four annotation-enabled repos (engine, eidos, work, ledger). Each example is designed as a layered base that blocks can later enhance with orchestration patterns.

## Scope

### Engine annotation gaps to cover

| Capability | Currently | After |
|---|---|---|
| `@Worker(value = "name")` | Deployment test only | Banking example |
| `@SystemPrompt("prompt")` | Deployment test only | All 5 new examples |
| Standalone `@Capability` | Deployment test only | Search & rescue, wildfire |
| Repeatable `@Bind` / cron trigger | Deployment test only | Banking, incident response |

### Cross-repo dependencies to add

Root `pom.xml` `<dependencyManagement>`:

```xml
<!-- Eidos annotations -->
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

<!-- Work annotations -->
<dependency>
    <groupId>io.casehub</groupId>
    <artifactId>casehub-work-annotations</artifactId>
    <version>${version.io.casehub.work}</version>
</dependency>
<dependency>
    <groupId>io.casehub</groupId>
    <artifactId>casehub-work-annotations-deployment</artifactId>
    <version>${version.io.casehub.work}</version>
</dependency>

<!-- Ledger annotations -->
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

## Design Principle: Layered for Blocks Enhancement

Each example is a complete, working engine-level case. Domain models, worker boundaries, and capability structures are designed so blocks can later layer orchestration patterns on top:

| Blocks Pattern | Natural Extension Point |
|---|---|
| `@DebateAgent` | Incident response containment strategy, wildfire resource allocation |
| `@OversightGate` | Aircraft maintenance repair authorization, search & rescue evacuation |
| `@TrustRouted` | Warehouse route safety, incident response triage confidence |
| `@VotingAgent` | Wildfire multi-agency resource consensus |
| `@HtnAgent` | Search & rescue multi-phase operation decomposition |

Workers that are candidates for blocks enhancement are noted with `// blocks: @Pattern` comments in the examples.

---

## Example 1: Customer Onboarding — ENRICH (banking, EXPLICIT)

**Module:** `examples/simple-case-annotated`
**Engine-only** — no upstream annotation dependencies. The "hello world" entry point.

### Changes

1. **`@Worker(value = "kycChecker")`** — rename the compliance worker to demonstrate name override
2. **Repeatable `@Bind`** — add a second `@Bind(cron = "0 0 * * * ?")` on the compliance worker for periodic re-screening
3. **Cron trigger** — the hourly cron re-fires the compliance check (periodic KYC refresh is a real banking pattern)

### Updated interface sketch

```java
@Case(namespace = "banking", name = "CustomerOnboarding", version = "1.0.0",
      title = "Customer Onboarding",
      summary = "Opens a new bank account — verifies identity, runs compliance, provisions")
public interface SimpleAnnotatedCase {

    @Worker(capability = "verifyIdentity", description = "Verifies customer identity documents")
    @Bind(contextChange = ".application != null")
    default IdentityResult verifyIdentity(String application) { ... }

    @Worker(value = "kycChecker", capability = "complianceCheck",
            description = "Runs KYC/AML compliance screening")
    @Bind(contextChange = ".identityResult != null", when = ".identityResult.verified == true")
    @Bind(cron = "0 0 * * * ?")
    default ComplianceResult checkCompliance(IdentityResult identityResult) { ... }

    @Worker(capability = "provisionAccount")
    @Bind(contextChange = ".complianceResult != null", when = ".complianceResult.status == 'PASS'")
    default Account provisionAccount(ComplianceResult complianceResult) { ... }

    // existing milestone, goal, customize unchanged
}
```

### Blocks extension points

- `provisionAccount` → `@OversightGate` for high-value accounts
- `checkCompliance` → `@TrustRouted` for routing to experienced compliance agents

---

## Example 2: Contract Review — ENRICH (legal, GOAP)

**Module:** `examples/goap-case-annotated`
**Adds:** eidos annotations (`@Identity`, `@Disposition`), standalone `@Capability`

### Changes

1. **`@Identity` + `@Disposition`** on the interface — the contract reviewer has a cautious, rule-following personality
2. **Standalone `@Capability`** — declare an `externalLegalOpinion` capability with no `@Worker`. This represents an external legal expert (MCP, A2A, or builder-defined) that the GOAP planner can incorporate

### Updated interface sketch

```java
@Identity(slot = "contract-reviewer", provider = "casehub",
          modelFamily = "claude", jurisdiction = "EU")
@Disposition(socialOrient = "collaborative", ruleFollowing = "strict",
             riskAppetite = "cautious")
@Case(namespace = "legal", name = "ContractReview", version = "1.0.0",
      title = "Contract Review", planning = PlanningMode.GOAP)
public interface GoapAnnotatedCase {

    @Worker(capability = "analyse", cost = 0.2, benefit = 0.1,
            description = "Analyses contract structure and key terms")
    default AnalysisResult analyse(String contract) { ... }

    @Worker(capability = "extractClauses", cost = 0.3)
    default ClauseList extractClauses(String contract, AnalysisResult analysisResult) { ... }

    @Capability(name = "externalLegalOpinion",
                description = "External legal expert opinion — satisfied by MCP, A2A, or builder")
    LegalOpinion externalOpinion(AnalysisResult analysisResult);

    @Worker(capability = "assessRisk", cost = 0.5)
    @Effect("riskAssessment")
    default RiskReport assessRisk(
            AnalysisResult analysisResult, ClauseList clauseList,
            @SoftDependency LegalOpinion legalOpinion,
            @SoftDependency PriorReview priorReview,
            @Param("jurisdiction") String jurisdiction) { ... }

    // existing goal, completion unchanged

    record LegalOpinion(String opinion, String source) {}
}
```

### Blocks extension points

- `assessRisk` → `@DebateAgent` between risk-averse and commercial-minded reviewers
- `externalLegalOpinion` → `@TrustRouted` based on jurisdiction expertise

---

## Example 3: Incident Response (NEW — cybersecurity, EXPLICIT)

**Module:** `examples/incident-response-annotated`
**Annotations:** engine + eidos + work + ledger (all four layers)

### Domain

Cybersecurity incident: detect anomaly → triage severity → contain threat → remediate → generate compliance report. Periodic scanning via cron. Human approval before containment actions.

### Interface sketch

```java
@Identity(slot = "security-analyst", provider = "casehub",
          modelFamily = "claude", jurisdiction = "US")
@Disposition(socialOrient = "independent", ruleFollowing = "strict",
             riskAppetite = "cautious", autonomy = "bounded")
@AgentGoals({
    @AgentGoalDef(name = "minimise-blast-radius",
                  description = "Contain threats before they spread",
                  priority = AgentGoalDef.Priority.PRIMARY),
    @AgentGoalDef(name = "preserve-evidence",
                  description = "Maintain forensic chain of custody",
                  priority = AgentGoalDef.Priority.SECONDARY)
})
@Case(namespace = "security", name = "IncidentResponse", version = "1.0.0",
      title = "Incident Response",
      summary = "Detects, triages, contains, and remediates cybersecurity incidents")
public interface IncidentResponseCase {

    @Worker(capability = "scanForAnomalies",
            description = "Periodic security scan for anomalous activity")
    @Bind(cron = "0 */15 * * * ?")
    @Bind(contextChange = ".newAlert != null")
    @Audited(entryType = Audited.EntryType.EVENT)
    @SystemPrompt("You are a security monitoring agent. Analyse system logs and network traffic for anomalies. Return structured findings.")
    ScanResult scan();

    @Worker(capability = "triageSeverity",
            description = "Classifies incident severity: P1-critical, P2-high, P3-medium, P4-low")
    @Bind(contextChange = ".scanResult != null", when = ".scanResult.anomalyDetected == true")
    @Audited
    @SystemPrompt("You are a security triage specialist. Given scan findings, classify the severity and recommend immediate actions.")
    TriageAssessment triage(ScanResult scanResult);

    @Worker(capability = "containThreat",
            description = "Executes containment: isolate host, block IP, revoke credentials")
    @Bind(contextChange = ".triageAssessment != null",
          when = ".triageAssessment.severity == 'P1' or .triageAssessment.severity == 'P2'")
    @HumanApproval(title = "Approve containment action",
                   candidateGroups = "security-ops",
                   claimDeadline = "PT10M")
    @Escalate(onExpiry = "security-director", deadline = "PT30M")
    @Audited(auditFailures = true)
    default ContainmentResult contain(TriageAssessment triageAssessment) {
        return new ContainmentResult(
            triageAssessment.recommendedActions(),
            "contained",
            java.time.Instant.now().toString());
    }

    @Worker(capability = "remediate",
            description = "Applies fixes: patch, config change, credential rotation")
    @Bind(contextChange = ".containmentResult != null",
          when = ".containmentResult.status == 'contained'")
    @Audited
    @SystemPrompt("You are a remediation engineer. Given the containment results, propose and execute remediation steps.")
    RemediationReport remediate(ContainmentResult containmentResult,
                                TriageAssessment triageAssessment);

    @Worker(capability = "generateComplianceReport",
            description = "Produces incident report for regulatory compliance")
    @Bind(contextChange = ".remediationReport != null")
    @Audited
    @ComplianceSupplement(algorithmRef = "incident-response-v2",
                          humanOverrideAvailable = true)
    @SystemPrompt("You are a compliance reporting agent. Compile all incident data into a structured compliance report following NIST SP 800-61 format.")
    ComplianceReport report(ScanResult scanResult,
                            TriageAssessment triageAssessment,
                            ContainmentResult containmentResult,
                            RemediationReport remediationReport);

    @Milestone(name = "threatContained",
               entryCriteria = ".triageAssessment != null",
               completionCriteria = ".containmentResult.status == 'contained'")
    default void threatContained() {}

    @Goal(value = "Incident fully resolved and reported",
          condition = ".complianceReport != null")
    @Completion
    default GoalExpression resolved() {
        return GoalExpression.goal("resolved");
    }

    record ScanResult(boolean anomalyDetected, String finding, String source, String severity) {}
    record TriageAssessment(String severity, String category, List<String> recommendedActions,
                            String justification) {}
    record ContainmentResult(List<String> actionsApplied, String status, String timestamp) {}
    record RemediationReport(List<String> fixes, String status, String verificationResult) {}
    record ComplianceReport(String incidentId, String nistCategory, String timeline,
                            String impact, String lessonsLearned) {}
}
```

### Blocks extension points

- `containThreat` → `@DebateAgent` between aggressive containment and business-continuity perspectives
- `triageSeverity` → `@TrustRouted` based on analyst experience with threat category
- Full workflow → `@HtnAgent` for multi-phase incident decomposition

---

## Example 4: Search & Rescue (NEW — emergency services, GOAP)

**Module:** `examples/search-rescue-annotated`
**Annotations:** engine + eidos + work + ledger

### Domain

Missing person search: assess conditions → deploy drones → locate survivor → assess medical needs → dispatch rescue team → evacuate. GOAP plans the operation from typed dependencies. External rescue team is a standalone capability.

### Interface sketch

```java
@Identity(slot = "rescue-coordinator", provider = "casehub",
          modelFamily = "claude")
@Disposition(socialOrient = "directive", ruleFollowing = "strict",
             riskAppetite = "cautious")
@AgentGoals({
    @AgentGoalDef(name = "locate-and-extract",
                  description = "Find and safely extract the missing person",
                  priority = AgentGoalDef.Priority.PRIMARY),
    @AgentGoalDef(name = "responder-safety",
                  description = "No responder injuries during the operation",
                  priority = AgentGoalDef.Priority.PRIMARY)
})
@AgentConstraints({
    @AgentConstraintDef(name = "weather-limits",
                        description = "Do not deploy air assets in winds exceeding 40 knots",
                        severity = AgentConstraintDef.Severity.HARD),
    @AgentConstraintDef(name = "daylight-ops",
                        description = "Ground search only during daylight unless equipped for night ops",
                        severity = AgentConstraintDef.Severity.SOFT)
})
@Case(namespace = "emergency", name = "SearchAndRescue", version = "1.0.0",
      title = "Search and Rescue",
      summary = "Coordinates search and rescue for missing persons",
      planning = PlanningMode.GOAP)
public interface SearchRescueCase {

    @Worker(capability = "assessConditions", cost = 0.1,
            description = "Evaluates weather, terrain, and last-known-position data")
    @SystemPrompt("You are a search operations planner. Assess conditions and recommend search strategy.")
    @Audited
    ConditionAssessment assessConditions(String lastKnownPosition, String terrain);

    @Worker(capability = "deployDrones", cost = 0.3,
            description = "Plans and deploys drone search grid")
    @SystemPrompt("You are a drone operations coordinator. Plan optimal search grid based on conditions.")
    @Audited
    DroneSearchResult deployDrones(ConditionAssessment conditionAssessment);

    @Worker(capability = "assessMedical", cost = 0.2,
            description = "Remote medical assessment from drone imagery and vitals")
    @SystemPrompt("You are a remote medical assessor. Evaluate survivor condition from available data.")
    @Audited
    MedicalAssessment assessMedical(DroneSearchResult droneSearchResult);

    @Capability(name = "fieldRescueTeam",
                description = "On-ground rescue team — external capability, not an AI agent")
    RescueResult dispatchRescue(MedicalAssessment medicalAssessment,
                                ConditionAssessment conditionAssessment);

    @Worker(capability = "authoriseEvacuation", cost = 0.1,
            description = "Authorises evacuation method based on medical and terrain data")
    @HumanApproval(title = "Authorise evacuation plan",
                   candidateGroups = "incident-commander")
    @Escalate(onExpiry = "regional-coordinator", deadline = "PT15M")
    @Audited
    default EvacuationPlan authoriseEvacuation(MedicalAssessment medicalAssessment,
                                               RescueResult rescueResult) {
        String method = medicalAssessment.criticalCondition() ? "helicopter" : "ground";
        return new EvacuationPlan(method, rescueResult.location(), medicalAssessment.priority());
    }

    @Goal(value = "Person located and evacuated",
          condition = ".evacuationPlan != null")
    @Completion
    default GoalExpression rescued() {
        return GoalExpression.goal("rescued");
    }

    record ConditionAssessment(String weather, String terrainDifficulty, String searchStrategy,
                               boolean safeForAir) {}
    record DroneSearchResult(boolean personLocated, String location, String imagery) {}
    record MedicalAssessment(boolean criticalCondition, String priority,
                             List<String> observedConditions) {}
    record RescueResult(String status, String location, String teamId) {}
    record EvacuationPlan(String method, String destination, String priority) {}
}
```

### Blocks extension points

- `authoriseEvacuation` → `@OversightGate` with trust-based routing
- Full operation → `@HtnAgent` for multi-phase decomposition (search phase → rescue phase → evacuation phase)

---

## Example 5: Aircraft Maintenance (NEW — aviation MRO, EXPLICIT)

**Module:** `examples/aircraft-maintenance-annotated`
**Annotations:** engine + eidos + work + ledger (strongest ledger/compliance fit)

### Domain

Defect reported → severity assessment → repair plan → parts approval → repair execution → dual-inspector sign-off → airworthiness certification. Heavy regulatory compliance (EASA/FAA). Quorum approval for safety-critical sign-off.

### Interface sketch

```java
@Identity(slot = "maintenance-engineer", provider = "casehub",
          modelFamily = "claude", jurisdiction = "EU")
@Disposition(socialOrient = "methodical", ruleFollowing = "strict",
             riskAppetite = "risk-averse")
@Case(namespace = "aviation", name = "AircraftMaintenance", version = "1.0.0",
      title = "Aircraft Maintenance",
      summary = "Manages aircraft defect assessment, repair, and airworthiness certification")
public interface AircraftMaintenanceCase {

    @Worker(capability = "assessDefect",
            description = "Classifies defect severity per MEL/CDL categories")
    @Bind(contextChange = ".defectReport != null")
    @Audited
    @ComplianceSupplement(algorithmRef = "easa-part-145-defect-classification")
    @SystemPrompt("You are an aircraft maintenance engineer. Classify the defect severity per EASA Part-145 MEL/CDL categories. Be conservative — when uncertain, classify higher.")
    DefectAssessment assessDefect(String defectReport, String aircraftType);

    @Worker(capability = "planRepair",
            description = "Creates repair plan with required parts and procedures")
    @Bind(contextChange = ".defectAssessment != null")
    @Audited
    @SystemPrompt("You are a repair planning specialist. Create a detailed repair plan following the aircraft maintenance manual. List required parts, tools, and estimated time.")
    RepairPlan planRepair(DefectAssessment defectAssessment, String aircraftType);

    @Worker(capability = "approveParts",
            description = "Validates parts availability and authorises procurement")
    @Bind(contextChange = ".repairPlan != null",
          when = ".repairPlan.partsRequired | length > 0")
    @HumanApproval(title = "Approve parts procurement",
                   candidateGroups = "parts-authority")
    @Audited
    default PartsApproval approveParts(RepairPlan repairPlan) {
        return new PartsApproval(repairPlan.partsRequired(), "approved",
                                java.time.Instant.now().toString());
    }

    @Worker(capability = "executeRepair",
            description = "Executes repair procedures and records work performed")
    @Bind(contextChange = ".partsApproval != null",
          when = ".partsApproval.status == 'approved'")
    @Audited(auditFailures = true)
    default RepairRecord executeRepair(RepairPlan repairPlan, PartsApproval partsApproval) {
        return new RepairRecord(repairPlan.procedures(), "completed",
                                partsApproval.approvedParts());
    }

    @Worker(capability = "certifyAirworthy",
            description = "Dual-inspector airworthiness certification")
    @Bind(contextChange = ".repairRecord != null",
          when = ".repairRecord.status == 'completed'")
    @HumanApproval(title = "Certify aircraft airworthy",
                   candidateGroups = "licensed-inspectors")
    @RequiresQuorum(instances = 2, required = 2,
                    candidateGroups = "licensed-inspectors")
    @Escalate(onExpiry = "chief-inspector", deadline = "PT4H")
    @Audited
    @ComplianceSupplement(algorithmRef = "easa-part-145-release-to-service",
                          humanOverrideAvailable = false)
    default AirworthinessCertification certify(RepairRecord repairRecord,
                                               DefectAssessment defectAssessment) {
        return new AirworthinessCertification(
            defectAssessment.defectId(), "CRS-" + System.currentTimeMillis(),
            "fit-for-service");
    }

    @Milestone(name = "repairComplete",
               entryCriteria = ".repairPlan != null",
               completionCriteria = ".repairRecord.status == 'completed'")
    default void repairComplete() {}

    @Goal(value = "Aircraft certified airworthy",
          condition = ".airworthinessCertification != null")
    @Completion
    default GoalExpression certified() {
        return GoalExpression.goal("certified");
    }

    record DefectAssessment(String defectId, String severity, String melCategory,
                            List<String> affectedSystems) {}
    record RepairPlan(List<String> procedures, List<String> partsRequired,
                      String estimatedTime) {}
    record PartsApproval(List<String> approvedParts, String status, String timestamp) {}
    record RepairRecord(List<String> proceduresPerformed, String status,
                        List<String> partsUsed) {}
    record AirworthinessCertification(String defectId, String certificateNumber,
                                      String verdict) {}
}
```

### Blocks extension points

- `certifyAirworthy` → `@OversightGate` with trust-weighted inspector routing
- `assessDefect` → `@TrustRouted` based on engineer experience with aircraft type
- `planRepair` → `@DebateAgent` between cost-optimised and safety-first repair strategies

---

## Example 6: Autonomous Warehouse (NEW — logistics, GOAP)

**Module:** `examples/warehouse-annotated`
**Annotations:** engine + eidos + work + ledger

### Domain

Order received → plan pick route → pick items → quality check → pack → dispatch. GOAP plans the fulfillment from typed dependencies. AI route optimiser with `@SystemPrompt`. Human approval for hazardous materials.

### Interface sketch

```java
@Identity(slot = "warehouse-optimizer", provider = "casehub",
          modelFamily = "claude")
@Disposition(socialOrient = "efficient", ruleFollowing = "flexible",
             riskAppetite = "moderate")
@Case(namespace = "logistics", name = "WarehouseFulfillment", version = "1.0.0",
      title = "Warehouse Fulfillment",
      summary = "Optimises order picking, packing, and dispatch in an automated warehouse",
      planning = PlanningMode.GOAP)
public interface WarehouseCase {

    @Worker(capability = "planRoute", cost = 0.2,
            description = "Optimises pick route through warehouse zones")
    @SystemPrompt("You are a warehouse route optimizer. Given the order items and warehouse layout, plan the most efficient pick route minimising travel distance and zone transitions.")
    @Audited
    PickRoute planRoute(String orderId, List<String> items);

    @Worker(capability = "pickItems", cost = 0.4,
            description = "Picks items along the planned route")
    @Audited
    default PickResult pickItems(PickRoute pickRoute) {
        return new PickResult(pickRoute.items(), pickRoute.zones(), "picked");
    }

    @Worker(capability = "qualityCheck", cost = 0.2,
            description = "Inspects picked items for damage and correctness")
    @SystemPrompt("You are a quality inspector. Verify picked items match the order and check for damage.")
    @Audited
    QualityReport qualityCheck(PickResult pickResult);

    @Worker(capability = "handleHazmat", cost = 0.3,
            description = "Special handling for hazardous materials")
    @HumanApproval(title = "Approve hazmat handling procedure",
                   candidateGroups = "hazmat-certified")
    @Audited(auditFailures = true)
    default HazmatClearance handleHazmat(PickResult pickResult, QualityReport qualityReport) {
        return new HazmatClearance("cleared", qualityReport.hazmatItems());
    }

    @Worker(capability = "packAndDispatch", cost = 0.2,
            description = "Packs items and generates shipping label")
    @Audited
    default DispatchConfirmation packAndDispatch(QualityReport qualityReport,
                                                 @SoftDependency HazmatClearance hazmatClearance) {
        String hazmatStatus = hazmatClearance != null ? hazmatClearance.status() : "not-applicable";
        return new DispatchConfirmation("SHIP-" + System.currentTimeMillis(),
                                        qualityReport.verifiedItems(), hazmatStatus);
    }

    @Goal(value = "Order dispatched", condition = ".dispatchConfirmation != null")
    @Completion
    default GoalExpression dispatched() {
        return GoalExpression.goal("dispatched");
    }

    record PickRoute(List<String> items, List<String> zones, String estimatedTime) {}
    record PickResult(List<String> pickedItems, List<String> visitedZones, String status) {}
    record QualityReport(List<String> verifiedItems, List<String> hazmatItems,
                         boolean allCorrect) {}
    record HazmatClearance(String status, List<String> clearedItems) {}
    record DispatchConfirmation(String shipmentId, List<String> items,
                                String hazmatStatus) {}
}
```

### Blocks extension points

- `planRoute` → `@TrustRouted` for safety-critical route segments
- `qualityCheck` → `@DebateAgent` on borderline damage assessments
- Full fulfillment → `@HtnAgent` for decomposing multi-order batch fulfillment

---

## Example 7: Wildfire Response (NEW — disaster management, GOAP)

**Module:** `examples/wildfire-response-annotated`
**Annotations:** engine + eidos + work + ledger

### Domain

Satellite detects fire → assess risk → allocate resources → issue evacuation orders → containment operations → damage assessment. Multi-agency coordination with safety constraints.

### Interface sketch

```java
@Identity(slot = "wildfire-commander", provider = "casehub",
          modelFamily = "claude")
@Disposition(socialOrient = "directive", ruleFollowing = "strict",
             riskAppetite = "cautious", autonomy = "bounded")
@AgentGoals({
    @AgentGoalDef(name = "protect-life",
                  description = "Civilian and responder safety is the absolute priority",
                  priority = AgentGoalDef.Priority.PRIMARY),
    @AgentGoalDef(name = "contain-spread",
                  description = "Prevent fire from reaching populated areas",
                  priority = AgentGoalDef.Priority.PRIMARY)
})
@AgentConstraints({
    @AgentConstraintDef(name = "responder-safety",
                        description = "Never deploy ground crews into unassessed terrain",
                        severity = AgentConstraintDef.Severity.HARD),
    @AgentConstraintDef(name = "air-tanker-limits",
                        description = "No aerial drops in winds exceeding 35 knots",
                        severity = AgentConstraintDef.Severity.HARD)
})
@Case(namespace = "disaster", name = "WildfireResponse", version = "1.0.0",
      title = "Wildfire Response",
      summary = "Coordinates wildfire detection, evacuation, and containment across agencies",
      planning = PlanningMode.GOAP)
public interface WildfireResponseCase {

    @Worker(capability = "assessFireRisk", cost = 0.1,
            description = "Analyses satellite imagery and weather data to assess fire spread risk")
    @SystemPrompt("You are a wildfire risk analyst. Assess fire spread probability based on satellite imagery, wind patterns, terrain, and vegetation density. Classify risk zones.")
    @Audited
    FireRiskAssessment assessRisk(String satelliteData, String weatherData);

    @Worker(capability = "allocateResources", cost = 0.3,
            description = "Assigns fire crews, air tankers, and equipment to sectors")
    @SystemPrompt("You are a resource allocation coordinator. Assign available crews and equipment to fire sectors based on risk priority and resource proximity.")
    @Audited
    ResourceAllocation allocateResources(FireRiskAssessment fireRiskAssessment);

    @Worker(capability = "issueEvacuation", cost = 0.2,
            description = "Issues evacuation orders for at-risk communities")
    @HumanApproval(title = "Approve evacuation order",
                   candidateGroups = "incident-commander")
    @Escalate(onExpiry = "state-emergency-director", deadline = "PT20M")
    @Audited
    @ComplianceSupplement(algorithmRef = "nims-evacuation-protocol",
                          humanOverrideAvailable = true)
    default EvacuationOrder issueEvacuation(FireRiskAssessment fireRiskAssessment,
                                            ResourceAllocation resourceAllocation) {
        List<String> zones = fireRiskAssessment.highRiskZones();
        return new EvacuationOrder(zones, "mandatory", resourceAllocation.assignedRoutes());
    }

    @Capability(name = "groundContainment",
                description = "Ground crew containment operations — external field capability")
    ContainmentStatus executeContainment(ResourceAllocation resourceAllocation,
                                         EvacuationOrder evacuationOrder);

    @Worker(capability = "assessDamage", cost = 0.2,
            description = "Post-containment damage assessment for recovery planning")
    @SystemPrompt("You are a damage assessment specialist. Survey the affected area and produce a structured damage report for recovery planning and insurance purposes.")
    @Audited
    DamageReport assessDamage(ContainmentStatus containmentStatus,
                              FireRiskAssessment fireRiskAssessment);

    @Goal(value = "Fire contained and damage assessed",
          condition = ".damageReport != null")
    @Completion
    default GoalExpression resolved() {
        return GoalExpression.goal("resolved");
    }

    record FireRiskAssessment(List<String> highRiskZones, String spreadDirection,
                               String windSpeed, String overallRisk) {}
    record ResourceAllocation(Map<String, String> crewAssignments,
                               List<String> assignedRoutes, String estimatedArrival) {}
    record EvacuationOrder(List<String> zones, String level, List<String> routes) {}
    record ContainmentStatus(String status, double percentContained,
                              List<String> activeFireLines) {}
    record DamageReport(String areaBurned, List<String> structuresDamaged,
                         String estimatedCost, String recoveryPriority) {}
}
```

### Blocks extension points

- `allocateResources` → `@VotingAgent` for multi-agency resource consensus
- `issueEvacuation` → `@OversightGate` with quorum from multiple agency commanders
- `assessRisk` → `@DebateAgent` between aggressive and conservative risk models
- Full response → `@HtnAgent` for multi-phase decomposition (assess → evacuate → contain → recover)

---

## Module Structure

Each new example module follows the established pattern:

```
examples/<name>/
    pom.xml
    src/main/java/io/casehub/examples/<CaseName>.java
    src/test/java/io/casehub/examples/<CaseName>Test.java
    src/test/resources/application.properties
```

All new modules added to root `pom.xml` `<modules>` and set `<maven.deploy.skip>true</maven.deploy.skip>`.

### pom.xml template (new cross-repo examples)

Dependencies vary per example — only include annotation modules that the example actually uses:

```xml
<!-- Engine annotations (all examples) -->
<dependency><groupId>io.casehub</groupId><artifactId>casehub-engine-annotations</artifactId></dependency>
<dependency><groupId>io.casehub</groupId><artifactId>casehub-engine-annotations-deployment</artifactId></dependency>

<!-- Eidos annotations (examples with @Identity/@Disposition) -->
<dependency><groupId>io.casehub</groupId><artifactId>casehub-eidos-annotations</artifactId></dependency>
<dependency><groupId>io.casehub</groupId><artifactId>casehub-eidos-annotations-deployment</artifactId></dependency>

<!-- Work annotations (examples with @HumanApproval/@Escalate) -->
<dependency><groupId>io.casehub</groupId><artifactId>casehub-work-annotations</artifactId></dependency>
<dependency><groupId>io.casehub</groupId><artifactId>casehub-work-annotations-deployment</artifactId></dependency>

<!-- Ledger annotations (examples with @Audited) -->
<dependency><groupId>io.casehub</groupId><artifactId>casehub-ledger-annotations</artifactId></dependency>
<dependency><groupId>io.casehub</groupId><artifactId>casehub-ledger-annotations-deployment</artifactId></dependency>
```

## Testing Strategy

Each example has a `@QuarkusTest` that:

1. Injects the generated `CaseDefinition` CDI bean
2. Verifies annotation-declared fields (namespace, name, workers, capabilities, goals, milestones)
3. Verifies cross-repo annotation processing (eidos `AgentDescriptor` generated, work `HumanTaskTarget` wired, ledger interceptor bound)
4. For GOAP examples: verifies inferred dependency chain produces the expected execution order
5. For `@SystemPrompt` workers: verifies `AgentWorkerFunction` generation (does NOT require a running LLM — asserts the function type, not execution)

Tests use `casehub-persistence-memory` and `quarkus.arc.selected-alternatives` for in-memory SPI implementations.

## CAPABILITY-MATRIX.md Update

After all examples are implemented, update `annotations/CAPABILITY-MATRIX.md`:

- Add columns for all 7 examples
- All 21 engine capabilities should now have at least one example check
- Add a new "Cross-Repo Composition" section tracking which upstream annotations are demonstrated where
- Coverage target: 21/21 engine capabilities in examples (was 17/21)

## References

- casehubio/engine#945 — this issue
- casehubio/eidos#141 — eidos annotation examples pattern (6 examples, CAPABILITY-MATRIX.md)
- casehubio/eidos#139 — eidos annotations module
- `annotations/CAPABILITY-MATRIX.md` — current coverage (17/21)
- `docs/specs/issue-909-engine-annotations/2026-08-16-annotation-driven-programming-model-design.md` — annotation architecture spec
- GE-20260813 — Quarkus `@Scheduled` duration format gotcha (relevant for cron `@Bind`)
- Engine root `pom.xml` — `version.io.casehub`, `version.io.casehub.work`, `version.io.casehub.ledger` properties
