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
package io.casehub.api.model;

/**
 * SLA (Service Level Agreement) status of a milestone.
 *
 * <p>Tracks whether a milestone with {@code slaDuration} is within or past its deadline.
 *
 * <ul>
 *   <li><b>NOT_STARTED</b> — milestone not yet activated (PENDING state)
 *   <li><b>ON_TRACK</b> — milestone activated, within SLA deadline
 *   <li><b>BREACHED</b> — SLA deadline passed
 * </ul>
 *
 * <p>SLA status is orthogonal to lifecycle status: a milestone can be COMPLETED + BREACHED (late
 * completion).
 */
public enum SlaStatus {
  NOT_STARTED,
  ON_TRACK,
  BREACHED
}
