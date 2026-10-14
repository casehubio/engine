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
package io.casehub.engine.runtime.engine.recovery;

import static org.assertj.core.api.Assertions.assertThat;

import io.casehub.platform.api.acl.WorkerCredentialStore;
import jakarta.enterprise.inject.Instance;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WorkerRecoveryHealthCheckTest {

  @Test
  void pending_reportsUp_withInProgressData() {
    var check = new WorkerRecoveryHealthCheck(coordinatorWithStatus(RecoveryStatus.PENDING));
    HealthCheckResponse response = check.call();

    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    assertThat(response.getData()).isPresent();
    assertThat(response.getData().get()).containsEntry("status", "in-progress");
  }

  @Test
  void completed_reportsUp_withNoData() {
    var check = new WorkerRecoveryHealthCheck(coordinatorWithStatus(RecoveryStatus.COMPLETED));
    HealthCheckResponse response = check.call();

    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.UP);
    assertThat(response.getData()).isEmpty();
  }

  @Test
  void failed_reportsDown_withFailedData() {
    var check = new WorkerRecoveryHealthCheck(coordinatorWithStatus(RecoveryStatus.FAILED));
    HealthCheckResponse response = check.call();

    assertThat(response.getStatus()).isEqualTo(HealthCheckResponse.Status.DOWN);
    assertThat(response.getData()).isPresent();
    assertThat(response.getData().get()).containsEntry("status", "failed");
  }

  @SuppressWarnings("unchecked")
  private WorkerRecoveryCoordinator coordinatorWithStatus(RecoveryStatus status) {
    Instance<WorkerCredentialStore> empty = Mockito.mock(Instance.class);
    Mockito.when(empty.isResolvable()).thenReturn(false);
    return new WorkerRecoveryCoordinator(null, empty, null) {
      @Override
      public RecoveryStatus getRecoveryStatus() {
        return status;
      }
    };
  }
}
