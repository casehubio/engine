package io.casehub.engine.internal.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public final class WorkerExecutionKeys {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private WorkerExecutionKeys() {
    }

    public static String inputDataHash(Map<String, Object> inputData) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(inputData);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute input data hash", e);
        }
    }

    public static String inputDataHash(String workerName, String capabilityName, Map<String, Object> inputData) {
        return inputDataHash(workerName, capabilityName, inputDataHash(inputData));
    }

    public static String inputDataHash(String workerName, String capabilityName, String inputDataHash) {
        return workerName + ":" + capabilityName + ":" + inputDataHash;
    }
}
