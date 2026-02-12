-- Create CaseHub table (definition/metadata)
CREATE TABLE IF NOT EXISTS casehub (
    id BIGSERIAL PRIMARY KEY,
    namespace VARCHAR(255),
    name VARCHAR(255) NOT NULL,
    version VARCHAR(50) NOT NULL,
    title VARCHAR(500),
    dsl VARCHAR(50),
    definition JSONB,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_casehub_namespace_name_version UNIQUE (namespace, name, version)
);

-- Create CaseHub Instance Run table (execution runs)
CREATE TABLE IF NOT EXISTS casehub_instance_run (
    id BIGSERIAL PRIMARY KEY,
    run_id UUID NOT NULL UNIQUE,
    casehub_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_casehub_instance_run_casehub FOREIGN KEY (casehub_id) REFERENCES casehub(id) ON DELETE CASCADE
);

-- Indexes for CaseHub table
CREATE INDEX IF NOT EXISTS idx_casehub_namespace ON casehub(namespace);
CREATE INDEX IF NOT EXISTS idx_casehub_namespace_name ON casehub(namespace, name);
CREATE INDEX IF NOT EXISTS idx_casehub_definition ON casehub USING gin(definition);

-- Indexes for CaseHub Instance Run table
CREATE INDEX IF NOT EXISTS idx_casehub_instance_run_run_id ON casehub_instance_run(run_id);
CREATE INDEX IF NOT EXISTS idx_casehub_instance_run_casehub_id ON casehub_instance_run(casehub_id);
CREATE INDEX IF NOT EXISTS idx_casehub_instance_run_status ON casehub_instance_run(status);
CREATE INDEX IF NOT EXISTS idx_casehub_instance_run_created_at ON casehub_instance_run(created_at);

-- Comments
COMMENT ON TABLE casehub IS 'Stores CaseHub definitions (metadata)';
COMMENT ON COLUMN casehub.namespace IS 'Namespace of the CaseHub definition';
COMMENT ON COLUMN casehub.name IS 'Name of the CaseHub definition';
COMMENT ON COLUMN casehub.version IS 'Semantic version of the CaseHub definition';
COMMENT ON COLUMN casehub.definition IS 'Full CaseHub definition stored as JSON';

COMMENT ON TABLE casehub_instance_run IS 'Stores execution runs of CaseHub definitions';
COMMENT ON COLUMN casehub_instance_run.run_id IS 'Unique identifier for this execution run';
COMMENT ON COLUMN casehub_instance_run.casehub_id IS 'Reference to the CaseHub definition';
COMMENT ON COLUMN casehub_instance_run.status IS 'Current status: CREATED, RUNNING, COMPLETED, FAILED, CANCELLED';
COMMENT ON COLUMN casehub_instance_run.started_at IS 'Timestamp when the run started';
COMMENT ON COLUMN casehub_instance_run.completed_at IS 'Timestamp when the run completed (or failed/cancelled)';
