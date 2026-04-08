package io.casehub.engine.internal.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.casehub.api.context.StateContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StateContextImpl.applyDiff")
class StateContextImplApplyDiffTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonNode patch(String json) throws Exception {
        return MAPPER.readTree(json);
    }

    // ------------------------------------------------------------------ //
    //  Helpers: roundtrip via snapshot().diff()                           //
    // ------------------------------------------------------------------ //

    /** Produces a patch that transforms {@code before} into {@code after}. */
    private JsonNode diffFromSnapshots(StateContextImpl before, StateContextImpl after) {
        return before.snapshot().diff(after);
    }

    // ================================================================== //

    @Nested
    @DisplayName("add operations")
    class AddOperations {

        @Test
        @DisplayName("should add a new string field to an empty context")
        void addStringToEmpty() throws Exception {
            StateContextImpl ctx = new StateContextImpl();
            ctx.applyDiff(patch("[{\"op\":\"add\",\"path\":\"/status\",\"value\":\"active\"}]"));

            assertEquals("active", ctx.getString("status"));
        }

        @Test
        @DisplayName("should add a new integer field")
        void addInteger() throws Exception {
            StateContextImpl ctx = new StateContextImpl();
            ctx.applyDiff(patch("[{\"op\":\"add\",\"path\":\"/amount\",\"value\":42}]"));

            assertEquals(42, ctx.getInt("amount"));
        }

        @Test
        @DisplayName("should add a boolean field")
        void addBoolean() throws Exception {
            StateContextImpl ctx = new StateContextImpl();
            ctx.applyDiff(patch("[{\"op\":\"add\",\"path\":\"/approved\",\"value\":true}]"));

            assertTrue(ctx.getBoolean("approved"));
        }

        @Test
        @DisplayName("should add a null field")
        void addNull() throws Exception {
            StateContextImpl ctx = new StateContextImpl(Map.of("x", "y"));
            ctx.applyDiff(patch("[{\"op\":\"add\",\"path\":\"/empty\",\"value\":null}]"));

            assertTrue(ctx.contains("empty"));
            assertNull(ctx.get("empty"));
        }

        @Test
        @DisplayName("should add a nested object field")
        void addNestedObject() throws Exception {
            StateContextImpl ctx = new StateContextImpl();
            ctx.applyDiff(patch("""
                    [{"op":"add","path":"/payment","value":{"amount":500,"currency":"USD"}}]
                    """));

            @SuppressWarnings("unchecked")
            Map<String, Object> payment = (Map<String, Object>) ctx.get("payment");
            assertEquals(500, payment.get("amount"));
            assertEquals("USD", payment.get("currency"));
        }

        @Test
        @DisplayName("should add a list field")
        void addList() throws Exception {
            StateContextImpl ctx = new StateContextImpl();
            ctx.applyDiff(patch("[{\"op\":\"add\",\"path\":\"/tags\",\"value\":[\"a\",\"b\"]}]"));

            List<?> tags = ctx.getList("tags", Object.class);
            assertEquals(2, tags.size());
            assertEquals("a", tags.get(0));
        }

        @Test
        @DisplayName("should add multiple fields in one patch")
        void addMultipleFields() throws Exception {
            StateContextImpl ctx = new StateContextImpl();
            ctx.applyDiff(patch("""
                    [
                      {"op":"add","path":"/status","value":"pending"},
                      {"op":"add","path":"/count","value":3}
                    ]
                    """));

            assertEquals("pending", ctx.getString("status"));
            assertEquals(3, ctx.getInt("count"));
        }
    }

    // ================================================================== //

    @Nested
    @DisplayName("replace operations")
    class ReplaceOperations {

        @Test
        @DisplayName("should replace an existing string field")
        void replaceString() throws Exception {
            StateContextImpl ctx = new StateContextImpl(Map.of("status", "pending"));
            ctx.applyDiff(patch("[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"active\"}]"));

            assertEquals("active", ctx.getString("status"));
        }

        @Test
        @DisplayName("should replace an integer field")
        void replaceInteger() throws Exception {
            StateContextImpl ctx = new StateContextImpl(Map.of("count", 1));
            ctx.applyDiff(patch("[{\"op\":\"replace\",\"path\":\"/count\",\"value\":99}]"));

            assertEquals(99, ctx.getInt("count"));
        }

        @Test
        @DisplayName("should replace a field with a nested object")
        void replaceWithObject() throws Exception {
            StateContextImpl ctx = new StateContextImpl(Map.of("data", "simple"));
            ctx.applyDiff(patch("[{\"op\":\"replace\",\"path\":\"/data\",\"value\":{\"key\":\"val\"}}]"));

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) ctx.get("data");
            assertEquals("val", data.get("key"));
        }

        @Test
        @DisplayName("should not affect other fields when replacing one")
        void replaceDoesNotAffectOthers() throws Exception {
            StateContextImpl ctx = new StateContextImpl(Map.of("a", "1", "b", "2"));
            ctx.applyDiff(patch("[{\"op\":\"replace\",\"path\":\"/a\",\"value\":\"updated\"}]"));

            assertEquals("updated", ctx.getString("a"));
            assertEquals("2", ctx.getString("b"));
        }
    }

    // ================================================================== //

    @Nested
    @DisplayName("remove operations")
    class RemoveOperations {

        @Test
        @DisplayName("should remove an existing field")
        void removeField() throws Exception {
            StateContextImpl ctx = new StateContextImpl(Map.of("status", "active", "other", "keep"));
            ctx.applyDiff(patch("[{\"op\":\"remove\",\"path\":\"/status\"}]"));

            assertFalse(ctx.contains("status"));
            assertTrue(ctx.contains("other"));
        }

        @Test
        @DisplayName("should leave context empty when removing the only field")
        void removeOnlyField() throws Exception {
            StateContextImpl ctx = new StateContextImpl(Map.of("x", "y"));
            ctx.applyDiff(patch("[{\"op\":\"remove\",\"path\":\"/x\"}]"));

            assertTrue(ctx.isEmpty());
        }
    }

    // ================================================================== //

    @Nested
    @DisplayName("empty patch")
    class EmptyPatch {

        @Test
        @DisplayName("should leave context unchanged for empty patch array")
        void emptyPatchLeavesContextUnchanged() throws Exception {
            StateContextImpl ctx = new StateContextImpl(Map.of("status", "original"));
            ctx.applyDiff(patch("[]"));

            assertEquals("original", ctx.getString("status"));
            assertEquals(1, ctx.size());
        }

        @Test
        @DisplayName("should still increment version for empty patch")
        void emptyPatchIncrementsVersion() throws Exception {
            StateContextImpl ctx = new StateContextImpl();
            long before = ctx.getVersion();
            ctx.applyDiff(patch("[]"));

            assertEquals(before + 1, ctx.getVersion());
        }
    }

    // ================================================================== //

    @Nested
    @DisplayName("version tracking")
    class VersionTracking {

        @Test
        @DisplayName("should increment version on each applyDiff call")
        void versionIncrementsEachCall() throws Exception {
            StateContextImpl ctx = new StateContextImpl();
            long initial = ctx.getVersion();

            ctx.applyDiff(patch("[{\"op\":\"add\",\"path\":\"/a\",\"value\":1}]"));
            assertEquals(initial + 1, ctx.getVersion());

            ctx.applyDiff(patch("[{\"op\":\"add\",\"path\":\"/b\",\"value\":2}]"));
            assertEquals(initial + 2, ctx.getVersion());

            ctx.applyDiff(patch("[{\"op\":\"replace\",\"path\":\"/a\",\"value\":99}]"));
            assertEquals(initial + 3, ctx.getVersion());
        }
    }

    // ================================================================== //

    @Nested
    @DisplayName("roundtrip: snapshot → diff → applyDiff")
    class Roundtrip {

        @Test
        @DisplayName("add field roundtrip")
        void addFieldRoundtrip() {
            StateContextImpl original = new StateContextImpl(Map.of("x", "1"));
            StateContext snapshot = original.snapshot();

            original.set("y", "2");
            JsonNode patch = snapshot.diff(original);

            StateContextImpl replica = new StateContextImpl(Map.of("x", "1"));
            replica.applyDiff(patch);

            assertEquals("1", replica.getString("x"));
            assertEquals("2", replica.getString("y"));
        }

        @Test
        @DisplayName("replace field roundtrip")
        void replaceFieldRoundtrip() {
            StateContextImpl original = new StateContextImpl(Map.of("status", "pending"));
            StateContext snapshot = original.snapshot();

            original.set("status", "active");
            JsonNode patch = snapshot.diff(original);

            StateContextImpl replica = new StateContextImpl(Map.of("status", "pending"));
            replica.applyDiff(patch);

            assertEquals("active", replica.getString("status"));
        }

        @Test
        @DisplayName("remove field roundtrip")
        void removeFieldRoundtrip() {
            StateContextImpl original = new StateContextImpl(Map.of("a", "1", "b", "2"));
            StateContext snapshot = original.snapshot();

            original.remove("b");
            JsonNode patch = snapshot.diff(original);

            StateContextImpl replica = new StateContextImpl(Map.of("a", "1", "b", "2"));
            replica.applyDiff(patch);

            assertTrue(replica.contains("a"));
            assertFalse(replica.contains("b"));
        }

        @Test
        @DisplayName("multiple changes roundtrip")
        void multipleChangesRoundtrip() {
            StateContextImpl original = new StateContextImpl(Map.of("a", "old", "b", "keep"));
            StateContext snapshot = original.snapshot();

            original.set("a", "new");
            original.set("c", "added");
            original.remove("b");
            JsonNode patch = snapshot.diff(original);

            StateContextImpl replica = new StateContextImpl(Map.of("a", "old", "b", "keep"));
            replica.applyDiff(patch);

            assertEquals("new", replica.getString("a"));
            assertFalse(replica.contains("b"));
            assertEquals("added", replica.getString("c"));
        }

        @Test
        @DisplayName("sequential patches reconstruct state correctly")
        void sequentialPatches() {
            StateContextImpl source = new StateContextImpl(Map.of("step", 0));

            // patch 1: step 0 → 1
            StateContext snap1 = source.snapshot();
            source.set("step", 1);
            JsonNode patch1 = snap1.diff(source);

            // patch 2: step 1 → 2 + add field
            StateContext snap2 = source.snapshot();
            source.set("step", 2);
            source.set("done", true);
            JsonNode patch2 = snap2.diff(source);

            // replay on fresh context
            StateContextImpl replica = new StateContextImpl(Map.of("step", 0));
            replica.applyDiff(patch1);
            replica.applyDiff(patch2);

            assertEquals(2, replica.getInt("step"));
            assertTrue(replica.getBoolean("done"));
        }

        @Test
        @DisplayName("signal roundtrip: setPath → diff → applyDiff")
        void signalSetPathRoundtrip() {
            StateContextImpl original = new StateContextImpl(Map.of("balance", 100));
            StateContext snapshot = original.snapshot();

            original.setPath("payment.amount", 250);
            JsonNode patch = snapshot.diff(original);

            StateContextImpl replica = new StateContextImpl(Map.of("balance", 100));
            replica.applyDiff(patch);

            assertEquals(100, replica.getInt("balance"));
            assertNotNull(replica.get("payment"));
        }
    }

    // ================================================================== //

    @Nested
    @DisplayName("existing data is preserved")
    class ExistingDataPreserved {

        @Test
        @DisplayName("should preserve pre-existing fields when adding new ones")
        void preservesExistingOnAdd() throws Exception {
            StateContextImpl ctx = new StateContextImpl(Map.of("existing", "value"));
            ctx.applyDiff(patch("[{\"op\":\"add\",\"path\":\"/new\",\"value\":\"field\"}]"));

            assertEquals("value", ctx.getString("existing"));
            assertEquals("field", ctx.getString("new"));
        }

        @Test
        @DisplayName("should preserve all fields on empty patch")
        void preservesAllOnEmptyPatch() throws Exception {
            Map<String, Object> initial = Map.of("a", "1", "b", 2, "c", true);
            StateContextImpl ctx = new StateContextImpl(initial);
            ctx.applyDiff(patch("[]"));

            assertEquals("1", ctx.getString("a"));
            assertEquals(2, ctx.getInt("b"));
            assertTrue(ctx.getBoolean("c"));
        }
    }

    // ================================================================== //

    @Nested
    @DisplayName("invalid patch")
    class InvalidPatch {

        @Test
        @DisplayName("should throw when patch references a non-existent path for replace")
        void replaceNonExistentThrows() throws Exception {
            StateContextImpl ctx = new StateContextImpl();
            JsonNode badPatch = patch("[{\"op\":\"replace\",\"path\":\"/missing\",\"value\":\"x\"}]");

            assertThrows(Exception.class, () -> ctx.applyDiff(badPatch));
        }

        @Test
        @DisplayName("should silently ignore remove of a non-existent path (library is lenient)")
        void removeNonExistentIsIgnored() throws Exception {
            StateContextImpl ctx = new StateContextImpl(Map.of("existing", "value"));
            ctx.applyDiff(patch("[{\"op\":\"remove\",\"path\":\"/missing\"}]"));

            // context unchanged — library ignores the no-op
            assertEquals("value", ctx.getString("existing"));
            assertEquals(1, ctx.size());
        }

        @Test
        @DisplayName("should not mutate context state when patch fails (replace on non-existent)")
        void contextUnchangedOnFailure() throws Exception {
            StateContextImpl ctx = new StateContextImpl(Map.of("status", "original"));
            JsonNode badPatch = patch("[{\"op\":\"replace\",\"path\":\"/nonexistent\",\"value\":\"x\"}]");

            assertThrows(Exception.class, () -> ctx.applyDiff(badPatch));
            assertEquals("original", ctx.getString("status"));
            assertEquals(1, ctx.size());
        }
    }
}
