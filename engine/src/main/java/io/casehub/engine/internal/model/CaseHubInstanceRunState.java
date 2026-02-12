package io.casehub.engine.internal.model;

import io.casehub.context.StateContext;

import java.util.UUID;

public class CaseHubInstanceRunState {

    private final CaseHubInstanceRun run;
    private final StateContext stateContext;

    public CaseHubInstanceRunState(CaseHubInstanceRun run, StateContext stateContext) {
        this.run = run;
        this.stateContext = stateContext;
    }

    public CaseHubInstanceRun getRun() {
        return run;
    }

    public StateContext getStateContext() {
        return stateContext;
    }

    public UUID getRunId() {
        return run.getRunId();
    }

    public CaseHub getCaseHub() {
        return run.getCaseHub();
    }

    public CaseHubInstanceRun.CaseHubStatus getStatus() {
        return run.getStatus();
    }

    @Override
    public String toString() {
        return "CaseHubInstanceRunState{" +
                "runId=" + getRunId() +
                ", status=" + getStatus() +
                ", stateContextSize=" + stateContext.size() +
                '}';
    }
}
