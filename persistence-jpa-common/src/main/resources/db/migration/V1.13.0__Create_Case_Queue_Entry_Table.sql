CREATE TABLE IF NOT EXISTS case_queue_entry (
    id              UUID PRIMARY KEY,
    case_id         UUID         NOT NULL,
    tenancy_id      VARCHAR(64)  NOT NULL,
    view_id         UUID         NOT NULL,
    view_name       VARCHAR(255),
    status          VARCHAR(20)  NOT NULL,
    assigned_to     VARCHAR(255),
    claimed_at      TIMESTAMP,
    escalated_at    TIMESTAMP,
    previous_view_id   UUID,
    previous_view_name VARCHAR(255),
    created_at      TIMESTAMP    NOT NULL,
    CONSTRAINT uq_cqe_case_view UNIQUE (case_id, view_id)
);

CREATE INDEX IF NOT EXISTS idx_cqe_case_id ON case_queue_entry (case_id);
CREATE INDEX IF NOT EXISTS idx_cqe_view_id_tenancy ON case_queue_entry (view_id, tenancy_id);
CREATE INDEX IF NOT EXISTS idx_cqe_tenancy_id ON case_queue_entry (tenancy_id);
