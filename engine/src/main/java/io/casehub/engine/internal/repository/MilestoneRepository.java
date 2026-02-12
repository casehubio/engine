package io.casehub.engine.internal.repository;

import io.casehub.engine.internal.model.MilestoneReached;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;

@ApplicationScoped
public class MilestoneRepository {

  @Inject
  Mutiny.SessionFactory sessionFactory;

  public Uni<MilestoneReached> persistReachedMilestone(MilestoneReached milestoneReached) {
    return sessionFactory.withTransaction((session, tx) ->
            session.persist(milestoneReached)
                    .replaceWith(milestoneReached)
    );
  }


}
