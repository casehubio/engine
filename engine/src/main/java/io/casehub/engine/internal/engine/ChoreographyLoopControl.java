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
package io.casehub.engine.internal.engine;

import io.casehub.api.context.StateContext;
import io.casehub.api.engine.LoopControl;
import io.casehub.api.model.DispatchRule;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/**
 * Default {@link LoopControl} — fires all eligible dispatch rules concurrently.
 *
 * <p>Pure choreography: every rule whose trigger condition matched is scheduled for execution
 * without deliberate ordering or prioritisation. Replace this bean via
 * {@code @Alternative @Priority} to introduce a planning strategy or sequential execution model.
 */
@ApplicationScoped
public class ChoreographyLoopControl implements LoopControl {

  @Override
  public List<DispatchRule> select(final StateContext context, final List<DispatchRule> eligible) {
    return eligible;
  }
}
