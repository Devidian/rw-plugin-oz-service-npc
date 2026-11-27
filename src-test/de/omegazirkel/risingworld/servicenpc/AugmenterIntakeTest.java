package de.omegazirkel.risingworld.servicenpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.omegazirkel.risingworld.tools.bridge.WalletBridge.WalletTransferCallResult;

import com.google.gson.JsonParser;
import net.risingworld.api.objects.Inventory.SlotType;
import org.junit.Test;

public class AugmenterIntakeTest {
    private static final WalletTransferCallResult PAID = new WalletTransferCallResult(true, "", "");

    private static ServiceInventoryTransfer.PlannedRemoval step(int slot, boolean target) {
        return new ServiceInventoryTransfer.PlannedRemoval(SlotType.Inventory, slot, "axe",
                0, 10, (short) 0, "", 0, 1, 1, target);
    }

    @Test public void targetDoesNotAlsoCountAsMaterial() {
        var target = step(0, true);
        assertTrue(ServiceInventoryTransfer.planAugment(List.of(target), target, Map.of("axe", 1)).isEmpty());
        assertEquals(List.of(target, new ServiceInventoryTransfer.PlannedRemoval(SlotType.Inventory, 1, "axe",
                0, 10, (short) 0, "", 0, 1, 1, false)),
                ServiceInventoryTransfer.planAugment(List.of(target, step(1, false)), target, Map.of("AXE", 1)));
        var stack = new ServiceInventoryTransfer.PlannedRemoval(SlotType.Inventory, 0, "axe",
                0, 10, (short) 0, "", 0, 2, 1, true);
        var plan = ServiceInventoryTransfer.planAugment(List.of(stack), stack, Map.of("axe", 1));
        assertEquals(1, plan.get(1).expectedStack());
        assertFalse(plan.get(1).target());
    }

    @Test public void confirmedInsufficientFundsDoesNotTouchInventoryOrQuarantine() throws Exception {
        List<String> states = new ArrayList<>();
        assertEquals(AugmenterIntake.Outcome.PAYMENT_REJECTED, AugmenterIntake.submit(List.of(step(0, true)),
                (status, details) -> states.add(status),
                () -> new WalletTransferCallResult(false, "", "Wallet balance is too low for this transfer."),
                ignored -> { fail("unexpected removal"); return false; },
                () -> { fail("unexpected refund"); return false; }));
        assertEquals(List.of("PREPARING", "CANCELLED"), states);
    }

    @Test public void partialFailureRetainsConfirmedAndAmbiguousStepsAndRefundResult() throws Exception {
        for (boolean refunded : new boolean[] { true, false }) {
            List<String> states = new ArrayList<>();
            int[] attempts = { 0 };
            AugmenterIntake.Outcome outcome = AugmenterIntake.submit(List.of(step(0, true), step(1, false)),
                    (status, details) -> states.add(status + ":" + details), () -> PAID,
                    ignored -> ++attempts[0] == 1, () -> refunded);
            assertEquals(AugmenterIntake.Outcome.REVIEW_REQUIRED, outcome);
            assertEquals(2, attempts[0]);
            String finalState = states.get(states.size() - 1);
            assertTrue(finalState.startsWith("RECONCILE_REQUIRED:"));
            var json = JsonParser.parseString(finalState.substring(finalState.indexOf(':') + 1)).getAsJsonObject();
            assertEquals("CONFIRMED", json.getAsJsonArray("inventory").get(0).getAsJsonObject().get("outcome").getAsString());
            assertEquals("ATTEMPTED", json.getAsJsonArray("inventory").get(1).getAsJsonObject().get("outcome").getAsString());
            assertEquals(refunded ? "CONFIRMED" : "ATTEMPTED", json.get("refund").getAsString());
        }
    }

    @Test public void failedOpenWriteKeepsConfirmedIntakeForRecoveryWithoutRetry() throws Exception {
        List<String> states = new ArrayList<>();
        int[] removals = { 0 };
        try {
            AugmenterIntake.submit(List.of(step(0, true)), (status, details) -> {
                if ("OPEN".equals(status)) throw new SQLException("injected OPEN failure");
                states.add(details);
            }, () -> PAID, ignored -> { removals[0]++; return true; }, () -> { fail("unexpected refund"); return false; });
            fail("expected failure");
        } catch (SQLException expected) {
            assertEquals(1, removals[0]);
            assertEquals("CONFIRMED", JsonParser.parseString(states.get(states.size() - 1)).getAsJsonObject()
                    .getAsJsonArray("inventory").get(0).getAsJsonObject().get("outcome").getAsString());
        }
    }

    @Test public void interruptedMutationLeavesAttemptedStepAndNeverRetriesIt() throws Exception {
        List<String> states = new ArrayList<>();
        try {
            AugmenterIntake.submit(List.of(step(0, true)), (status, details) -> states.add(details),
                    () -> PAID, ignored -> { throw new IllegalStateException("ambiguous"); },
                    () -> { fail("unexpected refund"); return false; });
            fail("expected interruption");
        } catch (IllegalStateException expected) {
            assertEquals("ATTEMPTED", JsonParser.parseString(states.get(states.size() - 1)).getAsJsonObject()
                    .getAsJsonArray("inventory").get(0).getAsJsonObject().get("outcome").getAsString());
        }
    }

    @Test public void restartRetainsJournalAndDoesNotScheduleAmbiguousJob() throws Exception {
        var file = Files.createTempFile("augmenter-custody-", ".sqlite");
        try {
            try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file)) {
                ServiceNpcRepository repository = new ServiceNpcRepository(db);
                repository.initialize();
                ServiceJob job = job("job");
                repository.saveJob(job);
                assertEquals(AugmenterIntake.Outcome.REVIEW_REQUIRED, AugmenterIntake.submit(List.of(step(0, true)),
                        (status, details) -> repository.updateJobProgress(job.id(), status, details),
                        () -> PAID, ignored -> false, () -> false));
            }
            try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + file)) {
                ServiceNpcRepository afterRestart = new ServiceNpcRepository(db);
                assertEquals("RECONCILE_REQUIRED", afterRestart.job("job").orElseThrow().status());
                assertTrue(afterRestart.dueJobs(Long.MAX_VALUE).isEmpty());
                assertFalse(afterRestart.job("job").orElseThrow().open());
                assertFalse(afterRestart.reserveAugmenterJob(job("retry")));
                assertTrue(afterRestart.job("retry").isEmpty());
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static ServiceJob job(String id) {
        return new ServiceJob(id, 1, 2, "player", ServiceType.AUGMENTER, 0, 0,
                "PREPARING", "en", "axe", 0, 10, (short) 0, "", "", 0, 3, "coins", id + "-corr");
    }
}
