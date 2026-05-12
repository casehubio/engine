-- Add waiting_for_work_id to case_instance for orchestrated work correlation.
-- Stores the idempotency hash of work submitted via WorkOrchestrator when a case
-- transitions to WAITING. Cleared when WorkflowExecutionCompletedHandler resumes
-- the case to RUNNING. Persisted so WAITING→RUNNING resumption survives JVM restart.
ALTER TABLE case_instance ADD COLUMN IF NOT EXISTS waiting_for_work_id VARCHAR(255);
