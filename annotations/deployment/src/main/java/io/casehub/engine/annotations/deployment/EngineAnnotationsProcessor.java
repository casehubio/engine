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
package io.casehub.engine.annotations.deployment;

import io.casehub.api.model.CaseDefinition;
import io.casehub.engine.annotations.PlanningMode;
import io.casehub.engine.annotations.runtime.BindingDescriptor;
import io.casehub.engine.annotations.runtime.CaseDefinitionRecorder;
import io.casehub.engine.annotations.runtime.CaseDescriptor;
import io.casehub.engine.annotations.runtime.GoalDescriptor;
import io.casehub.engine.annotations.runtime.GoapActionDescriptor;
import io.casehub.engine.annotations.runtime.MilestoneDescriptor;
import io.casehub.engine.annotations.runtime.WorkerDescriptor;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

public class EngineAnnotationsProcessor {

  private static final Logger LOG = Logger.getLogger(EngineAnnotationsProcessor.class);

  private static final DotName CASE = DotName.createSimple("io.casehub.engine.annotations.Case");
  private static final DotName WORKER =
      DotName.createSimple("io.casehub.engine.annotations.Worker");
  private static final DotName BIND = DotName.createSimple("io.casehub.engine.annotations.Bind");
  private static final DotName BINDINGS =
      DotName.createSimple("io.casehub.engine.annotations.Bindings");
  private static final DotName GOAL = DotName.createSimple("io.casehub.engine.annotations.Goal");
  private static final DotName MILESTONE =
      DotName.createSimple("io.casehub.engine.annotations.Milestone");
  private static final DotName EFFECT =
      DotName.createSimple("io.casehub.engine.annotations.Effect");
  private static final DotName SOFT_DEPENDENCY =
      DotName.createSimple("io.casehub.engine.annotations.SoftDependency");
  private static final DotName PARAM = DotName.createSimple("io.casehub.engine.annotations.Param");
  private static final DotName COMPLETION =
      DotName.createSimple("io.casehub.engine.annotations.Completion");
  private static final DotName CUSTOMIZE =
      DotName.createSimple("io.casehub.engine.annotations.Customize");
  private static final DotName SYSTEM_PROMPT =
      DotName.createSimple("io.casehub.engine.annotations.SystemPrompt");
  private static final DotName CAPABILITY =
      DotName.createSimple("io.casehub.engine.annotations.Capability");
  private static final DotName COST = DotName.createSimple("io.casehub.engine.annotations.Cost");
  private static final DotName COMPOUND =
      DotName.createSimple("io.casehub.engine.annotations.Compound");
  private static final DotName COMPOUNDS =
      DotName.createSimple("io.casehub.engine.annotations.Compounds");
  private static final DotName SUBCASE =
      DotName.createSimple("io.casehub.engine.annotations.SubCase");
  private static final DotName JUDGMENT =
      DotName.createSimple("io.casehub.engine.annotations.Judgment");

  @BuildStep
  @Record(ExecutionTime.RUNTIME_INIT)
  void generateCaseDefinitions(
      CombinedIndexBuildItem indexBuildItem,
      CaseDefinitionRecorder recorder,
      BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
      BuildProducer<io.quarkus.arc.deployment.UnremovableBeanBuildItem> unremovableBeans) {

    IndexView index = indexBuildItem.getIndex();
    String builderClassName = CaseDefinition.Builder.class.getName();

    for (AnnotationInstance caseAnn : index.getAnnotations(CASE)) {
      ClassInfo caseClass = caseAnn.target().asClass();
      CaseDescriptor descriptor = buildDescriptor(caseAnn, caseClass, index);

      if (descriptor.customizers() != null) {
        for (var cd : descriptor.customizers()) {
          for (String paramType : cd.parameterTypes()) {
            if (!paramType.equals(builderClassName)) {
              unremovableBeans.produce(
                  io.quarkus.arc.deployment.UnremovableBeanBuildItem.beanClassNames(paramType));
            }
          }
        }
      }

      java.util.function.Supplier<CaseDefinition> supplier =
          recorder.createCaseDefinitionSupplier(descriptor);

      syntheticBeans.produce(
          SyntheticBeanBuildItem.configure(CaseDefinition.class)
              .scope(ApplicationScoped.class)
              .unremovable()
              .setRuntimeInit()
              .supplier(supplier)
              .done());
    }
  }

