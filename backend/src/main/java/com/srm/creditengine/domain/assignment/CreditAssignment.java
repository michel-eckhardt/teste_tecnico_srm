package com.srm.creditengine.domain.assignment;

import com.srm.creditengine.domain.assignor.Assignor;
import com.srm.creditengine.domain.common.StaleVersionException;
import com.srm.creditengine.domain.currency.CurrencyCode;
import com.srm.creditengine.domain.pricing.PricedReceivable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.jspecify.annotations.Nullable;

/**
 * Credit assignment operation: a batch of receivables bought from one assignor and paid in one
 * currency. Aggregate root: receivables are created with it and never change; only the lifecycle
 * (PENDING -> SETTLED | CANCELLED) evolves, guarded by an optimistic lock {@link Version}.
 *
 * <p>Totals are denormalized in the payment currency so the statement report does not need to
 * aggregate receivables: {@code totalFaceValue = totalDiscount + totalNetAmount}.
 */
@Entity
@Table(name = "credit_assignment")
public class CreditAssignment {

    public static final int MAX_RECEIVABLES = 500;

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignor_id", nullable = false, updatable = false)
    private Assignor assignor;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_currency", nullable = false, length = 3, updatable = false)
    private CurrencyCode paymentCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CreditAssignmentStatus status;

    @Column(name = "total_face_value", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal totalFaceValue;

    @Column(name = "total_discount", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal totalDiscount;

    @Column(name = "total_net_amount", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal totalNetAmount;

    @Column(name = "receivables_count", nullable = false, updatable = false)
    private int receivablesCount;

    @Column(name = "idempotency_key", length = 100, updatable = false)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64, updatable = false)
    private String requestHash;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @OneToMany(mappedBy = "creditAssignment", cascade = CascadeType.PERSIST)
    @OrderBy("id")
    private List<Receivable> receivables = new ArrayList<>();

    protected CreditAssignment() {
        // for JPA
    }

    private CreditAssignment(
            Assignor assignor,
            CurrencyCode paymentCurrency,
            @Nullable IdempotencyKey idempotencyKey,
            Instant createdAt) {
        this.assignor = assignor;
        this.paymentCurrency = paymentCurrency;
        this.status = CreditAssignmentStatus.PENDING;
        this.idempotencyKey = idempotencyKey == null ? null : idempotencyKey.key();
        this.requestHash = idempotencyKey == null ? null : idempotencyKey.requestHash();
        this.createdAt = createdAt;
    }

    /**
     * Opens a PENDING operation from receivables already priced in {@code paymentCurrency}.
     *
     * @param idempotencyKey client key and request fingerprint, when the client sent one
     */
    public static CreditAssignment open(
            Assignor assignor,
            CurrencyCode paymentCurrency,
            List<PricedReceivable> priced,
            @Nullable IdempotencyKey idempotencyKey,
            Instant createdAt) {
        Objects.requireNonNull(assignor, "assignor");
        Objects.requireNonNull(paymentCurrency, "paymentCurrency");
        Objects.requireNonNull(createdAt, "createdAt");
        if (priced == null || priced.isEmpty() || priced.size() > MAX_RECEIVABLES) {
            throw new IllegalArgumentException(
                    "an operation has between 1 and %d receivables".formatted(MAX_RECEIVABLES));
        }
        CreditAssignment operation = new CreditAssignment(assignor, paymentCurrency, idempotencyKey, createdAt);
        BigDecimal totalFace = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        for (PricedReceivable item : priced) {
            if (item.paymentCurrency() != paymentCurrency) {
                throw new IllegalArgumentException("receivable priced in %s for an operation paid in %s"
                        .formatted(item.paymentCurrency(), paymentCurrency));
            }
            operation.receivables.add(new Receivable(operation, item));
            totalFace = totalFace.add(item.faceValueInPaymentCurrency());
            totalNet = totalNet.add(item.netAmount());
        }
        operation.totalFaceValue = totalFace;
        operation.totalNetAmount = totalNet;
        operation.totalDiscount = totalFace.subtract(totalNet);
        operation.receivablesCount = priced.size();
        return operation;
    }

    /**
     * Optimistic concurrency check of a client command ({@code If-Match}).
     *
     * @throws StaleVersionException when the client saw another version
     */
    public void requireVersion(long expectedVersion) {
        if (version != expectedVersion) {
            throw new StaleVersionException(expectedVersion, version);
        }
    }

    /** PENDING -> SETTLED. The caller debits the fund in the same transaction. */
    public void settle(Instant at) {
        if (status == CreditAssignmentStatus.SETTLED) {
            throw new OperationAlreadySettledException(id);
        }
        transitionTo(CreditAssignmentStatus.SETTLED);
        this.settledAt = Objects.requireNonNull(at, "at");
    }

    /** PENDING -> CANCELLED. */
    public void cancel(Instant at) {
        transitionTo(CreditAssignmentStatus.CANCELLED);
        this.cancelledAt = Objects.requireNonNull(at, "at");
    }

    private void transitionTo(CreditAssignmentStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(id, status, target);
        }
        this.status = target;
    }

    public UUID getId() {
        return id;
    }

    public Assignor getAssignor() {
        return assignor;
    }

    public CurrencyCode getPaymentCurrency() {
        return paymentCurrency;
    }

    public CreditAssignmentStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalFaceValue() {
        return totalFaceValue;
    }

    public BigDecimal getTotalDiscount() {
        return totalDiscount;
    }

    public BigDecimal getTotalNetAmount() {
        return totalNetAmount;
    }

    public int getReceivablesCount() {
        return receivablesCount;
    }

    public @Nullable String getIdempotencyKey() {
        return idempotencyKey;
    }

    public @Nullable String getRequestHash() {
        return requestHash;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public @Nullable Instant getSettledAt() {
        return settledAt;
    }

    public @Nullable Instant getCancelledAt() {
        return cancelledAt;
    }

    public List<Receivable> getReceivables() {
        return Collections.unmodifiableList(receivables);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof CreditAssignment that && id != null && id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
