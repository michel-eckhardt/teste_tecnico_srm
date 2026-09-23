package com.srm.creditengine.domain.assignment;

import com.srm.creditengine.domain.common.BusinessClock;
import com.srm.creditengine.domain.common.ResourceNotFoundException;
import com.srm.creditengine.domain.treasury.CashMovement;
import com.srm.creditengine.domain.treasury.FundCashAccount;
import com.srm.creditengine.persistence.CashMovementRepository;
import com.srm.creditengine.persistence.CreditAssignmentRepository;
import com.srm.creditengine.persistence.FundCashAccountRepository;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single-transaction lifecycle commands. A settlement changes three things atomically: the
 * operation status, the fund cash account balance and the ledger; either all of them are committed
 * or none is (ACID). Retries of optimistic locking conflicts happen outside, in
 * {@link SettlementService}, around the whole transaction.
 */
@Component
class SettlementTransactions {

    private static final Logger log = LoggerFactory.getLogger(SettlementTransactions.class);

    private final CreditAssignmentRepository operations;
    private final FundCashAccountRepository accounts;
    private final CashMovementRepository movements;
    private final BusinessClock clock;

    SettlementTransactions(
            CreditAssignmentRepository operations,
            FundCashAccountRepository accounts,
            CashMovementRepository movements,
            BusinessClock clock) {
        this.operations = operations;
        this.accounts = accounts;
        this.movements = movements;
        this.clock = clock;
    }

    @Transactional
    public CreditAssignment settle(UUID operationId, long expectedVersion) {
        CreditAssignment operation = load(operationId);
        operation.requireVersion(expectedVersion);
        Instant now = clock.now();

        operation.settle(now);
        // Claim the operation row first: a concurrent settlement of the SAME operation fails here with
        // an optimistic locking conflict (and is retried into a 412) before touching the treasury.
        operations.flush();

        FundCashAccount account = accounts.findById(operation.getPaymentCurrency())
                .orElseThrow(() ->
                        new IllegalStateException("no cash account for currency " + operation.getPaymentCurrency()));
        account.debit(operation.getTotalNetAmount(), now);
        // Concurrent settlements of DIFFERENT operations in the same currency collide on this row.
        accounts.flush();

        movements.save(CashMovement.settlementDebit(
                account.getCurrency(), operation.getId(), operation.getTotalNetAmount(), now));
        log.info(
                "Credit assignment settled: id={} paymentCurrency={} amount={}",
                operation.getId(),
                operation.getPaymentCurrency(),
                operation.getTotalNetAmount());
        return operation;
    }

    @Transactional
    public CreditAssignment cancel(UUID operationId, long expectedVersion) {
        CreditAssignment operation = load(operationId);
        operation.requireVersion(expectedVersion);
        operation.cancel(clock.now());
        operations.flush();
        log.info("Credit assignment cancelled: id={}", operation.getId());
        return operation;
    }

    private CreditAssignment load(UUID operationId) {
        return operations
                .findAggregateById(operationId)
                .orElseThrow(() -> new ResourceNotFoundException("Operação de cessão", operationId));
    }
}
