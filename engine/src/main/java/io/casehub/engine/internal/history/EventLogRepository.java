package io.casehub.engine.internal.history;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class EventLogRepository implements PanacheRepository<EventLog> {

}
