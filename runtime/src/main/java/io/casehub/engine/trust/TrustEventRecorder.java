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
package io.casehub.engine.trust;

import io.casehub.ledger.api.model.AttestationVerdict;
import io.casehub.ledger.api.model.LedgerAttestation;
import io.casehub.ledger.api.model.LedgerEntryType;
import io.casehub.ledger.api.spi.LedgerEntryRepository;
import io.casehub.ledger.runtime.model.PlainLedgerEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.UUID;

@ApplicationScoped
public class TrustEventRecorder {

  private final TrustEvolutionConfig config;
  private final LedgerEntryRepository ledgerRepo;

  @Inject
  public TrustEventRecorder(
      Instance<TrustEvolutionConfig> config, Instance<LedgerEntryRepository> ledgerRepo) {
    this.config = config.isResolvable() ? config.get() : null;
    this.ledgerRepo = ledgerRepo.isResolvable() ? ledgerRepo.get() : null;
  }

  public void onTrustRelevantAction(@ObservesAsync TrustRelevantAction event) {
    if (config == null || ledgerRepo == null) return;
    var mapping = config.findMapping(event.actionType());
    if (mapping.isEmpty()) return;

    var m = mapping.get();
    var entry = new PlainLedgerEntry();
    entry.actorId = event.actorId();
    entry.entryType = LedgerEntryType.EVENT;
    entry.subjectId = UUID.nameUUIDFromBytes(event.targetId().getBytes());
    var saved = ledgerRepo.save(entry, event.tenantId());

    createAttestation(saved.id, event.targetId(), m.verdict(), m.confidence(), event.tenantId());

    for (String witnessId : event.witnessIds()) {
      createAttestation(saved.id, witnessId, m.verdict(), m.witnessConfidence(), event.tenantId());
    }
  }

  private void createAttestation(
      UUID entryId,
      String attestorId,
      AttestationVerdict verdict,
      double confidence,
      String tenantId) {
    var attestation = new LedgerAttestation();
    attestation.ledgerEntryId = entryId;
    attestation.attestorId = attestorId;
    attestation.verdict = verdict;
    attestation.confidence = confidence;
    ledgerRepo.saveAttestation(attestation, tenantId);
  }
}
