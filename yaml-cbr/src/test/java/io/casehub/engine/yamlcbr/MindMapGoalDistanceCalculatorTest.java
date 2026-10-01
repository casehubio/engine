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

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.casehub.neocortex.cognitive.Confidence;
import io.casehub.neocortex.mindmap.EdgeInput;
import io.casehub.neocortex.mindmap.MergeResult;
import io.casehub.neocortex.mindmap.MindMapCapability;
import io.casehub.neocortex.mindmap.MindMapEdge;
import io.casehub.neocortex.mindmap.MindMapNode;
import io.casehub.neocortex.mindmap.MindMapQuery;
import io.casehub.neocortex.mindmap.MindMapStore;
import io.casehub.neocortex.mindmap.MindMapSubgraph;
import io.casehub.neocortex.mindmap.NodeInput;
import io.casehub.neocortex.mindmap.NodeRef;
import io.casehub.neocortex.mindmap.NodeUpdate;
import io.casehub.neocortex.mindmap.SubgraphInput;
import io.casehub.neocortex.mindmap.SubgraphTypes;
import io.casehub.neocortex.mindmap.SupersessionStatus;
import io.casehub.neocortex.mindmap.ValidationTier;
import io.casehub.platform.api.identity.PrincipalId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MindMapGoalDistanceCalculatorTest {

  @Test
  void returns1_0WhenNoActiveGoals() {
    var store = new GraphBuilder().build();
    var calc = new MindMapGoalDistanceCalculator(store);
    assertEquals(1.0, calc.boost("entity", "t"), 0.01);
  }

  @Test
  void returns1_0WhenEntityNotFound() {
    var store = new GraphBuilder().goalNode("goal-1", "active").build();
    var calc = new MindMapGoalDistanceCalculator(store);
    assertEquals(1.0, calc.boost("nonexistent", "t"), 0.01);
  }

  @Test
  void returns1_0ForDirectGoalNode() {
    var store = new GraphBuilder().goalNode("entity", "active").build();
    var calc = new MindMapGoalDistanceCalculator(store);
    assertEquals(1.0, calc.boost("entity", "t"), 0.01);
  }

  @Test
  void returns1_0ForDistance1() {
    var store =
        new GraphBuilder()
            .goalNode("goal-1", "active")
            .domainNode("entity")
            .edge("entity", "goal-1")
            .build();
    var calc = new MindMapGoalDistanceCalculator(store);
    assertEquals(1.0, calc.boost("entity", "t"), 0.01);
  }

  @Test
  void returns0_7ForDistance2() {
    var store =
        new GraphBuilder()
            .goalNode("goal-1", "active")
            .domainNode("mid")
            .domainNode("entity")
            .edge("entity", "mid")
            .edge("mid", "goal-1")
            .build();
    var calc = new MindMapGoalDistanceCalculator(store);
    assertEquals(0.7, calc.boost("entity", "t"), 0.01);
  }

  @Test
  void returns0_4ForDistance3() {
    var store =
        new GraphBuilder()
            .goalNode("goal-1", "active")
            .domainNode("mid1")
            .domainNode("mid2")
            .domainNode("entity")
            .edge("entity", "mid1")
            .edge("mid1", "mid2")
            .edge("mid2", "goal-1")
            .build();
    var calc = new MindMapGoalDistanceCalculator(store);
    assertEquals(0.4, calc.boost("entity", "t"), 0.01);
  }

  @Test
  void returns0_0ForDistanceBeyondMax() {
    var store =
        new GraphBuilder()
            .goalNode("goal-1", "active")
            .domainNode("m1")
            .domainNode("m2")
            .domainNode("m3")
            .domainNode("m4")
            .domainNode("entity")
            .edge("entity", "m1")
            .edge("m1", "m2")
            .edge("m2", "m3")
            .edge("m3", "m4")
            .edge("m4", "goal-1")
            .build();
    var calc = new MindMapGoalDistanceCalculator(store);
    assertEquals(0.0, calc.boost("entity", "t"), 0.01);
  }

  @Test
  void returns1_0WhenStoreThrows() {
    MindMapStore store = new ThrowingStore();
    var calc = new MindMapGoalDistanceCalculator(store);
    assertEquals(1.0, calc.boost("entity", "t"), 0.01);
  }

  // --- Graph Builder ---

  static class GraphBuilder {
    private final List<StubNode> goalNodes = new ArrayList<>();
    private final List<StubNode> domainNodes = new ArrayList<>();
    private final List<StubEdge> edges = new ArrayList<>();

    GraphBuilder goalNode(String name, String status) {
      goalNodes.add(new StubNode(name, name, SubgraphTypes.GOAL, Map.of("status", status)));
      return this;
    }

    GraphBuilder domainNode(String name) {
      domainNodes.add(new StubNode(name, name, SubgraphTypes.GENERAL, Map.of()));
      return this;
    }

    GraphBuilder edge(String source, String target) {
      edges.add(new StubEdge("e-" + source + "-" + target, source, target));
      return this;
    }

    StubMindMapStore build() {
      return new StubMindMapStore(goalNodes, domainNodes, edges);
    }
  }

  // --- Stubs ---

  record StubNode(String id, String name, String subgraphType, Map<String, String> props)
      implements MindMapNode {
    @Override
    public Confidence confidence() {
      return null;
    }

    @Override
    public String provenance() {
      return null;
    }

    @Override
    public Instant createdAt() {
      return Instant.now();
    }

    @Override
    public Instant updatedAt() {
      return Instant.now();
    }

    @Override
    public Instant validFrom() {
      return null;
    }

    @Override
    public Instant validUntil() {
      return null;
    }

    @Override
    public Set<String> traits() {
      return Set.of();
    }

    @Override
    public Set<NodeRef> refs() {
      return Set.of();
    }

    @Override
    public Double pleasure() {
      return null;
    }

    @Override
    public Double arousal() {
      return null;
    }

    @Override
    public Double dominance() {
      return null;
    }

    @Override
    public Optional<String> property(String key) {
      return Optional.ofNullable(props.get(key));
    }

    @Override
    public Map<String, String> properties() {
      return props;
    }

    @Override
    public String subgraphId() {
      return subgraphType;
    }

    @Override
    public PrincipalId principalId() {
      return null;
    }

    @Override
    public Set<String> sharedWith() {
      return Set.of();
    }
  }

  record StubEdge(String id, String sourceNodeId, String targetNodeId) implements MindMapEdge {
    @Override
    public String edgeType() {
      return "related";
    }

    @Override
    public ValidationTier tier() {
      return null;
    }

    @Override
    public Confidence confidence() {
      return null;
    }

    @Override
    public String provenance() {
      return null;
    }

    @Override
    public Instant createdAt() {
      return Instant.now();
    }

    @Override
    public Instant updatedAt() {
      return Instant.now();
    }

    @Override
    public Instant validFrom() {
      return null;
    }

    @Override
    public Instant validUntil() {
      return null;
    }

    @Override
    public Double pleasure() {
      return null;
    }

    @Override
    public Double arousal() {
      return null;
    }

    @Override
    public Double dominance() {
      return null;
    }

    @Override
    public Optional<String> property(String key) {
      return Optional.empty();
    }

    @Override
    public Map<String, String> properties() {
      return Map.of();
    }
  }

  static class StubMindMapStore implements MindMapStore {
    private final List<StubNode> goalNodes;
    private final List<StubNode> domainNodes;
    private final List<StubEdge> edges;
    private final Map<String, StubNode> allNodes = new HashMap<>();

    StubMindMapStore(List<StubNode> goalNodes, List<StubNode> domainNodes, List<StubEdge> edges) {
      this.goalNodes = goalNodes;
      this.domainNodes = domainNodes;
      this.edges = edges;
      goalNodes.forEach(n -> allNodes.put(n.id(), n));
      domainNodes.forEach(n -> allNodes.put(n.id(), n));
    }

    @Override
    public List<MindMapSubgraph> listSubgraphs(String tenantId) {
      List<MindMapSubgraph> sgs = new ArrayList<>();
      if (!goalNodes.isEmpty())
        sgs.add(
            new MindMapSubgraph(
                "sg-goal", "Goals", SubgraphTypes.GOAL, null, tenantId, Instant.now()));
      if (!domainNodes.isEmpty())
        sgs.add(
            new MindMapSubgraph(
                "sg-domain", "Domain", SubgraphTypes.GENERAL, null, tenantId, Instant.now()));
      return sgs;
    }

    @Override
    public List<MindMapNode> nodesIn(String subgraphId, String tenantId) {
      if ("sg-goal".equals(subgraphId)) return List.copyOf(goalNodes);
      if ("sg-domain".equals(subgraphId)) return List.copyOf(domainNodes);
      return List.of();
    }

    @Override
    public MindMapNode resolveNode(String nameOrAlias, String subgraphId, String tenantId) {
      return allNodes.get(nameOrAlias);
    }

    @Override
    public List<MindMapEdge> neighbors(String nodeId, String tenantId, PrincipalId p) {
      return edges.stream()
          .filter(e -> e.sourceNodeId().equals(nodeId) || e.targetNodeId().equals(nodeId))
          .map(e -> (MindMapEdge) e)
          .toList();
    }

    // --- Unused methods ---
    @Override
    public void registerVocabulary(io.casehub.neocortex.mindmap.MindMapVocabulary v) {}

    @Override
    public String addNode(NodeInput i, String t) {
      return null;
    }

    @Override
    public MindMapNode getNode(String id, String t) {
      return allNodes.get(id);
    }

    @Override
    public void updateNode(String id, NodeUpdate u, String t) {}

    @Override
    public String addEdge(EdgeInput i, String t) {
      return null;
    }

    @Override
    public MindMapEdge getEdge(String id, String t) {
      return null;
    }

    @Override
    public void removeEdge(String id, String t) {}

    @Override
    public void addAlias(String nid, String a, String t) {}

    @Override
    public void removeAlias(String nid, String a, String t) {}

    @Override
    public MergeResult mergeNodes(String k, String r, String t) {
      return null;
    }

    @Override
    public String createSubgraph(SubgraphInput i, String t) {
      return null;
    }

    @Override
    public MindMapSubgraph getSubgraph(String id, String t) {
      return null;
    }

    @Override
    public void updateSubgraph(String id, String rn, String t) {}

    @Override
    public List<MindMapEdge> bridgeEdges(String sg, String t, PrincipalId p) {
      return List.of();
    }

    @Override
    public List<MindMapEdge> neighbors(String nid, String et, String t, PrincipalId p) {
      return List.of();
    }

    @Override
    public List<MindMapNode> search(MindMapQuery q) {
      return List.of();
    }

    @Override
    public void supersede(String tid, String sid, String r, String t) {}

    @Override
    public void reinstate(String tid, String t) {}

    @Override
    public SupersessionStatus getSupersessionStatus(String tid, String t) {
      return null;
    }

    @Override
    public int eraseNode(String nid, String t) {
      return 0;
    }

    @Override
    public int eraseSubgraph(String sid, String t) {
      return 0;
    }

    @Override
    public int eraseEntity(String en, String t) {
      return 0;
    }

    @Override
    public int eraseEntityAcrossTenants(String en, Set<String> t) {
      return 0;
    }

    @Override
    public Set<MindMapCapability> capabilities() {
      return Set.of();
    }
  }

  static class ThrowingStore extends StubMindMapStore {
    ThrowingStore() {
      super(List.of(), List.of(), List.of());
    }

    @Override
    public List<MindMapSubgraph> listSubgraphs(String t) {
      throw new RuntimeException("store down");
    }
  }
}
