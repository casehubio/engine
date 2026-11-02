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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class TopologyTypeTest {

  @Test
  void centralised_initiator_derives_central() {
    assertThat(TopologyType.CENTRALISED.deriveAuthority(StructuralRole.INITIATOR))
        .isEqualTo(DecisionAuthority.CENTRAL);
  }

  @Test
  void centralised_contractor_derives_local() {
    assertThat(TopologyType.CENTRALISED.deriveAuthority(StructuralRole.CONTRACTOR))
        .isEqualTo(DecisionAuthority.LOCAL);
  }

  @Test
  void centralised_peer_is_invalid() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TopologyType.CENTRALISED.deriveAuthority(StructuralRole.PEER))
        .withMessageContaining("CENTRALISED");
  }

  @Test
  void distributed_peer_derives_coordinated() {
    assertThat(TopologyType.DISTRIBUTED.deriveAuthority(StructuralRole.PEER))
        .isEqualTo(DecisionAuthority.COORDINATED);
  }

  @Test
  void distributed_initiator_is_invalid() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TopologyType.DISTRIBUTED.deriveAuthority(StructuralRole.INITIATOR))
        .withMessageContaining("DISTRIBUTED");
  }

  @Test
  void distributed_contractor_is_invalid() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TopologyType.DISTRIBUTED.deriveAuthority(StructuralRole.CONTRACTOR))
        .withMessageContaining("DISTRIBUTED");
  }

  @Test
  void hybrid_initiator_derives_central() {
    assertThat(TopologyType.HYBRID.deriveAuthority(StructuralRole.INITIATOR))
        .isEqualTo(DecisionAuthority.CENTRAL);
  }

  @Test
  void hybrid_peer_derives_coordinated() {
    assertThat(TopologyType.HYBRID.deriveAuthority(StructuralRole.PEER))
        .isEqualTo(DecisionAuthority.COORDINATED);
  }

  @Test
  void hybrid_contractor_derives_local() {
    assertThat(TopologyType.HYBRID.deriveAuthority(StructuralRole.CONTRACTOR))
        .isEqualTo(DecisionAuthority.LOCAL);
  }
}
