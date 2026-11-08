package io.casehub.api.view;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record CaseOverviewView(
    CaseInstanceView instance,
    JsonNode context,
    List<PlanItemView> planItems,
    GoalEvaluationView goals,
    EventLogPage recentEvents) {}
