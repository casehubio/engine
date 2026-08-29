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
package io.casehub.engine.internal.worker;

import io.casehub.api.model.HumanRoutingConfig;
import io.casehub.api.model.HumanTaskTarget;
import io.casehub.api.model.JudgmentTarget;
import io.casehub.engine.common.spi.HumanTaskScheduleRequest;
import io.casehub.engine.common.spi.HumanTaskScheduler;
import io.casehub.engine.common.spi.JudgmentScheduleRequest;
import io.casehub.engine.common.spi.JudgmentScheduler;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@DefaultBean
@ApplicationScoped
public class DelegatingJudgmentScheduler implements JudgmentScheduler {

  private static final Logger LOG = Logger.getLogger(DelegatingJudgmentScheduler.class);

  @Inject Instance<HumanTaskScheduler> humanTaskScheduler;

  @Override
  public void schedule(JudgmentScheduleRequest request) {
    if (request.target().routingConfig() instanceof HumanRoutingConfig hrc
        && humanTaskScheduler.isResolvable()) {
      humanTaskScheduler.get().schedule(toHumanRequest(request, hrc));
      return;
    }
    LOG.debugf(
        "No routing config or no HumanTaskScheduler — judgment yield for caseId=%s binding=%s not dispatched",
        request.caseId(), request.bindingName());
  }

  private HumanTaskScheduleRequest toHumanRequest(
      JudgmentScheduleRequest req, HumanRoutingConfig hrc) {
    JudgmentTarget jt = req.target();
    HumanTaskTarget humanTarget = buildHumanTaskTarget(jt, hrc);
    return new HumanTaskScheduleRequest(
        req.caseId(),
        req.tenancyId(),
        req.bindingName(),
        humanTarget,
        req.inputData(),
        req.payloadTypeName(),
        req.resolutionTypeName(),
        req.resolvedCandidateGroups(),
        req.resolvedCandidateUsers(),
        req.caseBudgetDeadline(),
        req.expiresAtDeadline(),
        req.resolvedTitle(),
        req.resolvedScope(),
        req.experiences(),
        req.candidateScores());
  }

  private HumanTaskTarget buildHumanTaskTarget(JudgmentTarget jt, HumanRoutingConfig hrc) {
    HumanTaskTarget.Builder b =
        hrc.templateRef() != null
            ? HumanTaskTarget.template(hrc.templateRef())
            : HumanTaskTarget.inline();
    if (jt.title() != null) b.title(jt.title());
    if (jt.titleExpression() != null) b.titleExpression(jt.titleExpression());
    if (hrc.candidateGroups() != null) b.candidateGroups(hrc.candidateGroups());
    if (hrc.candidateUsers() != null) b.candidateUsers(hrc.candidateUsers());
    if (jt.expiresIn() != null) b.expiresIn(jt.expiresIn());
    if (jt.expiresInExpression() != null) b.expiresInExpression(jt.expiresInExpression());
    if (jt.expiresAtExpression() != null) b.expiresAtExpression(jt.expiresAtExpression());
    if (hrc.claimDeadlineHours() != null) b.claimDeadlineHours(hrc.claimDeadlineHours());
    if (jt.priority() != null) b.priority(jt.priority());
    if (jt.inputMapping() != null) b.inputMapping(jt.inputMapping());
    if (jt.outputMapping() != null) b.outputMapping(jt.outputMapping());
    if (jt.scope() != null) b.scope(jt.scope());
    if (jt.scopeExpression() != null) b.scopeExpression(jt.scopeExpression());
    if (jt.outcomes() != null && !jt.outcomes().isEmpty()) b.outcomes(jt.outcomes());
    if (hrc.payloadType() != null) b.payloadType(hrc.payloadType());
    if (jt.resolutionType() != null) b.resolutionType(jt.resolutionType());
    return b.build();
  }
}
