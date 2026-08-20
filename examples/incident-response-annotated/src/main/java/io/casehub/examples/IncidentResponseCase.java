/*
 * Copyright 2026-Present The Case Hub Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.casehub.examples;

import io.casehub.api.model.GoalExpression;
import io.casehub.eidos.annotations.AgentGoalDef;
import io.casehub.eidos.annotations.AgentGoals;
import io.casehub.eidos.annotations.Disposition;
import io.casehub.eidos.annotations.Identity;
import io.casehub.eidos.api.GoalPriority;
import io.casehub.engine.annotations.Bind;
import io.casehub.engine.annotations.Case;
import io.casehub.engine.annotations.Completion;
import io.casehub.engine.annotations.Goal;
import io.casehub.engine.annotations.Milestone;
import io.casehub.engine.annotations.SystemPrompt;
import io.casehub.engine.annotations.Worker;
import io.casehub.ledger.annotations.ActorId;
import io.casehub.ledger.annotations.Audited;
import io.casehub.ledger.annotations.ComplianceSupplement;
import io.casehub.ledger.annotations.SubjectId;
import io.casehub.work.annotations.Escalate;
import io.casehub.work.annotations.HumanApproval;
import java.util.List;
import java.util.UUID;

@Identity(
    slot = "security-analyst",
    provider = "casehub",
    modelFamily = "claude",
    jurisdiction = "US")
@Disposition(
    socialOrient = "independent",
    ruleFollowing = "strict",
    riskAppetite = "cautious",
    autonomy = "bounded")
@AgentGoals({
  @AgentGoalDef(
      name = "minimise-blast-radius",
      description = "Contain threats before they spread",
      priority = GoalPriority.PRIMARY),
  @AgentGoalDef(
      name = "preserve-evidence",
      description = "Maintain forensic chain of custody",
      priority = GoalPriority.SECONDARY)
})
@Case(
    namespace = "security",
    name = "IncidentResponse",
    version = "1.0.0",
    title = "Incident Response",
    summary = "Detects, triages, contains, and remediates cybersecurity incidents")
public interface IncidentResponseCase {

  @Worker(
      capability = "scanForAnomalies",
      description = "Periodic security scan for anomalous activity")
  @Bind(cron = "0 */15 * * * ?")
  @Bind(contextChange = ".newAlert != null")
  @SystemPrompt(
      "You are a security monitoring agent. Analyse system logs and network traffic for anomalies. Return structured findings.")
  ScanResult scan();

  @Worker(
      capability = "triageSeverity",
      description = "Classifies incident severity: P1-critical, P2-high, P3-medium, P4-low")
  @Bind(contextChange = ".scanResult != null", when = ".scanResult.anomalyDetected == true")
  @Audited
  @SystemPrompt(
      "You are a security triage specialist. Given scan findings, classify the severity and recommend immediate actions.")
  TriageAssessment triage(
      @SubjectId UUID incidentId, @ActorId String analystId, ScanResult scanResult);

  @Worker(
      capability = "containThreat",
      description = "Executes containment: isolate host, block IP, revoke credentials")
  @Bind(
      contextChange = ".triageAssessment != null",
      when = ".triageAssessment.severity == \"P1\" or .triageAssessment.severity == \"P2\"")
  @HumanApproval(
      title = "Approve containment action",
      candidateGroups = "security-ops",
      claimDeadline = "PT10M")
  @Escalate(onExpiry = "security-director", deadline = "PT30M")
  @Audited(auditFailures = true)
  default ContainmentResult contain(
      @SubjectId UUID incidentId, @ActorId String operatorId, TriageAssessment triageAssessment) {
    return new ContainmentResult(
        triageAssessment.recommendedActions(), "contained", java.time.Instant.now().toString());
  }

  @Worker(
      capability = "remediate",
      description = "Applies fixes: patch, config change, credential rotation")
  @Bind(
      contextChange = ".containmentResult != null",
      when = ".containmentResult.status == \"contained\"")
  @Audited
  @SystemPrompt(
      "You are a remediation engineer. Given the containment results, propose and execute remediation steps.")
  RemediationReport remediate(
      @SubjectId UUID incidentId,
      @ActorId String engineerId,
      ContainmentResult containmentResult,
      TriageAssessment triageAssessment);

  @Worker(
      capability = "generateComplianceReport",
      description = "Produces incident report for regulatory compliance")
  @Bind(contextChange = ".remediationReport != null")
  @Audited
  @ComplianceSupplement(algorithmRef = "incident-response-v2", humanOverrideAvailable = true)
  @SystemPrompt(
      "You are a compliance reporting agent. Compile all incident data into a structured compliance report following NIST SP 800-61 format.")
  ComplianceReport report(
      @SubjectId UUID incidentId,
      @ActorId String reporterId,
      ScanResult scanResult,
      TriageAssessment triageAssessment,
      ContainmentResult containmentResult,
      RemediationReport remediationReport);

  @Milestone(
      name = "threatContained",
      entryCriteria = ".triageAssessment != null",
      completionCriteria = ".containmentResult.status == \"contained\"")
  default void threatContained() {}

  @Goal(value = "Incident fully resolved and reported", condition = ".complianceReport != null")
  @Completion
  default GoalExpression resolved() {
    return GoalExpression.goal("resolved");
  }

  record ScanResult(boolean anomalyDetected, String finding, String source, String severity) {}

  record TriageAssessment(
      String severity, String category, List<String> recommendedActions, String justification) {}

  record ContainmentResult(List<String> actionsApplied, String status, String timestamp) {}

  record RemediationReport(List<String> fixes, String status, String verificationResult) {}

  record ComplianceReport(
      String incidentId,
      String nistCategory,
      String timeline,
      String impact,
      String lessonsLearned) {}
}
