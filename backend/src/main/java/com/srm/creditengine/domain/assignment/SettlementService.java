package com.srm.creditengine.domain.assignment;

import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Settlement and cancellation commands, protected by optimistic locking.
 *
 * <p>Every settlement in a currency debits the same cash account row, so settlements of different
 * operations regularly collide on its version. Such a conflict is transient: the whole transaction
 * is retried (small exponential backoff with jitter), reloading fresh state. A conflict on the
 * operation itself cannot be solved by retrying: the retry reloads the operation, whose version no
 * longer matches the client's {@code If-Match} (412) or whose status is final (409). Conflicts that
 * outlive the retries surface as {@code CONCURRENT_MODIFICATION} (409).
 *
 * <p>The retry must wrap the transaction, never run inside it: after an optimistic locking failure
 * the persistence context and the transaction are unusable.
 */
@Service
public class SettlementService {

    private final SettlementTransactions transactions;

    SettlementService(SettlementTransactions transactions) {
        this.transactions = transactions;
    }

    /**
     * PENDING -> SETTLED, debiting the fund cash account of the payment currency and recording the
     * ledger movement in the same transaction.
     *
     * @param expectedVersion version the client is acting on ({@code If-Match})
     */
    @Retryable(
            includes = OptimisticLockingFailureException.class,
            maxRetriesString = "${srm.settlement.retry.max-retries:5}",
            delayString = "${srm.settlement.retry.delay:50ms}",
            jitterString = "${srm.settlement.retry.jitter:25ms}",
            multiplierString = "${srm.settlement.retry.multiplier:2}",
            maxDelayString = "${srm.settlement.retry.max-delay:1s}")
    public CreditAssignment settle(UUID operationId, long expectedVersion) {
        return transactions.settle(operationId, expectedVersion);
    }

    /** PENDING -> CANCELLED. */
    @Retryable(
            includes = OptimisticLockingFailureException.class,
            maxRetriesString = "${srm.settlement.retry.max-retries:5}",
            delayString = "${srm.settlement.retry.delay:50ms}",
            jitterString = "${srm.settlement.retry.jitter:25ms}",
            multiplierString = "${srm.settlement.retry.multiplier:2}",
            maxDelayString = "${srm.settlement.retry.max-delay:1s}")
    public CreditAssignment cancel(UUID operationId, long expectedVersion) {
        return transactions.cancel(operationId, expectedVersion);
    }
}
