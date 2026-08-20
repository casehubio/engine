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
import io.casehub.eidos.annotations.Disposition;
import io.casehub.eidos.annotations.Identity;
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
import io.casehub.work.annotations.RequiresQuorum;
import java.util.List;
import java.util.UUID;

@Identity(
    slot = "maintenance-engineer",
    provider = "casehub",
    modelFamily = "claude",
    jurisdiction = "EU")
@Disposition(socialOrient = "methodical", ruleFollowing = "strict", riskAppetite = "risk-averse")
@Case(
    namespace = "aviation",
    name = "AircraftMaintenance",
    version = "1.0.0",
    title = "Aircraft Maintenance",
    summary = "Manages aircraft defect assessment, repair, and airworthiness certification")
public interface AircraftMaintenanceCase {

  @Worker(
      capability = "assessDefect",
      description = "Classifies defect severity per MEL/CDL categories")
  @Bind(contextChange = ".defectReport != null")
  @Audited
  @ComplianceSupplement(algorithmRef = "easa-part-145-defect-classification")
  @SystemPrompt(
      "You are an aircraft maintenance engineer. Classify the defect severity per EASA Part-145 MEL/CDL categories. Be conservative — when uncertain, classify higher.")
  DefectAssessment assessDefect(
      @SubjectId UUID workOrderId,
      @ActorId String engineerId,
      String defectReport,
      String aircraftType);

  @Worker(
      capability = "planRepair",
      description = "Creates repair plan with required parts and procedures")
  @Bind(contextChange = ".defectAssessment != null")
  @Audited
  @SystemPrompt(
      "You are a repair planning specialist. Create a detailed repair plan following the aircraft maintenance manual. List required parts, tools, and estimated time.")
  RepairPlan planRepair(
      @SubjectId UUID workOrderId,
      @ActorId String plannerId,
      DefectAssessment defectAssessment,
      String aircraftType);

  @Worker(
      capability = "approveParts",
      description = "Validates parts availability and authorises procurement")
  @Bind(contextChange = ".repairPlan != null", when = ".repairPlan.partsRequired | length > 0")
  @HumanApproval(title = "Approve parts procurement", candidateGroups = "parts-authority")
  @Audited
  default PartsApproval approveParts(
      @SubjectId UUID workOrderId, @ActorId String authorityId, RepairPlan repairPlan) {
    return new PartsApproval(
        repairPlan.partsRequired(), "approved", java.time.Instant.now().toString());
  }

  @Worker(
      capability = "executeRepair",
      description = "Executes repair procedures and records work performed")
  @Bind(contextChange = ".partsApproval != null", when = ".partsApproval.status == \"approved\"")
  @Audited(auditFailures = true)
  default RepairRecord executeRepair(
      @SubjectId UUID workOrderId,
      @ActorId String technicianId,
      RepairPlan repairPlan,
      PartsApproval partsApproval) {
    return new RepairRecord(repairPlan.procedures(), "completed", partsApproval.approvedParts());
  }

  @Worker(
      capability = "certifyAirworthy",
      description = "Dual-inspector airworthiness certification")
  @Bind(contextChange = ".repairRecord != null", when = ".repairRecord.status == \"completed\"")
  @HumanApproval(title = "Certify aircraft airworthy", candidateGroups = "licensed-inspectors")
  @RequiresQuorum(instances = 2, required = 2, candidateGroups = "licensed-inspectors")
  @Escalate(onExpiry = "chief-inspector", deadline = "PT4H")
  @Audited
  @ComplianceSupplement(
      algorithmRef = "easa-part-145-release-to-service",
      humanOverrideAvailable = false)
  default AirworthinessCertification certify(
      @SubjectId UUID workOrderId,
      @ActorId String inspectorId,
      RepairRecord repairRecord,
      DefectAssessment defectAssessment) {
    return new AirworthinessCertification(
        defectAssessment.defectId(),
        "CRS-" + workOrderId.toString().substring(0, 8),
        "fit-for-service");
  }

  @Milestone(
      name = "repairComplete",
      entryCriteria = ".repairPlan != null",
      completionCriteria = ".repairRecord.status == \"completed\"")
  default void repairComplete() {}

  @Goal(value = "Aircraft certified airworthy", condition = ".airworthinessCertification != null")
  @Completion
  default GoalExpression certified() {
    return GoalExpression.goal("certified");
  }

  record DefectAssessment(
      String defectId, String severity, String melCategory, List<String> affectedSystems) {}

  record RepairPlan(List<String> procedures, List<String> partsRequired, String estimatedTime) {}

  record PartsApproval(List<String> approvedParts, String status, String timestamp) {}

  record RepairRecord(List<String> proceduresPerformed, String status, List<String> partsUsed) {}

  record AirworthinessCertification(String defectId, String certificateNumber, String verdict) {}
}
