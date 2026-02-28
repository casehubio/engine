package io.casehub.engine.internal.worker;

import jakarta.enterprise.context.ApplicationScoped;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


@ApplicationScoped
public class WorkflowExecutionTask implements Job {

  @Override
  public void execute(JobExecutionContext executionContext) throws JobExecutionException {
    System.out.println("Executing workflow task: " + executionContext.getJobDetail().getKey());
  }
}
