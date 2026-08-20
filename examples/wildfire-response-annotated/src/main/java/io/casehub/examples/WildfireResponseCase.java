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
import io.casehub.eidos.annotations.AgentConstraintDef;
import io.casehub.eidos.annotations.AgentConstraints;
import io.casehub.eidos.annotations.AgentGoalDef;
import io.casehub.eidos.annotations.AgentGoals;
import io.casehub.eidos.annotations.Disposition;
import io.casehub.eidos.annotations.Identity;
import io.casehub.eidos.api.ConstraintSeverity;
import io.casehub.eidos.api.GoalPriority;
import io.casehub.engine.annotations.Capability;
import io.casehub.engine.annotations.Case;
import io.casehub.engine.annotations.Completion;
import io.casehub.engine.annotations.Goal;
import io.casehub.engine.annotations.Param;
import io.casehub.engine.annotations.PlanningMode;
import io.casehub.engine.annotations.SystemPrompt;
import io.casehub.engine.annotations.Worker;
import io.casehub.ledger.annotations.ActorId;
import io.casehub.ledger.annotations.Audited;
import io.casehub.ledger.annotations.ComplianceSupplement;
import io.casehub.ledger.annotations.SubjectId;
import io.casehub.work.annotations.Escalate;
import io.casehub.work.annotations.HumanApproval;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Identity(slot = "wildfire-commander", provider = "casehub", modelFamily = "claude")
@Disposition(
    socialOrient = "directive",
    ruleFollowing = "strict",
    riskAppetite = "cautious",
    autonomy = "bounded")
@AgentGoals({
  @AgentGoalDef(
      name = "protect-life",
      description = "Civilian and responder safety is the absolute priority",
      priority = GoalPriority.PRIMARY),
  @AgentGoalDef(
      name = "contain-spread",
      description = "Prevent fire from reaching populated areas",
      priority = GoalPriority.PRIMARY)
})
@AgentConstraints({
  @AgentConstraintDef(
      name = "responder-safety",
      description = "Never deploy ground crews into unassessed terrain",
      severity = ConstraintSeverity.HARD),
  @AgentConstraintDef(
      name = "air-tanker-limits",
      description = "No aerial drops in winds exceeding 35 knots",
      severity = ConstraintSeverity.HARD)
})
@Case(
    namespace = "disaster",
    name = "WildfireResponse",
    version = "1.0.0",
    title = "Wildfire Response",
    summary = "Coordinates wildfire detection, evacuation, and containment across agencies",
    planning = PlanningMode.GOAP)
public interface WildfireResponseCase {

  @Worker(
      capability = "assessFireRisk",
      cost = 0.1,
      description = "Analyses satellite imagery and weather data to assess fire spread risk")
  @SystemPrompt(
      "You are a wildfire risk analyst. Assess fire spread probability based on satellite imagery, wind patterns, terrain, and vegetation density. Classify risk zones.")
  @Audited
  FireRiskAssessment assessRisk(
      @Param("incidentId") @SubjectId UUID incidentId,
      @ActorId String analystId,
      String satelliteData,
      String weatherData);

  @Worker(
      capability = "allocateResources",
      cost = 0.3,
      description = "Assigns fire crews, air tankers, and equipment to sectors")
  @SystemPrompt(
      "You are a resource allocation coordinator. Assign available crews and equipment to fire sectors based on risk priority and resource proximity.")
  @Audited
  ResourceAllocation allocateResources(
      @Param("incidentId") @SubjectId UUID incidentId,
      @ActorId String coordinatorId,
      FireRiskAssessment fireRiskAssessment);

  @Worker(
      capability = "issueEvacuation",
      cost = 0.2,
      description = "Issues evacuation orders for at-risk communities")
  @HumanApproval(title = "Approve evacuation order", candidateGroups = "incident-commander")
  @Escalate(onExpiry = "state-emergency-director", deadline = "PT20M")
  @Audited
  @ComplianceSupplement(algorithmRef = "nims-evacuation-protocol", humanOverrideAvailable = true)
  default EvacuationOrder issueEvacuation(
      @Param("incidentId") @SubjectId UUID incidentId,
      @ActorId String commanderId,
      FireRiskAssessment fireRiskAssessment,
      ResourceAllocation resourceAllocation) {
    List<String> zones = fireRiskAssessment.highRiskZones();
    return new EvacuationOrder(zones, "mandatory", resourceAllocation.assignedRoutes());
  }

  @Capability(
      name = "groundContainment",
      description = "Ground crew containment operations — external field capability")
  ContainmentStatus executeContainment(
      ResourceAllocation resourceAllocation, EvacuationOrder evacuationOrder);

  @Worker(
      capability = "assessDamage",
      cost = 0.2,
      description = "Post-containment damage assessment for recovery planning")
  @SystemPrompt(
      "You are a damage assessment specialist. Survey the affected area and produce a structured damage report for recovery planning and insurance purposes.")
  @Audited
  DamageReport assessDamage(
      @Param("incidentId") @SubjectId UUID incidentId,
      @ActorId String assessorId,
      ContainmentStatus containmentStatus,
      FireRiskAssessment fireRiskAssessment);

  @Goal(value = "Fire contained and damage assessed", condition = ".damageReport != null")
  @Completion
  default GoalExpression resolved() {
    return GoalExpression.goal("resolved");
  }

  record FireRiskAssessment(
      List<String> highRiskZones, String spreadDirection, String windSpeed, String overallRisk) {}

  record ResourceAllocation(
      Map<String, String> crewAssignments, List<String> assignedRoutes, String estimatedArrival) {}

  record EvacuationOrder(List<String> zones, String level, List<String> routes) {}

  record ContainmentStatus(String status, double percentContained, List<String> activeFireLines) {}

  record DamageReport(
      String areaBurned,
      List<String> structuresDamaged,
      String estimatedCost,
      String recoveryPriority) {}
}
