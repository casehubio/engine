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
package io.casehub.api.model.coordination;

public enum DecisionScope {
  STIMULUS_DRIVEN {
    @Override
    public boolean isPermitted(DecisionType type) {
      return switch (type) {
        case SIGNALING -> true;
        case TASK_SELECTION, COLLABORATION, PROVISIONING, CONTEXT_MUTATION -> false;
      };
    }
  },
  TASK_ADAPTATION {
    @Override
    public boolean isPermitted(DecisionType type) {
      return switch (type) {
        case COLLABORATION, SIGNALING, CONTEXT_MUTATION -> true;
        case TASK_SELECTION, PROVISIONING -> false;
      };
    }
  },
  FULL_AUTONOMY {
    @Override
    public boolean isPermitted(DecisionType type) {
      return switch (type) {
        case TASK_SELECTION, COLLABORATION, SIGNALING, PROVISIONING, CONTEXT_MUTATION -> true;
      };
    }
  };

  public abstract boolean isPermitted(DecisionType type);
}
