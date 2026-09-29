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
package io.casehub.engine.yamlcbr;

import io.casehub.neocortex.mindmap.MindMapEdge;
import io.casehub.neocortex.mindmap.MindMapNode;
import io.casehub.neocortex.mindmap.MindMapStore;
import io.casehub.neocortex.mindmap.MindMapSubgraph;
import io.casehub.neocortex.mindmap.SubgraphTypes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jboss.logging.Logger;

public class MindMapGoalDistanceCalculator {

  private static final Logger LOG = Logger.getLogger(MindMapGoalDistanceCalculator.class);
  private static final int MAX_DEPTH = 4;

  private final MindMapStore store;

  public MindMapGoalDistanceCalculator(MindMapStore store) {
    this.store = store;
  }

  public double boost(String entityName, String tenancyId) {
    try {
      return computeBoost(entityName, tenancyId);
    } catch (Exception e) {
      LOG.warnf(e, "MindMap distance calculation failed — returning neutral boost");
      return 1.0;
    }
  }

  private double computeBoost(String entityName, String tenancyId) {
    List<MindMapNode> activeGoals = findActiveGoals(tenancyId);
    if (activeGoals.isEmpty()) {
      return 1.0;
    }

    MindMapNode entityNode = findEntityNode(entityName, tenancyId);
    if (entityNode == null) {
      return 1.0;
    }

    int distance = shortestPathToGoal(entityNode, activeGoals, tenancyId);
    if (distance < 0) return 0.0;
    if (distance <= 1) return 1.0;
    if (distance == 2) return 0.7;
    if (distance == 3) return 0.4;
    return 0.0;
  }

  private List<MindMapNode> findActiveGoals(String tenancyId) {
    String goalSgId = null;
    for (MindMapSubgraph sg : store.listSubgraphs(tenancyId)) {
      if (SubgraphTypes.GOAL.equals(sg.type())) {
        goalSgId = sg.id();
        break;
      }
    }
    if (goalSgId == null) return List.of();

    return store.nodesIn(goalSgId, tenancyId).stream()
        .filter(n -> "active".equals(n.property("status").orElse(null)))
        .toList();
  }

  private MindMapNode findEntityNode(String entityName, String tenancyId) {
    for (MindMapSubgraph sg : store.listSubgraphs(tenancyId)) {
      if (SubgraphTypes.GOAL.equals(sg.type())) continue;
      MindMapNode resolved = store.resolveNode(entityName, sg.id(), tenancyId);
      if (resolved != null) return resolved;
    }
    return null;
  }

  private int shortestPathToGoal(
      MindMapNode entityNode, List<MindMapNode> goals, String tenancyId) {
    Set<String> goalIds = new HashSet<>();
    for (MindMapNode g : goals) goalIds.add(g.id());

    if (goalIds.contains(entityNode.id())) return 0;

    Set<String> visited = new HashSet<>();
    visited.add(entityNode.id());
    Set<String> currentLevel = Set.of(entityNode.id());

    for (int depth = 1; depth <= MAX_DEPTH; depth++) {
      Set<String> nextLevel = new HashSet<>();
      for (String nodeId : currentLevel) {
        List<MindMapEdge> edges = store.neighbors(nodeId, tenancyId);
        for (MindMapEdge edge : edges) {
          String neighbor =
              edge.sourceNodeId().equals(nodeId) ? edge.targetNodeId() : edge.sourceNodeId();
          if (goalIds.contains(neighbor)) return depth;
          if (visited.add(neighbor)) nextLevel.add(neighbor);
        }
      }
      currentLevel = nextLevel;
      if (currentLevel.isEmpty()) break;
    }
    return -1;
  }
}
