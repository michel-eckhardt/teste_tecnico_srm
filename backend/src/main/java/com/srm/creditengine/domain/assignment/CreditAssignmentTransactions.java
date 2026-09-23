package com.srm.creditengine.domain.assignment;

import com.srm.creditengine.domain.assignor.Assignor;
import com.srm.creditengine.domain.common.BusinessClock;
import com.srm.creditengine.domain.common.ResourceNotFoundException;
import com.srm.creditengine.domain.pricing.PricedReceivable;
import com.srm.creditengine.domain.pricing.PricingEngine;
import com.srm.creditengine.domain.pricing.ReceivableTerms;
import com.srm.creditengine.domain.pricing.ReceivableType;
import com.srm.creditengine.domain.pricing.ReceivableTypeCatalog;
import com.srm.creditengine.persistence.AssignorRepository;
import com.srm.creditengine.persistence.CreditAssignmentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional units of work of the credit assignment aggregate. Kept apart from the public
 * services on purpose: idempotency handling and retries must wrap a whole transaction (a retry
 * inside a failed transaction would run on a rollback-only persistence context).
 */
@Component
class CreditAssignmentTransactions {

    private static final Logger log = LoggerFactory.getLogger(CreditAssignmentTransactions.class);

    private final CreditAssignmentRepository operations;
    private final AssignorRepository assignors;
    private final ReceivableTypeCatalog receivableTypes;
    private final PricingEngine pricingEngine;
    private final BusinessClock clock;

    CreditAssignmentTransactions(
            CreditAssignmentRepository operations,
            AssignorRepository assignors,
            ReceivableTypeCatalog receivableTypes,
            PricingEngine pricingEngine,
            BusinessClock clock) {
        this.operations = operations;
        this.assignors = assignors;
        this.receivableTypes = receivableTypes;
        this.pricingEngine = pricingEngine;
        this.clock = clock;
    }

    /** Prices every receivable and stores the operation with its items atomically. */
    @Transactional
    public CreditAssignment open(NewCreditAssignment command, @Nullable IdempotencyKey idempotencyKey) {
        Assignor assignor = assignors
                .findById(command.assignorId())
                .orElseThrow(() -> new ResourceNotFoundException("Cedente", command.assignorId()));
        List<ReceivableType> types = receivableTypes.requireActive(command.receivables().stream()
                .map(NewCreditAssignment.Item::receivableType)
                .toList());
        List<ReceivableTerms> terms = new ArrayList<>(types.size());
        for (int i = 0; i < types.size(); i++) {
            NewCreditAssignment.Item item = command.receivables().get(i);
            terms.add(new ReceivableTerms(types.get(i), item.faceValue(), item.faceCurrency(), item.dueDate()));
        }
        List<PricedReceivable> priced = pricingEngine.price(terms, command.paymentCurrency());

        CreditAssignment operation =
                CreditAssignment.open(assignor, command.paymentCurrency(), priced, idempotencyKey, clock.now());
        // flush now so a concurrent duplicate Idempotency-Key surfaces here, not at commit time
        operations.saveAndFlush(operation);
        log.info(
                "Credit assignment created: id={} assignorId={} paymentCurrency={} receivables={} totalNetAmount={}",
                operation.getId(),
                assignor.getId(),
                operation.getPaymentCurrency(),
                operation.getReceivablesCount(),
                operation.getTotalNetAmount());
        return operation;
    }

    @Transactional(readOnly = true)
    public Optional<CreditAssignment> findAggregate(UUID id) {
        return operations.findAggregateById(id);
    }

    @Transactional(readOnly = true)
    public Optional<CreditAssignment> findByIdempotencyKey(String key) {
        return operations.findByIdempotencyKey(key);
    }
}
