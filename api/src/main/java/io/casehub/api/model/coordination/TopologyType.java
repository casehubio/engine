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

public enum TopologyType {
  CENTRALISED {
    @Override
    public DecisionAuthority deriveAuthority(StructuralRole role) {
      return switch (role) {
        case INITIATOR -> DecisionAuthority.CENTRAL;
        case CONTRACTOR -> DecisionAuthority.LOCAL;
        case PEER ->
            throw new IllegalArgumentException(
                "CENTRALISED topology does not support PEER"
                    + " — centralised structure requires coordinator/subordinate relationships");
      };
    }
  },
  DISTRIBUTED {
    @Override
    public DecisionAuthority deriveAuthority(StructuralRole role) {
      return switch (role) {
        case PEER -> DecisionAuthority.COORDINATED;
        case INITIATOR ->
            throw new IllegalArgumentException(
                "DISTRIBUTED topology does not support INITIATOR"
                    + " — peer-to-peer structure has no central coordinator");
        case CONTRACTOR ->
            throw new IllegalArgumentException(
                "DISTRIBUTED topology does not support CONTRACTOR"
                    + " — peer-to-peer structure has no delegation hierarchy");
      };
    }
  },
  HYBRID {
    @Override
    public DecisionAuthority deriveAuthority(StructuralRole role) {
      return switch (role) {
        case INITIATOR -> DecisionAuthority.CENTRAL;
        case PEER -> DecisionAuthority.COORDINATED;
        case CONTRACTOR -> DecisionAuthority.LOCAL;
      };
    }
  },
  HIERARCHICAL {
    @Override
    public DecisionAuthority deriveAuthority(StructuralRole role) {
      return switch (role) {
        case INITIATOR -> DecisionAuthority.CENTRAL;
        case CONTRACTOR -> DecisionAuthority.LOCAL;
        case PEER ->
            throw new IllegalArgumentException(
                "HIERARCHICAL topology does not support PEER"
                    + " — tree structure requires parent/child relationships");
      };
    }
  },
  GRADUATED {
    @Override
    public DecisionAuthority deriveAuthority(StructuralRole role) {
      return switch (role) {
        case INITIATOR -> DecisionAuthority.CENTRAL;
        case PEER -> DecisionAuthority.COORDINATED;
        case CONTRACTOR -> DecisionAuthority.LOCAL;
      };
    }
  };

  public abstract DecisionAuthority deriveAuthority(StructuralRole role);
}
