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
package io.casehub.engine.runtime.stigmergy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.casehub.api.model.CaseChannel;
import io.casehub.api.model.stigmergy.AgentLifecycleState;
import io.casehub.api.model.stigmergy.AgentWorkState;
import io.casehub.api.model.stigmergy.CoordinationConfig;
import io.casehub.api.model.stigmergy.StigmergyConfig;
import io.casehub.api.spi.PostRequest;
import io.casehub.api.spi.stigmergy.AgentWorkLifecycleListener;
import io.casehub.engine.common.internal.convergence.ActivityTracker;
import io.casehub.engine.common.internal.observation.ObservationRegistry;
import io.casehub.engine.common.internal.signal.SignalRegistry;
import io.casehub.engine.runtime.worker.NoOpCaseChannelProvider;
import io.casehub.qhorus.api.message.MessageType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StigmergyCoordinatorTest {

  private StigmergyCoordinator coordinator;
  private SignalRegistry signalRegistry;
  private ObservationRegistry observationRegistry;
  private ActivityTracker activityTracker;
  private UUID caseId;
  private StigmergyConfig config;

  @BeforeEach
  void setUp() {
    signalRegistry = new SignalRegistry();
    observationRegistry = new ObservationRegistry();
    activityTracker = new ActivityTracker();
    coordinator = new StigmergyCoordinator(signalRegistry, observationRegistry, activityTracker);
    caseId = UUID.randomUUID();
    config = new StigmergyConfig(null, new CoordinationConfig(2, 10.0, 0.6), null);
  }

  @Test
  void notStigmergyCaseBeforeInit() {
    assertFalse(coordinator.isStigmergyCase(caseId));
  }

  @Test
  void isStigmergyCaseAfterInit() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test-tenant");
    assertTrue(coordinator.isStigmergyCase(caseId));
  }

  @Test
  void agentJoinedTransitionsToJoining() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test-tenant");
    coordinator.agentJoined(caseId, "agent-1", "binding-1");
    var agents = coordinator.activeAgents(caseId);
    assertTrue(agents.isEmpty());
    var agent = coordinator.getAgent(caseId, "agent-1");
    assertNotNull(agent);
    assertEquals(AgentLifecycleState.JOINING, agent.state());
  }

  @Test
  void agentActivatedTransitionsToActive() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test-tenant");
    coordinator.agentJoined(caseId, "agent-1", "binding-1");
    coordinator.agentActivated(caseId, "agent-1");
    var agents = coordinator.activeAgents(caseId);
    assertEquals(1, agents.size());
    assertEquals(AgentLifecycleState.ACTIVE, agents.get(0).state());
  }

  @Test
  void agentDepartedTransitionsToDeparted() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test-tenant");
    coordinator.agentJoined(caseId, "agent-1", "binding-1");
    coordinator.agentActivated(caseId, "agent-1");
    coordinator.agentDeparted(caseId, "agent-1");
    var agents = coordinator.activeAgents(caseId);
    assertTrue(agents.isEmpty());
    var agent = coordinator.getAgent(caseId, "agent-1");
    assertEquals(AgentLifecycleState.DEPARTED, agent.state());
  }

  @Test
  void allActiveReturnsTrueWhenAllPastJoining() {
    coordinator.initializeCase(caseId, List.of("a1", "a2"), config, "test-tenant");
    coordinator.agentJoined(caseId, "a1", "b1");
    coordinator.agentJoined(caseId, "a2", "b2");
    assertFalse(coordinator.allActive(caseId));
    coordinator.agentActivated(caseId, "a1");
    assertFalse(coordinator.allActive(caseId));
    coordinator.agentActivated(caseId, "a2");
    assertTrue(coordinator.allActive(caseId));
  }

  @Test
  void evictByCaseClearsAll() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test-tenant");
    coordinator.agentJoined(caseId, "agent-1", "binding-1");
    coordinator.evictByCase(caseId);
    assertFalse(coordinator.isStigmergyCase(caseId));
  }

  @Test
  void resetClearsAll() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test-tenant");
    coordinator.reset();
    assertFalse(coordinator.isStigmergyCase(caseId));
  }

  @Test
  void detectsSignalConsensus() {
    coordinator.initializeCase(caseId, List.of("a1", "a2"), config, "test-tenant");
    coordinator.agentJoined(caseId, "a1", "b1");
    coordinator.agentJoined(caseId, "a2", "b2");
    coordinator.agentActivated(caseId, "a1");
    coordinator.agentActivated(caseId, "a2");
    signalRegistry.deposit(caseId, "trail", 0.8, Duration.ofMinutes(5), "a1", 100);
    signalRegistry.deposit(caseId, "trail", 0.9, Duration.ofMinutes(5), "a2", 100);
    var events = coordinator.detectPatterns(caseId, config);
    assertEquals(1, events.size());
    assertEquals(
        StigmergyCoordinator.CoordinationEvent.Type.SIGNAL_CONSENSUS, events.get(0).type());
  }

  @Test
  void consensusDeduplicates() {
    coordinator.initializeCase(caseId, List.of("a1", "a2"), config, "test-tenant");
    signalRegistry.deposit(caseId, "trail", 0.8, Duration.ofMinutes(5), "a1", 100);
    signalRegistry.deposit(caseId, "trail", 0.9, Duration.ofMinutes(5), "a2", 100);
    var first = coordinator.detectPatterns(caseId, config);
    assertEquals(1, first.size());
    var second = coordinator.detectPatterns(caseId, config);
    assertTrue(second.isEmpty());
  }

  @Test
  void detectPatternsPostsConsensusToCoordinationChannel() {
    var channelProvider = new RecordingCaseChannelProvider();
    coordinator =
        new StigmergyCoordinator(
            signalRegistry, observationRegistry, activityTracker, null, channelProvider);
    coordinator.initializeCase(caseId, List.of("a1", "a2"), config, "test-tenant");
    coordinator.agentJoined(caseId, "a1", "b1");
    coordinator.agentJoined(caseId, "a2", "b2");
    coordinator.agentActivated(caseId, "a1");
    coordinator.agentActivated(caseId, "a2");
    signalRegistry.deposit(caseId, "trail", 0.8, Duration.ofMinutes(5), "a1", 100);
    signalRegistry.deposit(caseId, "trail", 0.9, Duration.ofMinutes(5), "a2", 100);

    coordinator.detectPatterns(caseId, config);

    assertTrue(
        channelProvider.posts.stream()
            .anyMatch(
                p ->
                    p.content().contains("SIGNAL_CONSENSUS_DETECTED")
                        && p.type() == MessageType.STATUS
                        && "engine:swarm".equals(p.from())));
  }

  @Test
  void detectPatternsWorksWithoutChannelProvider() {
    coordinator.initializeCase(caseId, List.of("a1", "a2"), config, "test-tenant");
    coordinator.agentJoined(caseId, "a1", "b1");
    coordinator.agentJoined(caseId, "a2", "b2");
    coordinator.agentActivated(caseId, "a1");
    coordinator.agentActivated(caseId, "a2");
    signalRegistry.deposit(caseId, "trail", 0.8, Duration.ofMinutes(5), "a1", 100);
    signalRegistry.deposit(caseId, "trail", 0.9, Duration.ofMinutes(5), "a2", 100);

    var events = coordinator.detectPatterns(caseId, config);

    assertEquals(1, events.size());
    assertEquals(
        StigmergyCoordinator.CoordinationEvent.Type.SIGNAL_CONSENSUS, events.get(0).type());
  }

  @Test
  void agentJoinedSetsWorkStateToIdle() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test-tenant");
    coordinator.agentJoined(caseId, "agent-1", "binding-1");
    var agent = coordinator.getAgent(caseId, "agent-1");
    assertEquals(AgentWorkState.IDLE, agent.workState());
  }

  @Test
  void agentDepartedResetsWorkStateToIdle() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test-tenant");
    coordinator.agentJoined(caseId, "agent-1", "binding-1");
    coordinator.agentActivated(caseId, "agent-1");
    coordinator.agentDeparted(caseId, "agent-1");
    var agent = coordinator.getAgent(caseId, "agent-1");
    assertEquals(AgentWorkState.IDLE, agent.workState());
  }

  @Test
  void transitionWorkStateChangesState() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test-tenant");
    coordinator.agentJoined(caseId, "agent-1", "binding-1");
    coordinator.agentActivated(caseId, "agent-1");

    coordinator.transitionWorkState(caseId, "agent-1", AgentWorkState.ASSIGNED);
    assertEquals(AgentWorkState.ASSIGNED, coordinator.getAgent(caseId, "agent-1").workState());

    coordinator.transitionWorkState(caseId, "agent-1", AgentWorkState.EXECUTING);
    assertEquals(AgentWorkState.EXECUTING, coordinator.getAgent(caseId, "agent-1").workState());

    coordinator.transitionWorkState(caseId, "agent-1", AgentWorkState.IDLE);
    assertEquals(AgentWorkState.IDLE, coordinator.getAgent(caseId, "agent-1").workState());
  }

  @Test
  void transitionWorkStateRejectsInvalidTransition() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test-tenant");
    coordinator.agentJoined(caseId, "agent-1", "binding-1");
    coordinator.agentActivated(caseId, "agent-1");

    assertThrows(
        IllegalStateException.class,
        () -> coordinator.transitionWorkState(caseId, "agent-1", AgentWorkState.EXECUTING));
  }

  @Test
  void transitionWorkStateFiresListener() {
    var listener = new RecordingWorkLifecycleListener();
    coordinator.addWorkLifecycleListener(listener);
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test-tenant");
    coordinator.agentJoined(caseId, "agent-1", "binding-1");
    coordinator.agentActivated(caseId, "agent-1");

    coordinator.transitionWorkState(caseId, "agent-1", AgentWorkState.ASSIGNED);

    assertEquals(1, listener.stateChanges.size());
    var change = listener.stateChanges.get(0);
    assertEquals(AgentWorkState.IDLE, change.from);
    assertEquals(AgentWorkState.ASSIGNED, change.to);
    assertEquals("agent-1", change.agentId);
  }

  @Test
  void taskAssignedTransitionsAndFiresListener() {
    var listener = new RecordingWorkLifecycleListener();
    coordinator.addWorkLifecycleListener(listener);
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test-tenant");
    coordinator.agentJoined(caseId, "agent-1", "binding-1");
    coordinator.agentActivated(caseId, "agent-1");

    coordinator.taskAssigned(caseId, "agent-1", "worker-1", Map.of("key", "value"));

    assertEquals(AgentWorkState.ASSIGNED, coordinator.getAgent(caseId, "agent-1").workState());
    assertEquals(1, listener.stateChanges.size());
    assertEquals(1, listener.taskAssignments.size());
    assertEquals("worker-1", listener.taskAssignments.get(0).taskId);
  }

  @Test
  void assignedToIdleIsValidForFailedResolution() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test-tenant");
    coordinator.agentJoined(caseId, "agent-1", "binding-1");
    coordinator.agentActivated(caseId, "agent-1");

    coordinator.transitionWorkState(caseId, "agent-1", AgentWorkState.ASSIGNED);
    coordinator.transitionWorkState(caseId, "agent-1", AgentWorkState.IDLE);
    assertEquals(AgentWorkState.IDLE, coordinator.getAgent(caseId, "agent-1").workState());
  }

  static class RecordingCaseChannelProvider extends NoOpCaseChannelProvider {
    final List<PostRequest> posts = new ArrayList<>();

    @Override
    public void postToChannel(CaseChannel channel, PostRequest request) {
      posts.add(request);
    }
  }

  @Test
  void recordAgentMessage_incrementsCounter() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test");
    coordinator.agentJoined(caseId, "agent-1", "b-1");

    coordinator.recordAgentMessage(caseId, "agent-1");
    coordinator.recordAgentMessage(caseId, "agent-1");

    var counters = coordinator.getAgentCounters(caseId, "agent-1");
    assertNotNull(counters);
    assertEquals(2, counters.messagesSent());
  }

  @Test
  void recordAgentTaskCompleted_incrementsCounter() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test");
    coordinator.agentJoined(caseId, "agent-1", "b-1");

    coordinator.recordAgentTaskCompleted(caseId, "agent-1");

    var counters = coordinator.getAgentCounters(caseId, "agent-1");
    assertEquals(1, counters.tasksCompleted());
  }

  @Test
  void updateAgentWorkState_setsState() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test");
    coordinator.agentJoined(caseId, "agent-1", "b-1");

    coordinator.updateAgentWorkState(caseId, "agent-1", AgentWorkState.EXECUTING);

    var counters = coordinator.getAgentCounters(caseId, "agent-1");
    assertEquals(AgentWorkState.EXECUTING, counters.workState());
  }

  @Test
  void counters_resetOnEvict() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test");
    coordinator.agentJoined(caseId, "agent-1", "b-1");
    coordinator.recordAgentMessage(caseId, "agent-1");

    coordinator.evictByCase(caseId);

    assertNull(coordinator.getAgentCounters(caseId, "agent-1"));
  }

  @Test
  void counters_createdOnAgentJoined() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test");
    coordinator.agentJoined(caseId, "agent-1", "b-1");

    var counters = coordinator.getAgentCounters(caseId, "agent-1");
    assertNotNull(counters);
    assertEquals(0, counters.messagesSent());
    assertEquals(0, counters.tasksCompleted());
    assertEquals(AgentWorkState.IDLE, counters.workState());
  }

  @Test
  void counters_nullForUnknownAgent() {
    coordinator.initializeCase(caseId, List.of("agent-1"), config, "test");
    assertNull(coordinator.getAgentCounters(caseId, "unknown"));
  }

  static class RecordingWorkLifecycleListener implements AgentWorkLifecycleListener {
    record StateChange(UUID caseId, String agentId, AgentWorkState from, AgentWorkState to) {}

    record TaskAssignment(
        UUID caseId, String agentId, String taskId, Map<String, Object> context) {}

    final List<StateChange> stateChanges = new ArrayList<>();
    final List<TaskAssignment> taskAssignments = new ArrayList<>();

    @Override
    public void onWorkStateChanged(
        UUID caseId, String agentId, AgentWorkState from, AgentWorkState to) {
      stateChanges.add(new StateChange(caseId, agentId, from, to));
    }

    @Override
    public void onTaskAssigned(
        UUID caseId, String agentId, String taskId, Map<String, Object> context) {
      taskAssignments.add(new TaskAssignment(caseId, agentId, taskId, context));
    }
  }
}
