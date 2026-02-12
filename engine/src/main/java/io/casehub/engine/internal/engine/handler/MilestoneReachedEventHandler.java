package io.casehub.engine.internal.engine.handler;

import io.casehub.engine.internal.event.EventBusAddresses;
import io.casehub.engine.internal.event.MilestoneReachedEvent;
import io.casehub.engine.internal.model.MilestoneReached;
import io.casehub.engine.internal.repository.MilestoneRepository;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class MilestoneReachedEventHandler {

  private static final Logger LOG = Logger.getLogger(MilestoneReachedEventHandler.class);

  @Inject
  MilestoneRepository milestoneRepository;

  @ConsumeEvent(value = EventBusAddresses.MILESTONE_REACHED)
  public Uni<Void> onMilestoneReached(MilestoneReachedEvent event) {
    LOG.infof("=== MilestoneReachedEvent received ===");
    LOG.infof("Milestone: %s", event.milestone().getName());
    LOG.infof("Run ID: %s", event.getRunId());

    MilestoneReached milestoneReached = new MilestoneReached();
    milestoneReached.setRunId(event.getRunId());
    milestoneReached.setMilestoneName(event.milestone().getName());
    milestoneReached.setMilestoneDescription(event.milestone().getDescription());
    milestoneReached.setMilestoneCondition(event.milestone().getCondition());
    milestoneReached.setContextSnapshot(event.runState().getStateContext().asJsonNode());

    return milestoneRepository.persistReachedMilestone(milestoneReached)
            .onItem().invoke(persisted ->
                    LOG.infof("Milestone '%s' persisted to database (id: %d) for runId: %s",
                            persisted.getMilestoneName(), persisted.getId(), persisted.getRunId())
            )
            .onFailure().invoke(e ->
                    LOG.errorf(e, "Failed to persist milestone '%s' for runId: %s",
                            event.milestone().getName(), event.getRunId())
            )
            .replaceWithVoid();
  }
}
