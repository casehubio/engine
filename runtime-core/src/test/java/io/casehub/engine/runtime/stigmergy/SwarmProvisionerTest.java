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
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.casehub.api.model.event.CaseHubEventType;
import io.casehub.api.model.stigmergy.DetectedRole;
import io.casehub.api.model.stigmergy.DetectedTeam;
import io.casehub.api.model.stigmergy.IntegrationPolicy;
import io.casehub.api.model.stigmergy.ProvisionBudget;
import io.casehub.api.model.stigmergy.StigmergyConfig;
import io.casehub.api.model.stigmergy.SwarmConfig;
import io.casehub.api.model.stigmergy.SwarmProgress;
import io.casehub.api.spi.DispatchBudget;
import io.casehub.api.spi.DispatchBudgetQuery;
import io.casehub.api.spi.ProvisionResult;
import io.casehub.api.spi.WorkerProvisioner;
import io.casehub.api.spi.stigmergy.SwarmCoordinationListener;
import io.casehub.engine.common.internal.convergence.ActivityTracker;
import io.casehub.engine.common.internal.observation.ObservationRegistry;
import io.casehub.engine.common.internal.observation.RuleRegistry;
import io.casehub.engine.common.internal.signal.SignalRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SwarmProvisionerTest {

  private SwarmProvisioner swarmProvisioner;
  private StigmergyCoordinator coordinator;
  private SignalRegistry signalRegistry;
  private ObservationRegistry observationRegistry;
  private ActivityTracker activityTracker;
  private RoleTracker roleTracker;
  private TeamDetector teamDetector;
  private SwarmProgressTracker progressTracker;
  private RecordingWorkerProvisioner workerProvisioner;
  private RecordingCoordinationListener listener;

  private UUID caseId;

  @BeforeEach
  void setUp() {
    observationRegistry = new ObservationRegistry();
    signalRegistry = new SignalRegistry();
    var ruleRegistry = new RuleRegistry();
    activityTracker = new ActivityTracker();
    coordinator = new StigmergyCoordinator(signalRegistry, observationRegistry, activityTracker);
    roleTracker = new RoleTracker(observationRegistry, signalRegistry, ruleRegistry, coordinator);
    teamDetector = new TeamDetector(observationRegistry, signalRegistry, roleTracker, coordinator);
    progressTracker = new SwarmProgressTracker(signalRegistry, roleTracker, coordinator);
    workerProvisioner = new RecordingWorkerProvisioner();
    listener = new RecordingCoordinationListener();
    swarmProvisioner =
        new SwarmProvisioner(
            workerProvisioner,
            coordinator,
            signalRegistry,
            activityTracker,
            roleTracker,
            teamDetector,
            progressTracker,
            new NoOpDispatchBudget(),
            null,
            List.<SwarmCoordinationListener>of(listener));
    caseId = UUID.randomUUID();
  }

  @Test
  void budgetMaxSwarmSizeBlocksProvisioning() {
    var budget = new ProvisionBudget(50, 10, 0, null, null);
    var policy = IntegrationPolicy.BALANCED;
    var swarmConfig =
        new SwarmConfig(2, null, null, null, null, null, null, null, null, budget, policy);
    var config = new StigmergyConfig(null, null, swarmConfig);
    coordinator.initializeCase(caseId, List.of("a1", "a2"), config);
    for (String id : List.of("a1", "a2")) {
      coordinator.agentJoined(caseId, id, "b-" + id);
      coordinator.agentActivated(caseId, id);
    }

    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.8, Duration.ofMinutes(5), "a1", 100);
    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.8, Duration.ofMinutes(5), "a2", 100);

    var events = swarmProvisioner.evaluateAndProvision(caseId, swarmConfig, config);

    assertTrue(
        events.stream()
            .anyMatch(e -> e.type() == CaseHubEventType.SWARM_PROVISION_BUDGET_EXHAUSTED));
    assertEquals(0, workerProvisioner.provisionCount());
  }

  @Test
  void cooldownPreventsRapidProvisioning() {
    var budget = new ProvisionBudget(50, 10, 5, null, null);
    var policy = IntegrationPolicy.BALANCED;
    var swarmConfig =
        new SwarmConfig(20, null, null, null, null, null, null, null, null, budget, policy);
    var config = new StigmergyConfig(null, null, swarmConfig);
    coordinator.initializeCase(caseId, List.of("a1", "a2"), config);
    for (String id : List.of("a1", "a2")) {
      coordinator.agentJoined(caseId, id, "b-" + id);
      coordinator.agentActivated(caseId, id);
    }

    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.8, Duration.ofMinutes(5), "a1", 100);
    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.8, Duration.ofMinutes(5), "a2", 100);

    var events1 = swarmProvisioner.evaluateAndProvision(caseId, swarmConfig, config);
    assertTrue(
        events1.stream().anyMatch(e -> e.type() == CaseHubEventType.SWARM_PROVISION_COMPLETED));

    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.9, Duration.ofMinutes(5), "a1", 100);
    var events2 = swarmProvisioner.evaluateAndProvision(caseId, swarmConfig, config);

    assertTrue(
        events2.stream()
            .anyMatch(e -> e.type() == CaseHubEventType.SWARM_PROVISION_BUDGET_EXHAUSTED));
    assertEquals(1, workerProvisioner.provisionCount());
  }

  @Test
  void successfulProvisionJoinsSwarm() {
    var budget = new ProvisionBudget(50, 10, 0, null, null);
    var policy = IntegrationPolicy.BALANCED;
    var swarmConfig =
        new SwarmConfig(20, null, null, null, null, null, null, null, null, budget, policy);
    var config = new StigmergyConfig(null, null, swarmConfig);
    coordinator.initializeCase(caseId, List.of("a1", "a2"), config);
    for (String id : List.of("a1", "a2")) {
      coordinator.agentJoined(caseId, id, "b-" + id);
      coordinator.agentActivated(caseId, id);
    }

    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.8, Duration.ofMinutes(5), "a1", 100);
    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.8, Duration.ofMinutes(5), "a2", 100);

    var events = swarmProvisioner.evaluateAndProvision(caseId, swarmConfig, config);

    assertTrue(
        events.stream().anyMatch(e -> e.type() == CaseHubEventType.SWARM_PROVISION_COMPLETED));
    assertEquals(3, coordinator.activeAgents(caseId).size());
    assertEquals(1, workerProvisioner.provisionCount());
  }

  @Test
  void integrationDelayExemptsFromIdleDetection() {
    var budget = new ProvisionBudget(50, 10, 0, 0.5, 1);
    var policy = new IntegrationPolicy(0.7, 5, 0.5);
    var swarmConfig =
        new SwarmConfig(20, null, null, null, null, null, null, null, null, budget, policy);
    var config = new StigmergyConfig(null, null, swarmConfig);
    coordinator.initializeCase(caseId, List.of("a1", "a2"), config);
    for (String id : List.of("a1", "a2")) {
      coordinator.agentJoined(caseId, id, "b-" + id);
      coordinator.agentActivated(caseId, id);
    }

    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.8, Duration.ofMinutes(5), "a1", 100);
    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.8, Duration.ofMinutes(5), "a2", 100);
    swarmProvisioner.evaluateAndProvision(caseId, swarmConfig, config);

    String provisionedId = workerProvisioner.lastProvisionedId();
    assertTrue(swarmProvisioner.isInIntegrationDelay(caseId, provisionedId));

    var deprovEvents = swarmProvisioner.evaluateDeprovisioning(caseId, swarmConfig);
    assertTrue(
        deprovEvents.stream()
            .noneMatch(
                e ->
                    e.type() == CaseHubEventType.SWARM_AGENT_TERMINATED
                        && e.metadata().get("agentId").equals(provisionedId)));
  }

  @Test
  void evictByCaseClearsState() {
    var budget = new ProvisionBudget(50, 10, 0, null, null);
    var policy = IntegrationPolicy.BALANCED;
    var swarmConfig =
        new SwarmConfig(20, null, null, null, null, null, null, null, null, budget, policy);
    var config = new StigmergyConfig(null, null, swarmConfig);
    coordinator.initializeCase(caseId, List.of("a1", "a2"), config);
    for (String id : List.of("a1", "a2")) {
      coordinator.agentJoined(caseId, id, "b-" + id);
      coordinator.agentActivated(caseId, id);
    }
    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.8, Duration.ofMinutes(5), "a1", 100);
    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.8, Duration.ofMinutes(5), "a2", 100);
    swarmProvisioner.evaluateAndProvision(caseId, swarmConfig, config);

    swarmProvisioner.evictByCase(caseId);

    assertFalse(swarmProvisioner.isInIntegrationDelay(caseId, "any"));
  }

  @Test
  void provisionEmitsInitialisedAndProvisioningChanged() {
    var budget = new ProvisionBudget(50, 10, 0, null, null);
    var policy = IntegrationPolicy.BALANCED;
    var swarmConfig =
        new SwarmConfig(20, null, null, null, null, null, null, null, null, budget, policy);
    var config = new StigmergyConfig(null, null, swarmConfig);
    coordinator.initializeCase(caseId, List.of("a1", "a2"), config);
    for (String id : List.of("a1", "a2")) {
      coordinator.agentJoined(caseId, id, "b-" + id);
      coordinator.agentActivated(caseId, id);
    }

    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.8, Duration.ofMinutes(5), "a1", 100);
    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.8, Duration.ofMinutes(5), "a2", 100);

    swarmProvisioner.evaluateAndProvision(caseId, swarmConfig, config);

    assertTrue(listener.events.contains("initialised"));
    assertTrue(listener.events.contains("provisioningChanged"));
    assertEquals(1, listener.initCount);
    assertEquals(3, listener.lastCurrentSize);
    assertEquals(20, listener.lastMaxSize);
  }

  @Test
  void secondProvisionDoesNotReEmitInitialised() {
    var budget = new ProvisionBudget(50, 10, 0, null, null);
    var policy = IntegrationPolicy.BALANCED;
    var swarmConfig =
        new SwarmConfig(20, null, null, null, null, null, null, null, null, budget, policy);
    var config = new StigmergyConfig(null, null, swarmConfig);
    coordinator.initializeCase(caseId, List.of("a1", "a2"), config);
    for (String id : List.of("a1", "a2")) {
      coordinator.agentJoined(caseId, id, "b-" + id);
      coordinator.agentActivated(caseId, id);
    }

    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.8, Duration.ofMinutes(5), "a1", 100);
    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.8, Duration.ofMinutes(5), "a2", 100);

    swarmProvisioner.evaluateAndProvision(caseId, swarmConfig, config);
    assertEquals(1, listener.initCount);

    for (int i = 0; i < 5; i++) {
      swarmProvisioner.incrementCycleCount(caseId);
    }
    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.9, Duration.ofMinutes(5), "a1", 100);
    swarmProvisioner.evaluateAndProvision(caseId, swarmConfig, config);

    assertEquals(1, listener.initCount);
    assertEquals(2, listener.provisioningChangedCount);
  }

  @Test
  void deprovisioningEmitsProvisioningChanged() {
    var budget = new ProvisionBudget(50, 10, 0, 0.5, 1);
    var policy = new IntegrationPolicy(0.7, 0, 0.5);
    var swarmConfig =
        new SwarmConfig(20, null, null, null, null, null, null, null, null, budget, policy);
    var config = new StigmergyConfig(null, null, swarmConfig);
    coordinator.initializeCase(caseId, List.of("a1", "a2"), config);
    for (String id : List.of("a1", "a2")) {
      coordinator.agentJoined(caseId, id, "b-" + id);
      coordinator.agentActivated(caseId, id);
    }

    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.8, Duration.ofMinutes(5), "a1", 100);
    signalRegistry.deposit(caseId, "swarm:need-capacity", 0.8, Duration.ofMinutes(5), "a2", 100);
    swarmProvisioner.evaluateAndProvision(caseId, swarmConfig, config);
    int provCountAfterProvision = listener.provisioningChangedCount;

    signalRegistry.evictByCase(caseId);
    swarmProvisioner.evaluateDeprovisioning(caseId, swarmConfig);

    assertTrue(
        listener.provisioningChangedCount > provCountAfterProvision,
        "Deprovisioning should emit onProvisioningChanged");
  }

  @Test
  void notifyRolesChangedForwardsToListeners() {
    var roles = List.of(new DetectedRole("role-1", Set.of("a1", "a2"), null, List.of(), 1, 0.0));
    swarmProvisioner.notifyRolesChanged(caseId, roles);

    assertTrue(listener.events.contains("roleChanged"));
    assertEquals(roles, listener.lastRoles);
  }

  @Test
  void notifyTeamsChangedForwardsToListeners() {
    var teams = List.of(new DetectedTeam("team-1", Set.of("a1", "a2"), Set.of(), 0.8, 1));
    swarmProvisioner.notifyTeamsChanged(caseId, teams);

    assertTrue(listener.events.contains("teamChanged"));
    assertEquals(teams, listener.lastTeams);
  }

  @Test
  void notifyProgressUpdatedForwardsToListeners() {
    var progress = new SwarmProgress(0.5, 0.7, 0.9, java.time.Instant.now());
    swarmProvisioner.notifyProgressUpdated(caseId, progress);

    assertTrue(listener.events.contains("progressUpdated"));
    assertEquals(progress, listener.lastProgress);
  }

  @Test
  void inactiveProvisionerDoesNotNotify() {
    var inactive = new SwarmProvisioner();
    var inactiveListener = new RecordingCoordinationListener();
    inactive.notifyRolesChanged(caseId, List.of());
    inactive.notifyTeamsChanged(caseId, List.of());
    inactive.notifyProgressUpdated(caseId, SwarmProgress.EMPTY);

    assertTrue(inactiveListener.events.isEmpty());
  }

  @Test
  void throwingListenerDoesNotPreventSubsequentListeners() {
    var throwing = new ThrowingCoordinationListener();
    var recording = new RecordingCoordinationListener();
    var provisioner =
        new SwarmProvisioner(
            workerProvisioner,
            coordinator,
            signalRegistry,
            activityTracker,
            roleTracker,
            teamDetector,
            progressTracker,
            new NoOpDispatchBudget(),
            null,
            List.<SwarmCoordinationListener>of(throwing, recording));

    var roles = List.of(new DetectedRole("role-1", Set.of("a1"), null, List.of(), 1, 0.0));
    provisioner.notifyRolesChanged(caseId, roles);
    assertTrue(
        recording.events.contains("roleChanged"),
        "recording listener should fire after throwing listener");

    var teams = List.of(new DetectedTeam("team-1", Set.of("a1"), Set.of(), 0.8, 1));
    provisioner.notifyTeamsChanged(caseId, teams);
    assertTrue(
        recording.events.contains("teamChanged"),
        "recording listener should fire after throwing listener");

    var progress = new SwarmProgress(0.5, 0.7, 0.9, java.time.Instant.now());
    provisioner.notifyProgressUpdated(caseId, progress);
    assertTrue(
        recording.events.contains("progressUpdated"),
        "recording listener should fire after throwing listener");
  }

  static class RecordingWorkerProvisioner implements WorkerProvisioner {
    private int count;
    private String lastId;

    @Override
    public ProvisionResult provision(
        Set<String> capabilities, io.casehub.api.model.ProvisionContext context) {
      count++;
      lastId = "swarm-provisioned-" + count;
      return new ProvisionResult(null, lastId);
    }

    @Override
    public void terminate(String workerId, String tenancyId) {}

    @Override
    public Set<String> getCapabilities() {
      return Set.of("*");
    }

    int provisionCount() {
      return count;
    }

    String lastProvisionedId() {
      return lastId;
    }
  }

  static class NoOpDispatchBudget implements DispatchBudget {
    @Override
    public int availableCapacity(DispatchBudgetQuery query) {
      return Integer.MAX_VALUE;
    }
  }

  static class ThrowingCoordinationListener implements SwarmCoordinationListener {
    @Override
    public void onSwarmInitialised(UUID caseId, SwarmProgress progress) {
      throw new RuntimeException("test: onSwarmInitialised");
    }

    @Override
    public void onRoleChanged(UUID caseId, List<DetectedRole> roles, SwarmProgress progress) {
      throw new RuntimeException("test: onRoleChanged");
    }

    @Override
    public void onTeamChanged(UUID caseId, List<DetectedTeam> teams, SwarmProgress progress) {
      throw new RuntimeException("test: onTeamChanged");
    }

    @Override
    public void onProgressUpdated(UUID caseId, SwarmProgress progress) {
      throw new RuntimeException("test: onProgressUpdated");
    }

    @Override
    public void onProvisioningChanged(UUID caseId, int currentSize, int maxSize) {
      throw new RuntimeException("test: onProvisioningChanged");
    }
  }

  static class RecordingCoordinationListener implements SwarmCoordinationListener {
    final List<String> events = new ArrayList<>();
    int initCount;
    int provisioningChangedCount;
    int lastCurrentSize;
    int lastMaxSize;
    List<DetectedRole> lastRoles;
    List<DetectedTeam> lastTeams;
    SwarmProgress lastProgress;

    @Override
    public void onSwarmInitialised(UUID caseId, SwarmProgress progress) {
      events.add("initialised");
      initCount++;
      lastProgress = progress;
    }

    @Override
    public void onRoleChanged(UUID caseId, List<DetectedRole> roles, SwarmProgress progress) {
      events.add("roleChanged");
      lastRoles = roles;
      lastProgress = progress;
    }

    @Override
    public void onTeamChanged(UUID caseId, List<DetectedTeam> teams, SwarmProgress progress) {
      events.add("teamChanged");
      lastTeams = teams;
      lastProgress = progress;
    }

    @Override
    public void onProgressUpdated(UUID caseId, SwarmProgress progress) {
      events.add("progressUpdated");
      lastProgress = progress;
    }

    @Override
    public void onProvisioningChanged(UUID caseId, int currentSize, int maxSize) {
      events.add("provisioningChanged");
      provisioningChangedCount++;
      lastCurrentSize = currentSize;
      lastMaxSize = maxSize;
    }
  }
}
