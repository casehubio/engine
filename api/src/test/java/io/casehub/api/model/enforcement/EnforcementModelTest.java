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
package io.casehub.api.model.enforcement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.casehub.api.model.coordination.CommunicationMode;
import java.util.List;
import org.junit.jupiter.api.Test;

class EnforcementModelTest {

  private static final InteractionRule RULE =
      new InteractionRule("r-1", "one agent at a time", RuleSeverity.MANDATORY);

  @Test
  void turn_request_stores_fields() {
    var request = new TurnRequest("agent-1", "speak", CommunicationMode.BROADCAST);
    assertThat(request.agentId()).isEqualTo("agent-1");
    assertThat(request.actionType()).isEqualTo("speak");
    assertThat(request.communicationMode()).isEqualTo(CommunicationMode.BROADCAST);
  }

  @Test
  void turn_request_rejects_null_agentId() {
    assertThatThrownBy(() -> new TurnRequest(null, "speak", CommunicationMode.BROADCAST))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void turn_request_rejects_blank_agentId() {
    assertThatThrownBy(() -> new TurnRequest("  ", "speak", CommunicationMode.BROADCAST))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void turn_decision_permitted_stores_agentId() {
    var decision = new TurnDecision.Permitted("agent-1");
    assertThat(decision.agentId()).isEqualTo("agent-1");
  }

  @Test
  void turn_decision_denied_stores_reason() {
    var decision = new TurnDecision.Denied("agent-1", "not your turn");
    assertThat(decision.agentId()).isEqualTo("agent-1");
    assertThat(decision.reason()).isEqualTo("not your turn");
  }

  @Test
  void turn_decision_deferred_accepts_null_reason() {
    var decision = new TurnDecision.Deferred("agent-1", null);
    assertThat(decision.reason()).isNull();
  }

  @Test
  void turn_decision_is_sealed() {
    assertThat(TurnDecision.class).isSealed();
  }

  @Test
  void interaction_rule_stores_fields() {
    var rule =
        new InteractionRule("r-1", "one at a time", RuleSeverity.MANDATORY, "max_concurrent=1");
    assertThat(rule.ruleId()).isEqualTo("r-1");
    assertThat(rule.description()).isEqualTo("one at a time");
    assertThat(rule.severity()).isEqualTo(RuleSeverity.MANDATORY);
    assertThat(rule.constraint()).isEqualTo("max_concurrent=1");
  }

  @Test
  void interaction_rule_accepts_null_constraint() {
    var rule = new InteractionRule("r-1", "be polite", RuleSeverity.ADVISORY);
    assertThat(rule.constraint()).isNull();
  }

  @Test
  void interaction_rule_rejects_blank_ruleId() {
    assertThatThrownBy(() -> new InteractionRule(" ", "desc", RuleSeverity.WARNING))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void violation_stores_fields() {
    var violation = new Violation("agent-1", RULE, "spoke out of turn");
    assertThat(violation.agentId()).isEqualTo("agent-1");
    assertThat(violation.rule()).isEqualTo(RULE);
    assertThat(violation.detail()).isEqualTo("spoke out of turn");
  }

  @Test
  void violation_rejects_null_rule() {
    assertThatThrownBy(() -> new Violation("agent-1", null, "detail"))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void violation_response_warned_stores_fields() {
    var response = new ViolationResponse.Warned("agent-1", "please wait your turn");
    assertThat(response.agentId()).isEqualTo("agent-1");
    assertThat(response.message()).isEqualTo("please wait your turn");
  }

  @Test
  void violation_response_blocked_stores_fields() {
    var response = new ViolationResponse.Blocked("agent-1", "repeated violation");
    assertThat(response.agentId()).isEqualTo("agent-1");
    assertThat(response.reason()).isEqualTo("repeated violation");
  }

  @Test
  void violation_response_ejected_stores_fields() {
    var response = new ViolationResponse.Ejected("agent-1", "disruptive behavior");
    assertThat(response.agentId()).isEqualTo("agent-1");
    assertThat(response.reason()).isEqualTo("disruptive behavior");
  }

  @Test
  void violation_response_is_sealed() {
    assertThat(ViolationResponse.class).isSealed();
  }

  @Test
  void enforcement_lifecycle_event_is_sealed() {
    assertThat(EnforcementLifecycleEvent.class).isSealed();
  }

  @Test
  void lifecycle_activated_stores_fields() {
    var event =
        new EnforcementLifecycleEvent.Activated(
            "mod-1", CommunicationMode.GROUP_CHANNEL, List.of(RULE));
    assertThat(event.moderatorId()).isEqualTo("mod-1");
    assertThat(event.mode()).isEqualTo(CommunicationMode.GROUP_CHANNEL);
    assertThat(event.rules()).containsExactly(RULE);
  }

  @Test
  void lifecycle_activated_defensive_copies_rules() {
    var rules = new java.util.ArrayList<>(List.of(RULE));
    var event =
        new EnforcementLifecycleEvent.Activated("mod-1", CommunicationMode.BROADCAST, rules);
    rules.clear();
    assertThat(event.rules()).containsExactly(RULE);
  }

  @Test
  void lifecycle_deactivated_stores_reason() {
    var event = new EnforcementLifecycleEvent.Deactivated("mod-1", "session ended");
    assertThat(event.moderatorId()).isEqualTo("mod-1");
    assertThat(event.reason()).isEqualTo("session ended");
  }

  @Test
  void lifecycle_rules_updated_stores_both_rule_sets() {
    var oldRule = new InteractionRule("r-1", "old rule", RuleSeverity.ADVISORY);
    var newRule = new InteractionRule("r-2", "new rule", RuleSeverity.MANDATORY);
    var event =
        new EnforcementLifecycleEvent.RulesUpdated("mod-1", List.of(oldRule), List.of(newRule));
    assertThat(event.previousRules()).containsExactly(oldRule);
    assertThat(event.newRules()).containsExactly(newRule);
  }
}
