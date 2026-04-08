package io.casehub.engine.internal.event;

import java.util.UUID;

public record SignalReceivedEvent(UUID caseId, String path, Object value) {

    public SignalReceivedEvent {
        if (caseId == null) {
            throw new IllegalArgumentException("caseId cannot be null");
        }
        if (path == null) {
            throw new IllegalArgumentException("path cannot be null");
        }
    }

    @Override
    public String toString() {
        return "SignalReceivedEvent{" +
                "caseId=" + caseId +
                ", path='" + path + '\'' +
                ", value=" + value +
                '}';
    }
}
