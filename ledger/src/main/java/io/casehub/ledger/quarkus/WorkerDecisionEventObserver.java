package io.casehub.ledger.quarkus;

import io.casehub.engine.common.spi.event.CaseLifecycleEvent;
import io.casehub.ledger.service.WorkerDecisionEventCapture;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class WorkerDecisionEventObserver {

  @Inject WorkerDecisionEventCapture capture;

  @Transactional
  void onCaseLifecycleEvent(@ObservesAsync CaseLifecycleEvent event) {
    capture.onCaseLifecycleEvent(event);
  }
}
