package de.omegazirkel.risingworld.servicenpc;

import java.sql.SQLException;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.omegazirkel.risingworld.tools.bridge.WalletBridge.WalletTransferCallResult;

/** Journals externally visible intake steps before mutating Wallet or inventory. */
final class AugmenterIntake {
    interface Journal {
        void save(String status, String details) throws SQLException;
    }

    enum Outcome { OPEN, PAYMENT_REJECTED, REVIEW_REQUIRED }

    private AugmenterIntake() { }

    static Outcome submit(List<ServiceInventoryTransfer.PlannedRemoval> plan, Journal journal,
            Supplier<WalletTransferCallResult> payment, Predicate<ServiceInventoryTransfer.PlannedRemoval> remove,
            BooleanSupplier refund) throws SQLException {
        JsonObject details = new JsonObject();
        details.addProperty("version", 1);
        details.addProperty("payment", "NOT_STARTED");
        details.addProperty("refund", "NOT_STARTED");
        JsonArray steps = new JsonArray();
        Gson gson = new Gson();
        for (var removal : plan) {
            JsonObject step = gson.toJsonTree(removal).getAsJsonObject();
            step.addProperty("outcome", "NOT_STARTED");
            steps.add(step);
        }
        details.add("inventory", steps);
        details.addProperty("payment", "ATTEMPTED");
        journal.save("PREPARING", gson.toJson(details));
        WalletTransferCallResult debit = payment.get();
        if (!debit.success()) {
            if ("INSUFFICIENT_FUNDS".equals(debit.errorCode())
                    || "Wallet balance is too low for this transfer.".equals(debit.message())) {
                details.addProperty("payment", "REJECTED");
                journal.save("CANCELLED", gson.toJson(details));
                return Outcome.PAYMENT_REJECTED;
            }
            journal.save("RECONCILE_REQUIRED", gson.toJson(details));
            return Outcome.REVIEW_REQUIRED;
        }
        details.addProperty("payment", "CONFIRMED");
        journal.save("PREPARING", gson.toJson(details));
        for (int index = 0; index < plan.size(); index++) {
            JsonObject step = steps.get(index).getAsJsonObject();
            step.addProperty("outcome", "ATTEMPTED");
            journal.save("PREPARING", gson.toJson(details));
            if (!remove.test(plan.get(index))) {
                details.addProperty("refund", "ATTEMPTED");
                journal.save("RECONCILE_REQUIRED", gson.toJson(details));
                if (refund.getAsBoolean()) details.addProperty("refund", "CONFIRMED");
                journal.save("RECONCILE_REQUIRED", gson.toJson(details));
                return Outcome.REVIEW_REQUIRED;
            }
            step.addProperty("outcome", "CONFIRMED");
            journal.save("PREPARING", gson.toJson(details));
        }
        journal.save("OPEN", gson.toJson(details));
        return Outcome.OPEN;
    }
}
