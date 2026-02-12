package io.casehub.engine.internal.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.engine.internal.model.CaseHubInstanceRunState;
import io.casehub.model.Capability;
import io.casehub.model.Worker;
import io.serverlessworkflow.api.types.Workflow;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Event fired when a Worker should be executed.
 * Contains all necessary data to run the workflow including input data.
 *
 * Idempotency: The event uses a deterministic key based on runId, worker, capability, and input data hash.
 * This ensures that if the same event is re-published (e.g., during restate), it will have the same
 * idempotency key and can be deduplicated.
 */
public class WorkerRunEvent {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CaseHubInstanceRunState runState;
    private final Worker worker;
    private final Capability capability;
    private final Workflow workflow;
    private final Map<String, Object> inputData;
    private final String inputDataHash;

    /**
     * Creates a WorkerRunEvent with deterministic idempotency key.
     *
     * @param runState the current run state
     * @param worker the worker to be executed
     * @param capability the capability being invoked
     * @param workflow the workflow configuration
     * @param inputData the input data for the workflow
     */
    public WorkerRunEvent(CaseHubInstanceRunState runState,
                          Worker worker,
                          Capability capability,
                          Workflow workflow,
                          Map<String, Object> inputData) {
        this.runState = Objects.requireNonNull(runState, "runState cannot be null");
        this.worker = Objects.requireNonNull(worker, "worker cannot be null");
        this.capability = Objects.requireNonNull(capability, "capability cannot be null");
        this.workflow = Objects.requireNonNull(workflow, "workflow cannot be null");
        this.inputData = Objects.requireNonNull(inputData, "inputData cannot be null");
        this.inputDataHash = computeInputDataHash(inputData);
    }

    /**
     * Computes a deterministic SHA-256 hash of the input data.
     * This hash is used in the idempotency key to ensure that the same input data
     * always produces the same key, enabling deduplication during restate.
     *
     * @param inputData the input data to hash
     * @return hex-encoded SHA-256 hash
     */
    private static String computeInputDataHash(Map<String, Object> inputData) {
        try {
            // Serialize to JSON for deterministic string representation
            String json = OBJECT_MAPPER.writeValueAsString(inputData);

            // Compute SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute input data hash", e);
        }
    }

    public CaseHubInstanceRunState getRunState() {
        return runState;
    }

    public UUID getRunId() {
        return runState.getRunId();
    }

    public Worker getWorker() {
        return worker;
    }

    public Capability getCapability() {
        return capability;
    }

    public Workflow getWorkflow() {
        return workflow;
    }

    public Map<String, Object> getInputData() {
        return inputData;
    }

    /**
     * Returns a deterministic execution ID derived from the idempotency key.
     * This ensures that the same event (same runId, worker, capability, input data)
     * always produces the same execution ID, enabling idempotent processing.
     *
     * @return deterministic execution ID
     */
    public UUID getExecutionId() {
        return UUID.nameUUIDFromBytes(getIdempotencyKey().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns the deterministic idempotency key for this workflow execution.
     * The key consists of: runId + worker name + capability name + input data hash
     *
     * Key properties:
     * - Deterministic: same input always produces same key
     * - Unique: different inputs produce different keys
     * - Restate-safe: re-publishing the same event produces the same key
     *
     * @return deterministic idempotency key
     */
    public String getIdempotencyKey() {
        return String.format("%s:%s:%s:%s",
                runState.getRunId(),
                worker.getName(),
                capability.getName(),
                inputDataHash);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkerRunEvent that = (WorkerRunEvent) o;
        return Objects.equals(runState.getRunId(), that.runState.getRunId())
            && Objects.equals(worker.getName(), that.worker.getName())
            && Objects.equals(capability.getName(), that.capability.getName())
            && Objects.equals(inputDataHash, that.inputDataHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(runState.getRunId(), worker.getName(), capability.getName(), inputDataHash);
    }

    @Override
    public String toString() {
        return "WorkerRunEvent{" +
                "runId=" + runState.getRunId() +
                ", worker='" + worker.getName() + '\'' +
                ", capability='" + capability.getName() + '\'' +
                ", inputDataKeys=" + inputData.keySet() +
                ", inputDataHash='" + inputDataHash.substring(0, 8) + "...'" +
                ", executionId=" + getExecutionId() +
                ", idempotencyKey='" + getIdempotencyKey() + '\'' +
                '}';
    }
}
