package io.casehub.engine;

import io.casehub.model.CaseHubDefinition;
import io.casehub.model.CaseDefinitionSpec;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class YamlSimpleCaseHubBeanTest {

  @Inject
  YamlSimpleCaseHubBean yamlSimpleCaseHubBean;

  //@Test
  public void test() {
    CaseHubDefinition def = yamlSimpleCaseHubBean.getDefinition();
    assertNotNull(def);

    assertEquals("0.1", def.getDsl());
    assertEquals("1.0.0", def.getVersion());
    assertEquals("Document Processing Test", def.getName());
    assertEquals("test", def.getNamespace());
    assertEquals("Test Case with Worker and Capability", def.getTitle());

    CaseDefinitionSpec spec = def.getSpec();
    assertNotNull(spec);

    // capabilities
    assertEquals(1, spec.getCapabilities().size());
    assertEquals("processDocument", spec.getCapabilities().get(0).getName());
    assertEquals("{ documentId: .documentId, status: .status }", spec.getCapabilities().get(0).getInputSchema());
    assertEquals("{ processedDocument: ., status: .status }", spec.getCapabilities().get(0).getOutputSchema());

    // workers
    assertEquals(1, spec.getWorkers().size());
    assertEquals("document-processor", spec.getWorkers().get(0).getName());
    assertEquals(1, spec.getWorkers().get(0).getCapabilities().size());
    assertEquals("processDocument", spec.getWorkers().get(0).getCapabilities().get(0));
    assertTrue(spec.getWorkers().get(0).isEmbeddedWorkflow());

    // rules
    assertEquals(1, spec.getRules().size());
    assertEquals("trigger-on-processing-status", spec.getRules().get(0).getName());
    assertEquals("processDocument", spec.getRules().get(0).getCapability());
    assertNotNull(spec.getRules().get(0).getOn().getContextChange());
    assertEquals(".status == \"processing\"", spec.getRules().get(0).getOn().getContextChange().getFilter());

    // milestones
    assertEquals(1, spec.getMilestones().size());
    assertEquals("documentProcessed", spec.getMilestones().get(0).getName());
    assertEquals(".status == \"processed\"", spec.getMilestones().get(0).getCondition());

    // goals
    assertEquals(1, spec.getGoals().size());
    assertEquals("documentProcessingComplete", spec.getGoals().get(0).getName());
    assertEquals(".status == \"processed\"", spec.getGoals().get(0).getCondition());

    // completion
    assertNotNull(spec.getCompletion());
    assertNotNull(spec.getCompletion().getSuccess());
    assertEquals(1, spec.getCompletion().getSuccess().getAllOf().size());
    assertEquals("documentProcessingComplete", spec.getCompletion().getSuccess().getAllOf().get(0));
  }

}
