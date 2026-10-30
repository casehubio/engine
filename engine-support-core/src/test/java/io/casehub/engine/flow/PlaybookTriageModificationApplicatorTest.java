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

import static org.assertj.core.api.Assertions.assertThat;

import io.serverlessworkflow.api.types.DoTask;
import io.serverlessworkflow.api.types.Document;
import io.serverlessworkflow.api.types.SetTask;
import io.serverlessworkflow.api.types.Task;
import io.serverlessworkflow.api.types.TaskItem;
import io.serverlessworkflow.api.types.ForTask;
import io.serverlessworkflow.api.types.ForTaskConfiguration;
import io.serverlessworkflow.api.types.TryTask;
import io.serverlessworkflow.api.types.TryTaskCatch;
import io.serverlessworkflow.api.types.Workflow;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlaybookTriageModificationApplicatorTest {

  private static Workflow workflow(TaskItem... items) {
    var doc =
        new Document().withDsl("1.0.0").withNamespace("test").withName("test").withVersion("0.1.0");
    return new Workflow(doc, new ArrayList<>(List.of(items)));
  }

  private static TaskItem setTask(String name) {
    return new TaskItem(name, new Task().withSetTask(new SetTask()));
  }

  private static TaskItem doBlock(String name, TaskItem... children) {
    var doTask = new DoTask().withDo(new ArrayList<>(List.of(children)));
    return new TaskItem(name, new Task().withDoTask(doTask));
  }

  private static TaskItem tryBlock(String name, TaskItem... tryBody) {
    var tryTask =
        new TryTask()
            .withTry(new ArrayList<>(List.of(tryBody)))
            .withCatch(new TryTaskCatch().withDo(List.of()));
    return new TaskItem(name, new Task().withTryTask(tryTask));
  }

  @Test
  void emptyModificationsReturnsWorkflowUnchanged() {
    Workflow wf = workflow(setTask("step-1"), setTask("step-2"));
    var result = PlaybookTriageModificationApplicator.apply(wf, List.of());

    assertThat(result.workflow().getDo()).hasSize(2);
    assertThat(result.parameterOverrides()).isEmpty();
  }

  @Test
  void skipStepReplacesTopLevelTaskWithNoop() {
    Workflow wf = workflow(setTask("step-1"), setTask("step-2"), setTask("step-3"));
    var mods =
        List.<PlaybookTriageModification>of(new PlaybookTriageModification.SkipStep("step-2"));

    var result = PlaybookTriageModificationApplicator.apply(wf, mods);

    assertThat(result.workflow().getDo()).hasSize(3);
    TaskItem replaced = result.workflow().getDo().get(1);
    assertThat(replaced.getName()).isEqualTo("step-2");
    assertThat(replaced.getTask().getSetTask()).isNotNull();
  }

  @Test
  void skipStepInNestedDoBlock() {
    Workflow wf = workflow(doBlock("outer", setTask("inner-1"), setTask("inner-2")));
    var mods =
        List.<PlaybookTriageModification>of(new PlaybookTriageModification.SkipStep("inner-1"));

    var result = PlaybookTriageModificationApplicator.apply(wf, mods);

    DoTask outerDo = result.workflow().getDo().getFirst().getTask().getDoTask();
    assertThat(outerDo.getDo()).hasSize(2);
    assertThat(outerDo.getDo().getFirst().getName()).isEqualTo("inner-1");
    assertThat(outerDo.getDo().getFirst().getTask().getSetTask()).isNotNull();
  }

  @Test
  void skipStepInTryBlock() {
    Workflow wf = workflow(tryBlock("try-outer", setTask("try-step-1"), setTask("try-step-2")));
    var mods =
        List.<PlaybookTriageModification>of(new PlaybookTriageModification.SkipStep("try-step-1"));

    var result = PlaybookTriageModificationApplicator.apply(wf, mods);

    TryTask tryTask = result.workflow().getDo().getFirst().getTask().getTryTask();
    assertThat(tryTask.getTry().getFirst().getName()).isEqualTo("try-step-1");
    assertThat(tryTask.getTry().getFirst().getTask().getSetTask()).isNotNull();
  }

  @Test
  void skipStepInForTask() {
    var forConfig = new ForTaskConfiguration();
    forConfig.setEach("item");
    forConfig.setIn(".items");
    var forTask =
        new ForTask()
            .withFor(forConfig)
            .withDo(new ArrayList<>(List.of(setTask("loop-step-1"), setTask("loop-step-2"))));
    Workflow wf = workflow(new TaskItem("loop", new Task().withForTask(forTask)));
    var mods =
        List.<PlaybookTriageModification>of(
            new PlaybookTriageModification.SkipStep("loop-step-1"));

    var result = PlaybookTriageModificationApplicator.apply(wf, mods);

    ForTask resultFor = result.workflow().getDo().getFirst().getTask().getForTask();
    assertThat(resultFor.getDo().getFirst().getName()).isEqualTo("loop-step-1");
    assertThat(resultFor.getDo().getFirst().getTask().getSetTask()).isNotNull();
    assertThat(resultFor.getDo().get(1).getName()).isEqualTo("loop-step-2");
  }

  @Test
  void unknownStepIdIsIgnored() {
    Workflow wf = workflow(setTask("step-1"));
    var mods =
        List.<PlaybookTriageModification>of(new PlaybookTriageModification.SkipStep("nonexistent"));

    var result = PlaybookTriageModificationApplicator.apply(wf, mods);

    assertThat(result.workflow().getDo()).hasSize(1);
    assertThat(result.workflow().getDo().getFirst().getName()).isEqualTo("step-1");
  }

  @Test
  void updateParameterCollectedInOverrides() {
    Workflow wf = workflow(setTask("step-1"));
    var mods =
        List.<PlaybookTriageModification>of(
            new PlaybookTriageModification.UpdateParameter("retryMode", "lenient"),
            new PlaybookTriageModification.UpdateParameter("timeout", 5000));

    var result = PlaybookTriageModificationApplicator.apply(wf, mods);

    assertThat(result.parameterOverrides())
        .containsEntry("retryMode", "lenient")
        .containsEntry("timeout", 5000);
  }

  @Test
  void replaceStepInputWrapsWithInputInjection() {
    Workflow wf = workflow(setTask("fetch-data"), setTask("process"));
    var mods =
        List.<PlaybookTriageModification>of(
            new PlaybookTriageModification.ReplaceStepInput(
                "fetch-data", Map.of("source", "backup", "format", "csv")));

    var result = PlaybookTriageModificationApplicator.apply(wf, mods);

    TaskItem modified = result.workflow().getDo().getFirst();
    assertThat(modified.getTask().getDoTask()).isNotNull();
    DoTask wrapper = modified.getTask().getDoTask();
    assertThat(wrapper.getDo()).hasSize(2);
    assertThat(wrapper.getDo().getFirst().getTask().getSetTask()).isNotNull();
  }

  @Test
  void multipleModificationsCombined() {
    Workflow wf = workflow(setTask("validate"), setTask("fetch"), setTask("process"));
    var mods =
        List.<PlaybookTriageModification>of(
            new PlaybookTriageModification.SkipStep("validate"),
            new PlaybookTriageModification.ReplaceStepInput("fetch", Map.of("url", "/backup")),
            new PlaybookTriageModification.UpdateParameter("mode", "safe"));

    var result = PlaybookTriageModificationApplicator.apply(wf, mods);

    assertThat(result.workflow().getDo().get(0).getTask().getSetTask()).isNotNull();
    assertThat(result.workflow().getDo().get(1).getTask().getDoTask()).isNotNull();
    assertThat(result.workflow().getDo().get(2).getName()).isEqualTo("process");
    assertThat(result.parameterOverrides()).containsEntry("mode", "safe");
  }

  @Test
  void originalWorkflowNotMutated() {
    Workflow original = workflow(setTask("step-1"), setTask("step-2"));
    int originalSize = original.getDo().size();
    String originalFirstName = original.getDo().getFirst().getName();

    PlaybookTriageModificationApplicator.apply(
        original, List.of(new PlaybookTriageModification.SkipStep("step-1")));

    assertThat(original.getDo()).hasSize(originalSize);
    assertThat(original.getDo().getFirst().getName()).isEqualTo(originalFirstName);
  }
}
