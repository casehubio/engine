package io.casehub.ledger.quarkus;

import io.casehub.ledger.api.model.WorkerDecisionEvent;
import io.casehub.ledger.service.WorkerDecisionEventCapture;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class WorkerDecisionEventObserver {

  @Inject WorkerDecisionEventCapture capture;

  @Transactional
  void onWorkerDecisionEvent(@ObservesAsync WorkerDecisionEvent event) {
    capture.onWorkerDecisionEvent(event);
  }
}
