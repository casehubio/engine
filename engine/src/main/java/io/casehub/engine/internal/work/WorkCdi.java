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
package io.casehub.engine.internal.work;

/**
 * CDI configuration for quarkus-work-core beans.
 *
 * <p>WorkBroker, LeastLoadedStrategy, ClaimFirstStrategy, and NoOpWorkerRegistry are all annotated
 * {@code @ApplicationScoped} in the quarkus-work-core library and are discovered automatically by
 * the CDI container. No explicit producer methods are needed.
 *
 * <p>To select a specific WorkerSelectionStrategy, inject the concrete type directly (e.g.
 * {@code @Inject LeastLoadedStrategy}) rather than the interface, as multiple implementations are
 * present on the classpath.
 */
public class WorkCdi {
  // Intentionally empty — quarkus-work-core provides its beans via @ApplicationScoped annotations.
}
