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
package io.casehub.blackboard.strategy;

import io.casehub.api.context.CaseContext;
import io.casehub.blackboard.plan.PlanItem;
import java.util.List;

/**
 * SPI for selecting which eligible {@link PlanItem}s to activate.
 *
 * <p>{@code eligible} contains PlanItem&lt;Worker&gt; and PlanItem&lt;SubCase&gt; whose containing
 * Stage is ACTIVE and whose Binding trigger conditions have been evaluated as true by the engine.
 */
public interface PlanningStrategy {
  List<PlanItem<?>> select(CaseContext context, List<PlanItem<?>> eligible);
}
