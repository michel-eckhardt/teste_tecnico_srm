package com.srm.creditengine.domain.currency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/**
 * Immutable exchange rate observation: one unit of {@code baseCurrency} is worth {@code rate} units
 * of {@code quoteCurrency} on {@code referenceDate}. Rows are append-only, so a priced receivable
 * can always point to the exact rate it used.
 */
@Entity
@Table(name = "exchange_rate")
public class ExchangeRate {

    /** Rates are stored with 8 decimal places (NUMERIC(19,8)). */
    public static final int SCALE = 8;

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_currency", nullable = false, length = 3, updatable = false)
    private CurrencyCode baseCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "quote_currency", nullable = false, length = 3, updatable = false)
    private CurrencyCode quoteCurrency;

    @Column(name = "rate", nullable = false, precision = 19, scale = SCALE, updatable = false)
    private BigDecimal rate;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20, updatable = false)
    private ExchangeRateSource source;

    @Column(name = "reference_date", nullable = false, updatable = false)
    private LocalDate referenceDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExchangeRate() {
        // for JPA
    }

    private ExchangeRate(
            CurrencyCode baseCurrency,
            CurrencyCode quoteCurrency,
            BigDecimal rate,
            ExchangeRateSource source,
            LocalDate referenceDate,
            Instant createdAt) {
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.rate = rate;
        this.source = source;
        this.referenceDate = referenceDate;
        this.createdAt = createdAt;
    }

    /**
     * Creates a new observation, enforcing the invariants also guarded by the database: distinct
     * currencies and a strictly positive rate with at most {@value #SCALE} decimal places.
     */
    public static ExchangeRate of(
            CurrencyCode base,
            CurrencyCode quote,
            BigDecimal rate,
            ExchangeRateSource source,
            LocalDate referenceDate,
            Instant createdAt) {
        Objects.requireNonNull(base, "base");
        Objects.requireNonNull(quote, "quote");
        Objects.requireNonNull(rate, "rate");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(referenceDate, "referenceDate");
        Objects.requireNonNull(createdAt, "createdAt");
        if (base == quote) {
            throw new IllegalArgumentException("base and quote currencies must differ");
        }
        if (rate.signum() <= 0) {
            throw new IllegalArgumentException("rate must be positive");
        }
        return new ExchangeRate(base, quote, normalize(rate), source, referenceDate, createdAt);
    }

    private static BigDecimal normalize(BigDecimal rate) {
        try {
            return rate.setScale(SCALE);
        } catch (ArithmeticException tooPrecise) {
            throw new IllegalArgumentException("rate must have at most " + SCALE + " decimal places", tooPrecise);
        }
    }

    /** Whether this rate converts between the two currencies, in either direction. */
    public boolean links(CurrencyCode one, CurrencyCode other) {
        return (baseCurrency == one && quoteCurrency == other) || (baseCurrency == other && quoteCurrency == one);
    }

    public UUID getId() {
        return id;
    }

    public CurrencyCode getBaseCurrency() {
        return baseCurrency;
    }

    public CurrencyCode getQuoteCurrency() {
        return quoteCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public ExchangeRateSource getSource() {
        return source;
    }

    public LocalDate getReferenceDate() {
        return referenceDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof ExchangeRate that && id != null && id.equals(that.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