  @BuildStep
  void generateCaseImplementations(
      CombinedIndexBuildItem indexBuildItem,
      BuildProducer<GeneratedClassBuildItem> generatedClasses) {

    IndexView index = indexBuildItem.getIndex();

    for (AnnotationInstance caseAnn : index.getAnnotations(CASE)) {
      ClassInfo caseClass = caseAnn.target().asClass();
      String implClassName = caseClass.name().toString() + "_CaseHubImpl";

      try (ClassCreator creator =
          ClassCreator.builder()
              .classOutput(new GeneratedClassGizmoAdaptor(generatedClasses, true))
              .className(implClassName)
              .interfaces(caseClass.name().toString())
              .build()) {

        try (MethodCreator ctor = creator.getMethodCreator("<init>", void.class)) {
          ctor.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), ctor.getThis());
          ctor.returnVoid();
        }
      }
    }
  }

  private CaseDescriptor buildDescriptor(
      AnnotationInstance caseAnn, ClassInfo caseClass, IndexView index) {

    String namespace = caseAnn.value("namespace").asString();
    String name = caseAnn.value("name").asString();
    String version = stringValueOrDefault(caseAnn, index, "version", "1.0.0");
    String title = stringValueOrDefault(caseAnn, index, "title", "");
    String summary = stringValueOrDefault(caseAnn, index, "summary", "");

    PlanningMode planning =
        PlanningMode.valueOf(stringValueOrDefault(caseAnn, index, "planning", "EXPLICIT"));

    String planningStrategy = null;
    if (planning == PlanningMode.GOAP) {
      planningStrategy = "goap";
    } else if (planning == PlanningMode.ADAPTIVE) {
      planningStrategy = "adaptive";
    }

    List<WorkerDescriptor> workers = new ArrayList<>();
    List<BindingDescriptor> bindings = new ArrayList<>();
    List<GoalDescriptor> goals = new ArrayList<>();
    List<MilestoneDescriptor> milestones = new ArrayList<>();
    List<GoapActionDescriptor> goapActions = new ArrayList<>();
    Map<String, List<String>> goalToEffectKeys = new HashMap<>();
    List<io.casehub.engine.annotations.runtime.CompletionDescriptor> completions =
        new ArrayList<>();
    List<io.casehub.engine.annotations.runtime.CustomizerDescriptor> customizers =
        new ArrayList<>();
    List<String> standaloneCapabilities = new ArrayList<>();
    List<io.casehub.engine.annotations.runtime.CompoundDescriptor> compoundDescriptors =
        new ArrayList<>();
    List<io.casehub.engine.annotations.runtime.SubCaseDescriptor> subCaseDescriptors =
        new ArrayList<>();
    List<io.casehub.engine.annotations.runtime.JudgmentDescriptor> judgmentDescriptors =
        new ArrayList<>();

    List<AnnotationInstance> compoundAnns = new ArrayList<>();
    AnnotationInstance singleCompound = caseClass.annotation(COMPOUND);
    if (singleCompound != null) compoundAnns.add(singleCompound);
    AnnotationInstance compoundsContainer = caseClass.annotation(COMPOUNDS);
    if (compoundsContainer != null) {
      compoundAnns.clear();
      for (AnnotationInstance nested : compoundsContainer.value().asNestedArray()) {
        compoundAnns.add(nested);
      }
    }
    for (AnnotationInstance ca : compoundAnns) {
      String compName = ca.value("name").asString();
      String[] workerNames = ca.value("workers").asStringArray();
      String compSemantics = stringValueOrDefault(ca, index, "completionSemantics", "all");
      String compDispatch = stringValueOrDefault(ca, index, "dispatchMode", "CHOREOGRAPHED");
      String compParticipation = stringValueOrDefault(ca, index, "participation", "PARTICIPANT");
      boolean compRepeatable = booleanValueOrDefault(ca, index, "repeatable", false);
      String compStrategy = stringValueOrDefault(ca, index, "planningStrategy", "");
      compoundDescriptors.add(
          new io.casehub.engine.annotations.runtime.CompoundDescriptor(
              compName,
              List.of(workerNames),
              compSemantics,
              compDispatch,
              compParticipation,
              compRepeatable,
              compStrategy.isEmpty() ? null : compStrategy));
    }

    for (MethodInfo method : caseClass.methods()) {
      AnnotationInstance workerAnn = method.annotation(WORKER);
      if (workerAnn != null) {
        processWorkerMethod(
            method, workerAnn, index, planning, workers, bindings, goapActions, goalToEffectKeys);

        AnnotationInstance systemPromptAnn = method.annotation(SYSTEM_PROMPT);
        if (systemPromptAnn != null) {
          var prev = workers.get(workers.size() - 1);
          workers.set(
              workers.size() - 1,
              new WorkerDescriptor(
                  prev.name(),
                  prev.capabilityName(),
                  prev.description(),
                  prev.methodName(),
                  prev.params(),
                  prev.returnTypeName(),
                  prev.effectKey(),
                  systemPromptAnn.value().asString(),
                  prev.scope(),
                  prev.participation(),
                  prev.executionMode()));
        }
      }

      AnnotationInstance goalAnn = method.annotation(GOAL);
      if (goalAnn != null) {
        processGoalMethod(method, goalAnn, index, goals);
      }

      AnnotationInstance milestoneAnn = method.annotation(MILESTONE);
      if (milestoneAnn != null) {
        processMilestoneMethod(method, milestoneAnn, index, milestones);
      }

      AnnotationInstance completionAnn = method.annotation(COMPLETION);
      if (completionAnn != null) {
        String kind = stringValueOrDefault(completionAnn, index, "kind", "SUCCESS");
        completions.add(
            new io.casehub.engine.annotations.runtime.CompletionDescriptor(method.name(), kind));
      }

      AnnotationInstance customizeAnn = method.annotation(CUSTOMIZE);
      if (customizeAnn != null) {
        String targetBinding = stringValueOrDefault(customizeAnn, index, "value", "");
        List<String> paramTypes =
            method.parameterTypes().stream().map(t -> t.name().toString()).toList();
        customizers.add(
            new io.casehub.engine.annotations.runtime.CustomizerDescriptor(
                method.name(),
                targetBinding.isEmpty() ? null : targetBinding,
                caseClass.name().toString(),
                paramTypes));
      }

      AnnotationInstance subCaseAnn = method.annotation(SUBCASE);
      if (subCaseAnn != null) {
        String scNamespace = stringValueOrDefault(subCaseAnn, index, "namespace", "");
        String scName = subCaseAnn.value("name").asString();
        String scVersion = stringValueOrDefault(subCaseAnn, index, "version", "");
        String scInput = stringValueOrDefault(subCaseAnn, index, "inputMapping", ".");
        String scOutput = stringValueOrDefault(subCaseAnn, index, "outputMapping", ".");
        int scDepth =
            subCaseAnn.valueWithDefault(index, "maxRecursionDepth") != null
                ? subCaseAnn.valueWithDefault(index, "maxRecursionDepth").asInt()
                : 0;
        List<AnnotationInstance> scBindAnns = collectBindAnnotations(method);
        String scTriggerType = "contextChange";
        String scTriggerValue = "true";
        String scWhen = null;
        if (!scBindAnns.isEmpty()) {
          AnnotationInstance scBind = scBindAnns.get(0);
          String cc = stringValueOrDefault(scBind, index, "contextChange", "");
          if (!cc.isEmpty()) {
            scTriggerType = "contextChange";
            scTriggerValue = cc;
          }
          String w = stringValueOrDefault(scBind, index, "when", "");
          if (!w.isEmpty()) scWhen = w;
        }
        subCaseDescriptors.add(
            new io.casehub.engine.annotations.runtime.SubCaseDescriptor(
                method.name(),
                scNamespace.isEmpty() ? null : scNamespace,
                scName,
                scVersion.isEmpty() ? null : scVersion,
                scInput,
                scOutput,
                scDepth,
                scTriggerType,
                scTriggerValue,
                scWhen));
      }

      AnnotationInstance judgmentAnn = method.annotation(JUDGMENT);
      if (judgmentAnn != null) {
        AnnotationValue jgGroups = judgmentAnn.value("candidateGroups");
        AnnotationValue jgUsers = judgmentAnn.value("candidateUsers");
        String jgTitle = stringValueOrDefault(judgmentAnn, index, "title", "");
        AnnotationValue jgOutcomes = judgmentAnn.value("outcomes");
        List<AnnotationInstance> jgBindAnns = collectBindAnnotations(method);
        String jgTriggerType = "contextChange";
        String jgTriggerValue = "true";
        String jgWhen = null;
        if (!jgBindAnns.isEmpty()) {
          AnnotationInstance jgBind = jgBindAnns.get(0);
          String cc = stringValueOrDefault(jgBind, index, "contextChange", "");
          if (!cc.isEmpty()) {
            jgTriggerType = "contextChange";
            jgTriggerValue = cc;
          }
          String w = stringValueOrDefault(jgBind, index, "when", "");
          if (!w.isEmpty()) jgWhen = w;
        }
        judgmentDescriptors.add(
            new io.casehub.engine.annotations.runtime.JudgmentDescriptor(
                method.name(),
                jgGroups != null ? List.of(jgGroups.asStringArray()) : List.of(),
                jgUsers != null ? List.of(jgUsers.asStringArray()) : List.of(),
                jgTitle.isEmpty() ? null : jgTitle,
                jgOutcomes != null ? List.of(jgOutcomes.asStringArray()) : List.of(),
                jgTriggerType,
                jgTriggerValue,
                jgWhen));
      }

      AnnotationInstance capAnn = method.annotation(CAPABILITY);
      if (capAnn != null && workerAnn == null) {
        String capName =
            capAnn.value("name") != null && !capAnn.value("name").asString().isEmpty()
                ? capAnn.value("name").asString()
                : method.name();
        standaloneCapabilities.add(capName);
      }
    }

    for (GoalDescriptor gd : goals) {
      if (gd.condition() != null) {
        java.util.Set<String> effectKeys =
            io.casehub.engine.annotations.runtime.GoalConditionParser.parseEffectKeys(
                gd.condition());
        if (!effectKeys.isEmpty()) {
          goalToEffectKeys.put(gd.name(), new ArrayList<>(effectKeys));
        }
      }
    }

    // Scan @Cost methods and attach to matching GOAP actions
    Map<String, Integer> capNameToActionIndex = new HashMap<>();
    for (int i = 0; i < goapActions.size(); i++) {
      capNameToActionIndex.put(goapActions.get(i).name(), i);
    }
    for (MethodInfo method : caseClass.methods()) {
      AnnotationInstance costAnn = method.annotation(COST);
      if (costAnn != null) {
        String targetWorker = costAnn.value().asString();
        Integer actionIndex = capNameToActionIndex.get(targetWorker);
        if (actionIndex != null) {
          GoapActionDescriptor old = goapActions.get(actionIndex);
          goapActions.set(
              actionIndex,
              new GoapActionDescriptor(
                  old.name(),
                  old.preconditions(),
                  old.effects(),
                  old.cost(),
                  old.benefit(),
                  old.softPreconditions(),
                  method.name()));
        }
      }
    }

    String implClassName = caseClass.name().toString() + "_CaseHubImpl";

    return new CaseDescriptor(
        namespace,
        name,
        version,
        title,
        summary,
        planningStrategy,
        implClassName,
        caseClass.name().toString(),
        workers,
        bindings,
        goals,
        milestones,
        goapActions.isEmpty() ? null : goapActions,
        goalToEffectKeys.isEmpty() ? null : goalToEffectKeys,
        completions.isEmpty() ? null : completions,
        customizers.isEmpty() ? null : customizers,
        standaloneCapabilities.isEmpty() ? null : standaloneCapabilities,
        compoundDescriptors.isEmpty() ? null : compoundDescriptors,
        subCaseDescriptors.isEmpty() ? null : subCaseDescriptors,
        judgmentDescriptors.isEmpty() ? null : judgmentDescriptors);
  }

  private void processWorkerMethod(
      MethodInfo method,
      AnnotationInstance workerAnn,
      IndexView index,
      PlanningMode planning,
      List<WorkerDescriptor> workers,
      List<BindingDescriptor> bindings,
      List<GoapActionDescriptor> goapActions,
      Map<String, List<String>> goalToEffectKeys) {

    String capabilityName = resolveCapabilityName(workerAnn, method, index);
    String description = stringValueOrDefault(workerAnn, index, "description", "");

    List<io.casehub.engine.annotations.runtime.WorkerParamDescriptor> params = new ArrayList<>();
    for (MethodParameterInfo param : method.parameters()) {
      Type paramType = param.type();
      if (paramType.name().toString().equals("io.casehub.worker.api.WorkerScope")) continue;
      String paramName = param.name() != null ? param.name() : "arg" + params.size();
      AnnotationInstance paramAnn = param.annotation(PARAM);
      String contextKey = paramAnn != null ? paramAnn.value().asString() : paramName;
      params.add(
          new io.casehub.engine.annotations.runtime.WorkerParamDescriptor(
              paramName, contextKey, paramType.name().toString()));
    }

    String returnTypeName = null;
    String effectKey = null;
    Type returnType = method.returnType();
    if (returnType.kind() != Type.Kind.VOID) {
      returnTypeName = returnType.name().toString();
      AnnotationInstance effectAnn = method.annotation(EFFECT);
      effectKey =
          effectAnn != null
              ? effectAnn.value().asString()
              : lowerCamelCase(returnType.name().local());
    }

    String scope = stringValueOrDefault(workerAnn, index, "scope", "BINDING");
    String participationVal =
        stringValueOrDefault(workerAnn, index, "participation", "PARTICIPANT");
    String executionModeVal = stringValueOrDefault(workerAnn, index, "executionMode", "TRANSIENT");

    workers.add(
        new WorkerDescriptor(
            method.name(),
            capabilityName,
            description,
            method.name(),
            params.isEmpty() ? null : params,
            returnTypeName,
            effectKey,
            null,
            scope,
            participationVal,
            executionModeVal));

    List<AnnotationInstance> bindAnns = collectBindAnnotations(method);
    if (!bindAnns.isEmpty()) {
      for (AnnotationInstance bindAnn : bindAnns) {
        bindings.add(processBindAnnotation(method, bindAnn, capabilityName, index));
      }
    } else if (planning == PlanningMode.GOAP || planning == PlanningMode.ADAPTIVE) {
      bindings.add(
          new BindingDescriptor(
              method.name(), capabilityName, "contextChange", "true", null, null, null));
    }

    if (planning == PlanningMode.GOAP || planning == PlanningMode.ADAPTIVE) {
      double cost =
          workerAnn.valueWithDefault(index, "cost") != null
              ? workerAnn.valueWithDefault(index, "cost").asDouble()
              : 0.0;
      double benefit =
          workerAnn.valueWithDefault(index, "benefit") != null
              ? workerAnn.valueWithDefault(index, "benefit").asDouble()
              : 0.0;
      goapActions.add(inferGoapAction(method, capabilityName, cost, benefit));
    }
  }

  private BindingDescriptor processBindAnnotation(
      MethodInfo method, AnnotationInstance bindAnn, String capabilityName, IndexView index) {

    String contextChange = stringValueOrDefault(bindAnn, index, "contextChange", "");
    String cron = stringValueOrDefault(bindAnn, index, "cron", "");
    boolean scopeActivated = booleanValueOrDefault(bindAnn, index, "scopeActivated", false);
    String when = stringValueOrDefault(bindAnn, index, "when", "");

    String triggerType;
    String triggerValue;

    if (!contextChange.isEmpty()) {
      triggerType = "contextChange";
      triggerValue = contextChange;
    } else if (!cron.isEmpty()) {
      triggerType = "cron";
      triggerValue = cron;
    } else if (scopeActivated) {
      triggerType = "scopeActivated";
      triggerValue = null;
    } else {
      triggerType = "contextChange";
      triggerValue = "true";
    }

    String conflictStrategy = stringValueOrDefault(bindAnn, index, "conflictStrategy", "");
    AnnotationValue producedKeysValue = bindAnn.value("producedKeys");
    String[] producedKeysArr =
        producedKeysValue != null ? producedKeysValue.asStringArray() : new String[0];
    java.util.List<String> producedKeys =
        producedKeysArr.length > 0 ? java.util.List.of(producedKeysArr) : null;

    return new BindingDescriptor(
        method.name(),
        capabilityName,
        triggerType,
        triggerValue,
        when.isEmpty() ? null : when,
        conflictStrategy.isEmpty() ? null : conflictStrategy,
        producedKeys);
  }

  private void processGoalMethod(
      MethodInfo method, AnnotationInstance goalAnn, IndexView index, List<GoalDescriptor> goals) {

    String description = goalAnn.value().asString();
    String condition = stringValueOrDefault(goalAnn, index, "condition", "");
    String kind = stringValueOrDefault(goalAnn, index, "kind", "SUCCESS");

    goals.add(
        new GoalDescriptor(
            method.name(), description, condition.isEmpty() ? null : condition, kind));
  }

  private void processMilestoneMethod(
      MethodInfo method,
      AnnotationInstance milestoneAnn,
      IndexView index,
      List<MilestoneDescriptor> milestones) {

    String name = milestoneAnn.value("name").asString();
    String completionCriteria = stringValueOrDefault(milestoneAnn, index, "completionCriteria", "");
    String entryCriteria = stringValueOrDefault(milestoneAnn, index, "entryCriteria", "");

    milestones.add(
        new MilestoneDescriptor(
            name,
            completionCriteria.isEmpty() ? null : completionCriteria,
            entryCriteria.isEmpty() ? null : entryCriteria));
  }

  private GoapActionDescriptor inferGoapAction(
      MethodInfo method, String name, double cost, double benefit) {

    Map<String, Boolean> preconditions = new HashMap<>();
    Map<String, Boolean> softPreconditions = new HashMap<>();

    for (MethodParameterInfo param : method.parameters()) {
      Type paramType = param.type();
      if (isInputParameterType(paramType)) continue;
      if (param.hasAnnotation(PARAM)) continue;

      String key = lowerCamelCase(paramType.name().local());
      if (param.hasAnnotation(SOFT_DEPENDENCY)) {
        softPreconditions.put(key, true);
      } else {
        preconditions.put(key, true);
      }
    }

    Map<String, Boolean> effects = new HashMap<>();
    Type returnType = method.returnType();
    if (returnType.kind() != Type.Kind.VOID) {
      AnnotationInstance effectAnn = method.annotation(EFFECT);
      String effectKey =
          effectAnn != null
              ? effectAnn.value().asString()
              : lowerCamelCase(returnType.name().local());
      effects.put(effectKey, true);
    }

    return new GoapActionDescriptor(
        name, preconditions, effects, cost, benefit, softPreconditions, null);
  }

  private List<AnnotationInstance> collectBindAnnotations(MethodInfo method) {
    List<AnnotationInstance> result = new ArrayList<>();
    AnnotationInstance single = method.annotation(BIND);
    if (single != null) {
      result.add(single);
    }
    AnnotationInstance container = method.annotation(BINDINGS);
    if (container != null) {
      result.clear();
      for (AnnotationInstance nested : container.value().asNestedArray()) {
        result.add(nested);
      }
    }
    return result;
  }

  private String resolveCapabilityName(
      AnnotationInstance workerAnn, MethodInfo method, IndexView index) {
    String value = stringValueOrDefault(workerAnn, index, "value", "");
    if (!value.isEmpty()) return value;
    String cap = stringValueOrDefault(workerAnn, index, "capability", "");
    if (!cap.isEmpty()) return cap;
    return method.name();
  }

  private boolean isInputParameterType(Type type) {
    String name = type.name().toString();
    return name.equals("java.lang.String")
        || name.equals("java.util.Map")
        || name.equals("int")
        || name.equals("long")
        || name.equals("double")
        || name.equals("float")
        || name.equals("boolean")
        || name.equals("byte")
        || name.equals("short")
        || name.equals("char")
        || name.equals("java.lang.Integer")
        || name.equals("java.lang.Long")
        || name.equals("java.lang.Double")
        || name.equals("java.lang.Float")
        || name.equals("java.lang.Boolean")
        || name.equals("io.casehub.worker.api.WorkerScope");
  }

  private static String lowerCamelCase(String simpleName) {
    if (simpleName == null || simpleName.isEmpty()) return simpleName;
    return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
  }

  private static String stringValueOrDefault(
      AnnotationInstance ann, IndexView index, String name, String defaultValue) {
    AnnotationValue value = ann.valueWithDefault(index, name);
    if (value == null) return defaultValue;
    String s = value.asString();
    return s != null ? s : defaultValue;
  }

  private static boolean booleanValueOrDefault(
      AnnotationInstance ann, IndexView index, String name, boolean defaultValue) {
    AnnotationValue value = ann.valueWithDefault(index, name);
    if (value == null) return defaultValue;
    return value.asBoolean();
  }
}
