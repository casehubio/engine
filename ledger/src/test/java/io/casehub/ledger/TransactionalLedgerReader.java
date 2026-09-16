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
package io.casehub.ledger;

import io.casehub.ledger.model.CaseLedgerEntry;
import io.casehub.ledger.model.WorkerDecisionEntry;
import io.casehub.ledger.repository.CaseLedgerEntryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TransactionalLedgerReader {

  @Inject CaseLedgerEntryRepository repository;

  @Transactional
  public List<CaseLedgerEntry> findByCaseId(UUID caseId) {
    return repository.findByCaseId(caseId);
  }

  @Transactional
  public List<WorkerDecisionEntry> findWorkerDecisionsByCaseId(UUID caseId) {
    return repository.findWorkerDecisionsByCaseId(caseId);
  }

  @Transactional
  public Optional<CaseLedgerEntry> findLatestByCaseId(UUID caseId) {
    return repository.findLatestByCaseId(caseId);
  }
}
