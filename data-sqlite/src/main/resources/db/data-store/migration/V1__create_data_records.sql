CREATE TABLE IF NOT EXISTS data_records (
    tenant_id   TEXT NOT NULL,
    app_id      TEXT NOT NULL,
    collection  TEXT NOT NULL,
    doc_id      TEXT NOT NULL,
    data        TEXT NOT NULL,
    _version    INTEGER NOT NULL DEFAULT 1,
    created_at  TEXT NOT NULL,
    updated_at  TEXT NOT NULL,
    PRIMARY KEY (tenant_id, app_id, collection, doc_id)
);

CREATE INDEX IF NOT EXISTS idx_data_records_collection
    ON data_records(tenant_id, app_id, collection);
