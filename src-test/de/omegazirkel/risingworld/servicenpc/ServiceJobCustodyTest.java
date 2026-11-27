package de.omegazirkel.risingworld.servicenpc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ServiceJobCustodyTest {
    @Test public void onlyConfirmedOpenJobCanBeCompleted() {
        assertFalse(job("PREPARING").open());
        assertFalse(job("REFUND_PENDING").open());
        assertFalse(job("RECONCILE_REQUIRED").open());
        assertTrue(job("OPEN").open());
    }

    private static ServiceJob job(String status) {
        return new ServiceJob("job", 1, 2, "player", ServiceType.AUGMENTER, 0, 0, status, "en",
                "axe", 0, 10, (short) 0, "", "", 0, 3, "coins", "correlation");
    }
}
