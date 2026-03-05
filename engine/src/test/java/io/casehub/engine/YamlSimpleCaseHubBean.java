package io.casehub.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.casehub.api.CaseHub;
import io.casehub.api.model.AllOfGoalExpression;
import io.casehub.api.model.AnyOfGoalExpression;
import io.casehub.api.model.Capability;
import io.casehub.api.model.CaseHubDefinition;
import io.casehub.api.model.ContextChangeTrigger;
import io.casehub.api.model.DispatchRule;
import io.casehub.api.model.Goal;
import io.casehub.api.model.GoalBasedCompletion;
import io.casehub.api.model.GoalExpression;
import io.casehub.api.model.GoalKind;
import io.casehub.api.model.Milestone;
import io.casehub.api.model.Trigger;
import io.casehub.api.model.Worker;
import io.casehub.engine.internal.worker.WorkflowFunction;
import io.casehub.api.model.evaluator.JQExpressionEvaluator;
import io.serverlessworkflow.api.types.Workflow;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class YamlSimpleCaseHubBean extends CaseHub {

  private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

  @Override
  public CaseHubDefinition getDefinition() {
    try (InputStream is = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("casehub/simple.yaml")) {
      if (is == null) {
        throw new IllegalStateException("Resource casehub/simple.yaml not found on classpath");
      }
      io.casehub.model.CaseHubDefinition schema = YAML_MAPPER.readValue(is, io.casehub.model.CaseHubDefinition.class);
      return convertToApiModel(schema);
    } catch (IOException e) {
      throw new RuntimeException("Failed to load CaseHub definition from casehub/simple.yaml", e);
    }
  }

  private CaseHubDefinition convertToApiModel(io.casehub.model.CaseHubDefinition schema) {
    CaseHubDefinition def = new CaseHubDefinition(schema.getNamespace(), schema.getName(), schema.getVersion());
    def.setDsl(schema.getDsl());
    def.setTitle(schema.getTitle());

    // Convert capabilities
    Map<String, Capability> capabilityMap = new java.util.LinkedHashMap<>();
    if (schema.getSpec().getCapabilities() != null) {
      for (io.casehub.model.Capability sc : schema.getSpec().getCapabilities()) {
        Capability cap = new Capability(sc.getName(), sc.getInputSchema(), sc.getOutputSchema());
        cap.setDescription(sc.getDescription());
        capabilityMap.put(sc.getName(), cap);
        def.getCapabilities().add(cap);
      }
    }

    // Convert workers
    if (schema.getSpec().getWorkers() != null) {
      for (io.casehub.model.Worker sw : schema.getSpec().getWorkers()) {
        List<Capability> workerCaps = sw.getCapabilities().stream()
                .map(capabilityMap::get)
                .collect(Collectors.toList());

        WorkflowFunction wf = null;
        if (sw.isEmbeddedWorkflow()) {
          wf = new WorkflowFunction(sw.getWorkflowAsEmbedded());
        }

        Worker worker = new Worker(sw.getName(), workerCaps, null, wf);
        worker.setDescription(sw.getDescription());
        def.getWorkers().add(worker);
      }
    }

    // Convert rules
    if (schema.getSpec().getRules() != null) {
      for (io.casehub.model.DispatchRule sr : schema.getSpec().getRules()) {
        Capability cap = capabilityMap.get(sr.getCapability());
        Trigger trigger = null;
        if (sr.getOn() != null && sr.getOn().getContextChange() != null) {
          trigger = new ContextChangeTrigger(
                  new JQExpressionEvaluator(sr.getOn().getContextChange().getFilter()));
        }
        DispatchRule rule = new DispatchRule(sr.getName(), cap, trigger, null);
        def.getRules().add(rule);
      }
    }

    // Convert milestones
    if (schema.getSpec().getMilestones() != null) {
      for (io.casehub.model.Milestone sm : schema.getSpec().getMilestones()) {
        Milestone milestone = new Milestone(sm.getName(),
                new JQExpressionEvaluator(sm.getCondition()));
        milestone.setDescription(sm.getDescription());
        def.getMilestones().add(milestone);
      }
    }

    // Convert goals
    Map<String, Goal> goalMap = new java.util.LinkedHashMap<>();
    if (schema.getSpec().getGoals() != null) {
      for (io.casehub.model.Goal sg : schema.getSpec().getGoals()) {
        Goal goal = new Goal(sg.getName(),
                new JQExpressionEvaluator(sg.getCondition()), GoalKind.SUCCESS);
        goal.setDescription(sg.getDescription());
        goalMap.put(sg.getName(), goal);
        def.getGoals().add(goal);
      }
    }

    // Convert completion
    if (schema.getSpec().getCompletion() != null) {
      io.casehub.model.CaseCompletion sc = schema.getSpec().getCompletion();
      GoalExpression successExpr = convertGoalExpression(sc.getSuccess(), goalMap);
      GoalExpression failureExpr = convertGoalExpression(sc.getFailure(), goalMap);
      def.setCompletion(new GoalBasedCompletion(successExpr, failureExpr));
    }

    return def;
  }

  private GoalExpression convertGoalExpression(io.casehub.model.GoalExpression expr, Map<String, Goal> goalMap) {
    if (expr == null) return null;

    if (expr.getAllOf() != null && !expr.getAllOf().isEmpty()) {
      List<Goal> goals = expr.getAllOf().stream()
              .map(goalMap::get)
              .collect(Collectors.toList());
      return new AllOfGoalExpression(goals);
    }

    if (expr.getAnyOf() != null && !expr.getAnyOf().isEmpty()) {
      List<Goal> goals = expr.getAnyOf().stream()
              .map(goalMap::get)
              .collect(Collectors.toList());
      return new AnyOfGoalExpression(goals);
    }

    return null;
  }
}
