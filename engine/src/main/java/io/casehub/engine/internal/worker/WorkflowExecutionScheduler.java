package io.casehub.engine.internal.worker;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.quartz.*;

@ApplicationScoped
public class WorkflowExecutionScheduler {

    @Inject
    Scheduler quartz;

    public void scheduleOrReschedule(JobDetail job, Trigger trigger) {
        try {
            quartz.scheduleJob(job, trigger);
        } catch (ObjectAlreadyExistsException e) {
            try {
                quartz.rescheduleJob(trigger.getKey(), trigger);
            } catch (SchedulerException ex) {
                throw new RuntimeException("Quartz scheduling failed for jobKey=" + job.getKey(), ex);
            }
        } catch (SchedulerException e) {
            throw new RuntimeException("Quartz scheduling failed for jobKey=" + job.getKey(), e);
        }
    }

    public Uni<Void> scheduleOrRescheduleAsync(JobDetail job, Trigger trigger) {
        return Uni.createFrom().item(() -> {
                    scheduleOrReschedule(job, trigger);
                    return (Void) null;
                })
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                .replaceWithVoid();
    }
}
