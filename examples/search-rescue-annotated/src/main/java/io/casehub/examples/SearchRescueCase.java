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
import io.casehub.ledger.annotations.SubjectId;
import io.casehub.work.annotations.Escalate;
import io.casehub.work.annotations.HumanApproval;
import java.util.List;
import java.util.UUID;

@Identity(slot = "rescue-coordinator", provider = "casehub", modelFamily = "claude")
@Disposition(socialOrient = "directive", ruleFollowing = "strict", riskAppetite = "cautious")
@AgentGoals({
  @AgentGoalDef(
      name = "locate-and-extract",
      description = "Find and safely extract the missing person",
      priority = GoalPriority.PRIMARY),
  @AgentGoalDef(
      name = "responder-safety",
      description = "No responder injuries during the operation",
      priority = GoalPriority.PRIMARY)
})
@AgentConstraints({
  @AgentConstraintDef(
      name = "weather-limits",
      description = "Do not deploy air assets in winds exceeding 40 knots",
      severity = ConstraintSeverity.HARD),
  @AgentConstraintDef(
      name = "daylight-ops",
      description = "Ground search only during daylight unless equipped for night ops",
      severity = ConstraintSeverity.SOFT)
})
@Case(
    namespace = "emergency",
    name = "SearchAndRescue",
    version = "1.0.0",
    title = "Search and Rescue",
    summary = "Coordinates search and rescue operations for missing persons",
    planning = PlanningMode.GOAP)
public interface SearchRescueCase {

  @Worker(
      capability = "assessConditions",
      cost = 0.1,
      description = "Evaluates weather, terrain, and last-known-position data")
  @SystemPrompt(
      "You are a search operations planner. Assess conditions and recommend search strategy.")
  @Audited
  ConditionAssessment assessConditions(
      @Param("missionId") @SubjectId UUID missionId,
      @ActorId String coordinatorId,
      String lastKnownPosition,
      String terrain);

  @Worker(
      capability = "deployDrones",
      cost = 0.3,
      description = "Plans and deploys drone search grid")
  @SystemPrompt(
      "You are a drone operations coordinator. Plan optimal search grid based on conditions.")
  @Audited
  DroneSearchResult deployDrones(
      @Param("missionId") @SubjectId UUID missionId,
      @ActorId String pilotId,
      ConditionAssessment conditionAssessment);

  @Worker(
      capability = "assessMedical",
      cost = 0.2,
      description = "Remote medical assessment from drone imagery and vitals")
  @SystemPrompt(
      "You are a remote medical assessor. Evaluate survivor condition from available data.")
  @Audited
  MedicalAssessment assessMedical(
      @Param("missionId") @SubjectId UUID missionId,
      @ActorId String medicId,
      DroneSearchResult droneSearchResult);

  @Capability(
      name = "fieldRescueTeam",
      description = "On-ground rescue team — external capability, not an AI agent")
  RescueResult dispatchRescue(
      MedicalAssessment medicalAssessment, ConditionAssessment conditionAssessment);

  @Worker(
      capability = "authoriseEvacuation",
      cost = 0.1,
      description = "Authorises evacuation method based on medical and terrain data")
  @HumanApproval(title = "Authorise evacuation plan", candidateGroups = "incident-commander")
  @Escalate(onExpiry = "regional-coordinator", deadline = "PT15M")
  @Audited
  default EvacuationPlan authoriseEvacuation(
      @Param("missionId") @SubjectId UUID missionId,
      @ActorId String commanderId,
      MedicalAssessment medicalAssessment,
      RescueResult rescueResult) {
    String method = medicalAssessment.criticalCondition() ? "helicopter" : "ground";
    return new EvacuationPlan(method, rescueResult.location(), medicalAssessment.priority());
  }

  @Goal(value = "Person located and evacuated", condition = ".evacuationPlan != null")
  @Completion
  default GoalExpression rescued() {
    return GoalExpression.goal("rescued");
  }

  record ConditionAssessment(
      String weather, String terrainDifficulty, String searchStrategy, boolean safeForAir) {}

  record DroneSearchResult(boolean personLocated, String location, String imagery) {}

  record MedicalAssessment(
      boolean criticalCondition, String priority, List<String> observedConditions) {}

  record RescueResult(String status, String location, String teamId) {}

  record EvacuationPlan(String method, String destination, String priority) {}
}
