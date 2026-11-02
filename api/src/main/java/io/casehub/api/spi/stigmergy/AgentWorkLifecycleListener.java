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
package io.casehub.api.spi.stigmergy;

import io.casehub.api.model.stigmergy.AgentWorkState;
import java.util.Map;
import java.util.UUID;

/**
 * Receives per-agent work lifecycle events from the engine runtime.
 *
 * <p>Implementations are multi-instance: multiple listeners coexist via CDI {@code Instance} /
 * Spring {@code ObjectProvider}. Do not use {@code @DefaultBean} on implementations.
 */
public interface AgentWorkLifecycleListener {

  default void onWorkStateChanged(
      UUID caseId, String agentId, AgentWorkState from, AgentWorkState to) {}

  default void onTaskAssigned(
      UUID caseId, String agentId, String taskId, Map<String, Object> context) {}

  default void onTaskAdapted(UUID caseId, String agentId, String taskId, String adaptationReason) {}

  default void onRoleAdopted(UUID caseId, String agentId, String groupId, String role) {}

  default void onRoleReleased(UUID caseId, String agentId, String groupId, String role) {}
}
