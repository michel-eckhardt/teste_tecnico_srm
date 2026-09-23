package com.srm.creditengine.domain.treasury;

import com.srm.creditengine.domain.currency.CurrencyCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.UuidGenerator;

/**
 * Append-only ledger entry of a fund cash account. The operation is referenced by id only, which
 * keeps the treasury module independent from the credit assignment module.
 */
@Entity
@Immutable
@Table(name = "cash_movement")
public class CashMovement {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private CurrencyCode currency;

    @Column(name = "credit_assignment_id")
    private UUID creditAssignmentId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 6)
    private MovementDirection direction;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CashMovement() {
        // for JPA
    }

    private CashMovement(
            CurrencyCode currency,
            UUID creditAssignmentId,
            BigDecimal amount,
            MovementDirection direction,
            Instant createdAt) {
        this.currency = currency;
        this.creditAssignmentId = creditAssignmentId;
        this.amount = amount;
        this.direction = direction;
        this.createdAt = createdAt;
    }

    /** Payment of a credit assignment (at most one per operation, enforced by a unique index). */
    public static CashMovement settlementDebit(
            CurrencyCode currency, UUID creditAssignmentId, BigDecimal amount, Instant at) {
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(creditAssignmentId, "creditAssignmentId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(at, "at");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("movement amount must be positive");
        }
        return new CashMovement(currency, creditAssignmentId, amount, MovementDirection.DEBIT, at);
    }

    public UUID getId() {
        return id;
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public UUID getCreditAssignmentId() {
        return creditAssignmentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public MovementDirection getDirection() {
        return direction;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
