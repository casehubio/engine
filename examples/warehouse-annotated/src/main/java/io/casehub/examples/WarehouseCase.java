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
import io.casehub.engine.annotations.Case;
import io.casehub.engine.annotations.Completion;
import io.casehub.engine.annotations.Goal;
import io.casehub.engine.annotations.Param;
import io.casehub.engine.annotations.PlanningMode;
import io.casehub.engine.annotations.SoftDependency;
import io.casehub.engine.annotations.SystemPrompt;
import io.casehub.engine.annotations.Worker;
import io.casehub.ledger.annotations.ActorId;
import io.casehub.ledger.annotations.Audited;
import io.casehub.ledger.annotations.SubjectId;
import io.casehub.work.annotations.HumanApproval;
import java.util.List;
import java.util.UUID;

@Identity(slot = "warehouse-optimizer", provider = "casehub", modelFamily = "claude")
@Disposition(socialOrient = "efficient", ruleFollowing = "flexible", riskAppetite = "moderate")
@Case(
    namespace = "logistics",
    name = "WarehouseFulfillment",
    version = "1.0.0",
    title = "Warehouse Fulfillment",
    summary = "Optimises order picking, packing, and dispatch in an automated warehouse",
    planning = PlanningMode.GOAP)
public interface WarehouseCase {

  @Worker(
      capability = "planRoute",
      cost = 0.2,
      description = "Optimises pick route through warehouse zones")
  @SystemPrompt(
      "You are a warehouse route optimizer. Given the order items and warehouse layout, plan the most efficient pick route minimising travel distance and zone transitions.")
  @Audited
  PickRoute planRoute(
      @Param("orderId") @SubjectId UUID orderId,
      @ActorId String operatorId,
      String warehouseLayout);

  @Worker(capability = "pickItems", cost = 0.4, description = "Picks items along the planned route")
  @Audited
  default PickResult pickItems(
      @Param("orderId") @SubjectId UUID orderId, @ActorId String pickerId, PickRoute pickRoute) {
    return new PickResult(pickRoute.items(), pickRoute.zones(), "picked");
  }

  @Worker(
      capability = "qualityCheck",
      cost = 0.2,
      description = "Inspects picked items for damage and correctness")
  @SystemPrompt(
      "You are a quality inspector. Verify picked items match the order and check for damage.")
  @Audited
  QualityReport qualityCheck(
      @Param("orderId") @SubjectId UUID orderId,
      @ActorId String inspectorId,
      PickResult pickResult);

  @Worker(
      capability = "handleHazmat",
      cost = 0.3,
      description = "Special handling for hazardous materials")
  @HumanApproval(title = "Approve hazmat handling procedure", candidateGroups = "hazmat-certified")
  @Audited(auditFailures = true)
  default HazmatClearance handleHazmat(
      @Param("orderId") @SubjectId UUID orderId,
      @ActorId String hazmatOfficerId,
      PickResult pickResult,
      QualityReport qualityReport) {
    return new HazmatClearance("cleared", qualityReport.hazmatItems());
  }

  @Worker(
      capability = "packAndDispatch",
      cost = 0.2,
      description = "Packs items and generates shipping label")
  @Audited
  default DispatchConfirmation packAndDispatch(
      @Param("orderId") @SubjectId UUID orderId,
      @ActorId String packerId,
      QualityReport qualityReport,
      @SoftDependency HazmatClearance hazmatClearance) {
    String hazmatStatus = hazmatClearance != null ? hazmatClearance.status() : "not-applicable";
    return new DispatchConfirmation(
        "SHIP-" + orderId.toString().substring(0, 8), qualityReport.verifiedItems(), hazmatStatus);
  }

  @Goal(value = "Order dispatched", condition = ".dispatchConfirmation != null")
  @Completion
  default GoalExpression dispatched() {
    return GoalExpression.goal("dispatched");
  }

  record PickRoute(List<String> items, List<String> zones, String estimatedTime) {}

  record PickResult(List<String> pickedItems, List<String> visitedZones, String status) {}

  record QualityReport(List<String> verifiedItems, List<String> hazmatItems, boolean allCorrect) {}

  record HazmatClearance(String status, List<String> clearedItems) {}

  record DispatchConfirmation(String shipmentId, List<String> items, String hazmatStatus) {}
}
