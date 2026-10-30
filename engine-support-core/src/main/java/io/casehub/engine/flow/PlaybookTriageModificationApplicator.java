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
package io.casehub.engine.flow;

import io.serverlessworkflow.api.types.DoTask;
import io.serverlessworkflow.api.types.ForTask;
import io.serverlessworkflow.api.types.ForkTask;
import io.serverlessworkflow.api.types.ForkTaskConfiguration;
import io.serverlessworkflow.api.types.Input;
import io.serverlessworkflow.api.types.InputFrom;
import io.serverlessworkflow.api.types.SetTask;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskBase;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.TryTask;
import io.serverlessworkflow.api.types.Workflow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

public final class PlaybookTriageModificationApplicator {

  private static final Logger LOG = Logger.getLogger(PlaybookTriageModificationApplicator.class);

  public record ModificationResult(Workflow workflow, Map<String, Object> parameterOverrides) {}

  private PlaybookTriageModificationApplicator() {}

  public static ModificationResult apply(
      Workflow workflow, List<PlaybookTriageModification> modifications) {
    if (modifications.isEmpty()) {
      return new ModificationResult(workflow, Map.of());
    }

    Set<String> skipSteps =
        modifications.stream()
            .filter(m -> m instanceof PlaybookTriageModification.SkipStep)
            .map(m -> ((PlaybookTriageModification.SkipStep) m).stepId())
            .collect(Collectors.toSet());

    Map<String, Map<String, Object>> replaceInputs = new LinkedHashMap<>();
    modifications.stream()
        .filter(m -> m instanceof PlaybookTriageModification.ReplaceStepInput)
        .map(m -> (PlaybookTriageModification.ReplaceStepInput) m)
        .forEach(r -> replaceInputs.put(r.stepId(), r.newInput()));

    Map<String, Object> paramOverrides = new LinkedHashMap<>();
    modifications.stream()
        .filter(m -> m instanceof PlaybookTriageModification.UpdateParameter)
        .map(m -> (PlaybookTriageModification.UpdateParameter) m)
        .forEach(u -> paramOverrides.put(u.key(), u.value()));

    List<TaskItem> modifiedTasks = transformTaskList(workflow.getDo(), skipSteps, replaceInputs);

    Workflow modified = new Workflow(workflow.getDocument(), modifiedTasks);
    if (workflow.getInput() != null) modified.setInput(workflow.getInput());
    if (workflow.getUse() != null) modified.setUse(workflow.getUse());
    if (workflow.getTimeout() != null) modified.setTimeout(workflow.getTimeout());
    if (workflow.getOutput() != null) modified.setOutput(workflow.getOutput());

    return new ModificationResult(modified, Map.copyOf(paramOverrides));
  }

  private static List<TaskItem> transformTaskList(
      List<TaskItem> tasks,
      Set<String> skipSteps,
      Map<String, Map<String, Object>> replaceInputs) {
    List<TaskItem> result = new ArrayList<>(tasks.size());
    for (TaskItem item : tasks) {
      result.add(transformTaskItem(item, skipSteps, replaceInputs));
    }
    return result;
  }

  private static TaskItem transformTaskItem(
      TaskItem item,
      Set<String> skipSteps,
      Map<String, Map<String, Object>> replaceInputs) {
    String name = item.getName();

    if (skipSteps.contains(name)) {
      LOG.debugf("Triage: skipping step '%s'", name);
      return new TaskItem(name, new Task().withSetTask(new SetTask()));
    }

    if (replaceInputs.containsKey(name)) {
      LOG.debugf("Triage: replacing input for step '%s'", name);
      Map<String, Object> newInput = replaceInputs.get(name);
      TaskItem setInput =
          new TaskItem(name + "_triage_input", new Task().withSetTask(new SetTask()));
      var doTask = new DoTask().withDo(new ArrayList<>(List.of(setInput, item)));
      var inputFrom = new InputFrom().withObject(newInput);
      var input = new Input().withFrom(inputFrom);
      doTask.setInput(input);
      return new TaskItem(name, new Task().withDoTask(doTask));
    }

    Task task = item.getTask();
    if (task == null) return item;

    Object active = task.get();
    if (active instanceof DoTask doTask && doTask.getDo() != null) {
      List<TaskItem> transformed = transformTaskList(doTask.getDo(), skipSteps, replaceInputs);
      var newDo = new DoTask().withDo(transformed);
      copyTaskBaseProperties(doTask, newDo);
      return new TaskItem(name, new Task().withDoTask(newDo));
    }
    if (active instanceof TryTask tryTask && tryTask.getTry() != null) {
      List<TaskItem> transformed = transformTaskList(tryTask.getTry(), skipSteps, replaceInputs);
      var newTry = new TryTask().withTry(transformed).withCatch(tryTask.getCatch());
      copyTaskBaseProperties(tryTask, newTry);
      return new TaskItem(name, new Task().withTryTask(newTry));
    }
    if (active instanceof ForTask forTask && forTask.getDo() != null) {
      List<TaskItem> transformed = transformTaskList(forTask.getDo(), skipSteps, replaceInputs);
      var newFor = new ForTask().withFor(forTask.getFor()).withDo(transformed);
      if (forTask.getWhile() != null) newFor.setWhile(forTask.getWhile());
      copyTaskBaseProperties(forTask, newFor);
      return new TaskItem(name, new Task().withForTask(newFor));
    }
    if (active instanceof ForkTask forkTask
        && forkTask.getFork() != null
        && forkTask.getFork().getBranches() != null) {
      List<TaskItem> transformed =
          transformTaskList(forkTask.getFork().getBranches(), skipSteps, replaceInputs);
      var newConfig = new ForkTaskConfiguration(transformed);
      newConfig.setCompete(forkTask.getFork().isCompete());
      var newFork = new ForkTask().withFork(newConfig);
      copyTaskBaseProperties(forkTask, newFork);
      return new TaskItem(name, new Task().withForkTask(newFork));
    }

    return item;
  }

  private static void copyTaskBaseProperties(TaskBase source, TaskBase target) {
    if (source.getIf() != null) target.setIf(source.getIf());
    if (source.getInput() != null) target.setInput(source.getInput());
    if (source.getOutput() != null) target.setOutput(source.getOutput());
    if (source.getExport() != null) target.setExport(source.getExport());
    if (source.getTimeout() != null) target.setTimeout(source.getTimeout());
    if (source.getThen() != null) target.setThen(source.getThen());
    if (source.getMetadata() != null) target.setMetadata(source.getMetadata());
  }
}
