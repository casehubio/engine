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
package io.casehub.api.engine;

import io.casehub.api.context.StateContext;
import io.casehub.api.model.DispatchRule;
import java.util.List;

/**
 * SPI for controlling which eligible dispatch rules are selected for execution after their trigger
 * conditions have been evaluated.
 *
 * <p>The default implementation ({@link io.casehub.engine.internal.engine.ChoreographyLoopControl})
 * fires all eligible rules concurrently — pure choreography. Provide an alternative CDI bean
 * ({@code @Alternative @Priority} ) to introduce deliberate ordering, prioritisation, or sequential
 * execution.
 */
public interface LoopControl {

  /**
   * Select the dispatch rules to fire from the set of rules whose trigger conditions have already
   * been evaluated and matched.
   *
   * @param context the current case state context
   * @param eligible rules whose trigger conditions matched — may be empty, never null
   * @return the subset to fire; may be empty, may be the full list, must not be null
   */
  List<DispatchRule> select(StateContext context, List<DispatchRule> eligible);
}
