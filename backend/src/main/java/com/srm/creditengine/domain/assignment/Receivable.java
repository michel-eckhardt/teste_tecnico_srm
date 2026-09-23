package com.srm.creditengine.domain.assignment;

import com.srm.creditengine.domain.currency.CurrencyCode;
import com.srm.creditengine.domain.currency.CurrencyConversion;
import com.srm.creditengine.domain.currency.ExchangeRate;
import com.srm.creditengine.domain.pricing.PricedReceivable;
import com.srm.creditengine.domain.pricing.ReceivableType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.UuidGenerator;
import org.jspecify.annotations.Nullable;

/**
 * Receivable bought within a credit assignment. Immutable: it is a snapshot of the pricing
 * (term, base rate, spread and the exact FX observation) so the operation can be audited and
 * recomputed later regardless of configuration or rate changes.
 */
@Entity
@Immutable
@Table(name = "receivable")
public class Receivable {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_assignment_id", nullable = false, updatable = false)
    private CreditAssignment creditAssignment;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_code", nullable = false, length = 40)
    private ReceivableType type;

    @Column(name = "face_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal faceValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "face_currency", nullable = false, length = 3)
    private CurrencyCode faceCurrency;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "term_days", nullable = false)
    private int termDays;

    @Column(name = "base_rate", nullable = false, precision = 19, scale = 8)
    private BigDecimal baseRate;

    @Column(name = "spread", nullable = false, precision = 19, scale = 8)
    private BigDecimal spread;

    @Column(name = "present_value", nullable = false, precision = 19, scale = 2)
    private BigDecimal presentValue;

    @Column(name = "discount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_rate_id")
    private ExchangeRate exchangeRate;

    @Column(name = "exchange_rate_applied", precision = 19, scale = 8)
    private BigDecimal exchangeRateApplied;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal netAmount;

    protected Receivable() {
        // for JPA
    }

    Receivable(CreditAssignment creditAssignment, PricedReceivable priced) {
        this.creditAssignment = creditAssignment;
        this.type = priced.type();
        this.faceValue = priced.faceValue();
        this.faceCurrency = priced.faceCurrency();
        this.dueDate = priced.dueDate();
        this.termDays = priced.termDays();
        this.baseRate = priced.baseRate();
        this.spread = priced.spread();
        this.presentValue = priced.presentValue();
        this.discount = priced.discount();
        CurrencyConversion conversion = priced.conversion();
        if (conversion != null) {
            this.exchangeRate = conversion.rate();
            this.exchangeRateApplied = conversion.rate().getRate();
        }
        this.netAmount = priced.netAmount();
    }

    public UUID getId() {
        return id;
    }

    public ReceivableType getType() {
        return type;
    }

    public BigDecimal getFaceValue() {
        return faceValue;
    }

    public CurrencyCode getFaceCurrency() {
        return faceCurrency;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public int getTermDays() {
        return termDays;
    }

    public BigDecimal getBaseRate() {
        return baseRate;
    }

    public BigDecimal getSpread() {
        return spread;
    }

    public BigDecimal getPresentValue() {
        return presentValue;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    /** Stored FX observation used to convert this receivable, or {@code null} for same currency. */
    public @Nullable ExchangeRate getExchangeRate() {
        return exchangeRate;
    }

    public @Nullable BigDecimal getExchangeRateApplied() {
        return exchangeRateApplied;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Receivable that && id != null && id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
