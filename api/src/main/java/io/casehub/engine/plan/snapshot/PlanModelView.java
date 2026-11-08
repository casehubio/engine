package io.casehub.engine.plan.snapshot;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PlanModelView(
    UUID caseId,
    List<AgendaItemView> agenda,
    String focus,
    String focusRationale,
    Map<String, Object> resourceBudget,
    List<SubCaseView> subCases,
    List<CompoundStatusView> compounds,
    Instant timestamp) {

  public record AgendaItemView(
      String planItemId, String bindingName, String status,
      String description, String targetType) {}

  public record SubCaseView(
      String caseDefinition, String namespace, String status) {}

  public record CompoundStatusView(
      String id, String name, String status,
      int childCount, int completedCount,
      CompletionSemanticsSnapshot completion) {}
}
