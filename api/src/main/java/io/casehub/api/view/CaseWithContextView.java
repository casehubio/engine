package io.casehub.api.view;

import com.fasterxml.jackson.databind.JsonNode;

public record CaseWithContextView(CaseInstanceView instance, JsonNode context) {}
