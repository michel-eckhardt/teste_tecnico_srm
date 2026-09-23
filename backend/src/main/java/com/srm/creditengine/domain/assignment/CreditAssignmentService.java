package com.srm.creditengine.domain.assignment;

import com.srm.creditengine.domain.common.BusinessMetrics;
import com.srm.creditengine.domain.common.ResourceNotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Credit assignment use cases exposed to the application layer.
 *
 * <p>Idempotency: a request carrying an {@code Idempotency-Key} already used with the same payload
 * returns the original operation; with another payload it is rejected. Two identical requests
 * racing with the same new key are resolved by the unique constraint on the key: the loser's
 * transaction fails as a whole and it answers with the winner's operation.
 */
@Service
public class CreditAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(CreditAssignmentService.class);

    private final CreditAssignmentTransactions transactions;
    private final BusinessMetrics metrics;

    CreditAssignmentService(CreditAssignmentTransactions transactions, BusinessMetrics metrics) {
        this.transactions = transactions;
        this.metrics = metrics;
    }

    public CreationResult create(NewCreditAssignment command, @Nullable String idempotencyKey) {
        CreationResult result = createOrReplay(command, idempotencyKey);
        if (result.created()) {
            metrics.creditAssignmentCreated(
                    result.assignment().getPaymentCurrency().name());
        }
        return result;
    }

    private CreationResult createOrReplay(NewCreditAssignment command, @Nullable String idempotencyKey) {
        if (idempotencyKey == null) {
            return new CreationResult(transactions.open(command, null), true);
        }
        IdempotencyKey key = new IdempotencyKey(idempotencyKey, command.fingerprint());
        Optional<CreditAssignment> previous = transactions.findByIdempotencyKey(idempotencyKey);
        if (previous.isPresent()) {
            return replay(previous.get(), key);
        }
        try {
            return new CreationResult(transactions.open(command, key), true);
        } catch (DataIntegrityViolationException raceLost) {
            CreditAssignment winner =
                    transactions.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> raceLost);
            log.info("Concurrent request with the same Idempotency-Key resolved to operation {}", winner.getId());
            return replay(winner, key);
        }
    }

    public CreditAssignment get(UUID id) {
        return transactions
                .findAggregate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Operação de cessão", id));
    }

    private static CreationResult replay(CreditAssignment existing, IdempotencyKey key) {
        if (!key.requestHash().equals(existing.getRequestHash())) {
            throw new IdempotencyKeyReusedException(key.key());
        }
        return new CreationResult(existing, false);
    }
}
