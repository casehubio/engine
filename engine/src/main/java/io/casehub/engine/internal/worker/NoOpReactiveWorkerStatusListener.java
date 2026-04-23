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
package io.casehub.engine.internal.worker;

import io.casehub.api.model.WorkResult;
import io.casehub.api.spi.ReactiveWorkerStatusListener;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import java.util.Map;

/**
 * Reactive no-op WorkerStatusListener. Silently ignores all lifecycle events.
 *
 * <p>Marked {@code @Alternative} — not the primary bean. Activate explicitly when a reactive
 * listener is needed but no real implementation is registered.
 */
@Alternative
@ApplicationScoped
public class NoOpReactiveWorkerStatusListener implements ReactiveWorkerStatusListener {

  @Override
  public Uni<Void> onWorkerStarted(String workerId, Map<String, String> sessionMeta) {
    return Uni.createFrom().voidItem();
  }

  @Override
  public Uni<Void> onWorkerCompleted(String workerId, WorkResult result) {
    return Uni.createFrom().voidItem();
  }

  @Override
  public Uni<Void> onWorkerStalled(String workerId) {
    return Uni.createFrom().voidItem();
  }
}
