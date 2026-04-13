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
package io.casehub.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.casehub.api.model.AllOfGoalExpression;
import io.casehub.api.model.CaseHubDefinition;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.GoalBasedCompletion;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.quarkus.test.junit.QuarkusTest;
import io.serverlessworkflow.api.types.Workflow;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class YamlSimpleCaseHubBeanTest {

  @Inject YamlSimpleCaseHubBean yamlSimpleCaseHubBean;

  @Test
  public void test() {
    CaseHubDefinition def = yamlSimpleCaseHubBean.getDefinition();
    assertNotNull(def);

    assertEquals("0.1", def.getDsl());
    assertEquals("1.0.0", def.getVersion());
    assertEquals("Document Processing Test", def.getName());
    assertEquals("test", def.getNamespace());
    assertEquals("Test Case with Worker and Capability", def.getTitle());

    // capabilities
    assertEquals(1, def.getCapabilities().size());
    assertEquals("processDocument", def.getCapabilities().get(0).getName());
    assertEquals(
        "{ documentId: .documentId, status: .status }",
        def.getCapabilities().get(0).getInputSchema());
    assertEquals(
        "{ processedDocument: ., status: .status }",
        def.getCapabilities().get(0).getOutputSchema());

    // workers
    assertEquals(1, def.getWorkers().size());
    assertEquals("document-processor", def.getWorkers().get(0).getName());
    assertEquals(1, def.getWorkers().get(0).getCapabilities().size());
    assertEquals("processDocument", def.getWorkers().get(0).getCapabilities().get(0).getName());
    assertInstanceOf(Workflow.class, def.getWorkers().get(0).getFunction().getValue());

    // rules
    assertEquals(1, def.getRules().size());
    assertEquals("trigger-on-processing-status", def.getRules().get(0).getName());
    assertEquals("processDocument", def.getRules().get(0).getCapability().getName());
    assertInstanceOf(ContextChangeTrigger.class, def.getRules().get(0).getOn());
    ContextChangeTrigger cct = (ContextChangeTrigger) def.getRules().get(0).getOn();
    assertEquals(
        ".status == \"processing\"", ((JQExpressionEvaluator) cct.getFilter()).expression());

    // milestones
    assertEquals(1, def.getMilestones().size());
    assertEquals("documentProcessed", def.getMilestones().get(0).getName());
    assertEquals(
        ".status == \"processed\"",
        ((JQExpressionEvaluator) def.getMilestones().get(0).getCondition()).expression());

    // goals
    assertEquals(1, def.getGoals().size());
    assertEquals("documentProcessingComplete", def.getGoals().get(0).getName());
    assertEquals(
        ".status == \"processed\"",
        ((JQExpressionEvaluator) def.getGoals().get(0).getCondition()).expression());

    // completion
    assertNotNull(def.getCompletion());
    assertInstanceOf(GoalBasedCompletion.class, def.getCompletion());
    GoalBasedCompletion completion = (GoalBasedCompletion) def.getCompletion();
    assertNotNull(completion.getSuccess());
    assertInstanceOf(AllOfGoalExpression.class, completion.getSuccess());
    assertEquals(1, completion.getSuccess().getGoals().size());
    assertEquals(
        "documentProcessingComplete",
        completion.getSuccess().getGoals().iterator().next().getName());
  }
}
