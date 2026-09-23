package com.srm.creditengine.domain.treasury;

import com.srm.creditengine.domain.currency.CurrencyCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Cash of the fund in one currency. Every settlement of that currency debits this single row, so
 * it is the contention point of the system: the {@link Version} column turns concurrent debits into
 * optimistic locking conflicts (retried by the settlement service) instead of lost updates.
 */
@Entity
@Table(name = "fund_cash_account")
public class FundCashAccount {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 3)
    private CurrencyCode currency;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FundCashAccount() {
        // for JPA
    }

    public FundCashAccount(CurrencyCode currency, BigDecimal balance, Instant updatedAt) {
        this.currency = Objects.requireNonNull(currency, "currency");
        this.balance = Objects.requireNonNull(balance, "balance");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    /**
     * Withdraws {@code amount} from the account.
     *
     * @throws InsufficientFundsException when the balance does not cover the amount; the balance is
     *     left untouched (the database also forbids a negative balance)
     */
    public void debit(BigDecimal amount, Instant at) {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("debit amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(currency, amount, balance);
        }
        this.balance = balance.subtract(amount);
        this.updatedAt = Objects.requireNonNull(at, "at");
    }

    public CurrencyCode getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public long getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
