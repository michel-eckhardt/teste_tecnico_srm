package com.srm.creditengine.domain.pricing;

import ch.obermuhlner.math.big.BigDecimalMath;
import com.srm.creditengine.domain.common.BusinessClock;
import com.srm.creditengine.domain.common.BusinessMetrics;
import com.srm.creditengine.domain.currency.Currency;
import com.srm.creditengine.domain.currency.CurrencyCode;
import com.srm.creditengine.domain.currency.CurrencyConversion;
import com.srm.creditengine.domain.currency.ExchangeRateService;
import com.srm.creditengine.persistence.CurrencyRepository;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prices receivables: {@code presentValue = faceValue / (1 + baseRate + spread) ^ (termDays / 30)}.
 *
 * <p>Numerical policy (the heart of the business):
 *
 * <ul>
 *   <li>every intermediate value is a {@link BigDecimal} computed with {@link MathContext#DECIMAL128}
 *       (34 significant digits); the fractional power uses big-math, never {@code double};
 *   <li>the risk spread comes from the {@link PricingStrategy} of the receivable type;
 *   <li>a cross-currency conversion is applied <em>at the end</em>, on the unrounded present value;
 *   <li>amounts are rounded once, {@link RoundingMode#HALF_EVEN} (banker's rounding, no systematic
 *       bias over many operations), to the minor units of their currency, and
 *       {@code discount = faceValue - presentValue} so the two always add up to the face value.
 * </ul>
 *
 * <p>The operation date is the current business date. External FX providers are never called here:
 * rates come from the persisted history and must be fresh.
 */
@Service
public class PricingEngine {

    private static final MathContext PRECISION = MathContext.DECIMAL128;
    private static final BigDecimal DAYS_PER_MONTH = BigDecimal.valueOf(30);
    private static final int RATE_SCALE = FixedSpreadPricingStrategy.RATE_SCALE;

    private final PricingStrategyResolver strategies;
    private final ExchangeRateService exchangeRates;
    private final CurrencyRepository currencies;
    private final BusinessClock clock;
    private final BusinessMetrics metrics;
    private final int maxTermDays;
    private final Map<CurrencyCode, BigDecimal> baseRates;

    public PricingEngine(
            PricingStrategyResolver strategies,
            ExchangeRateService exchangeRates,
            CurrencyRepository currencies,
            BusinessClock clock,
            BusinessMetrics metrics,
            PricingProperties properties) {
        this.strategies = strategies;
        this.exchangeRates = exchangeRates;
        this.currencies = currencies;
        this.clock = clock;
        this.metrics = metrics;
        this.maxTermDays = properties.maxTermDays();
        this.baseRates = new EnumMap<>(CurrencyCode.class);
        properties.baseRates().forEach((currency, rate) -> baseRates.put(currency, rate.setScale(RATE_SCALE)));
        List<CurrencyCode> missing = Arrays.stream(CurrencyCode.values())
                .filter(currency -> !baseRates.containsKey(currency))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("srm.pricing.base-rates has no base rate for " + missing);
        }
    }

    /**
     * Prices a batch as of the same operation date and with one FX snapshot per currency pair, so
     * all receivables of an operation are priced consistently.
     */
    @Transactional(readOnly = true)
    public List<PricedReceivable> price(List<ReceivableTerms> receivables, CurrencyCode paymentCurrency) {
        return metrics.timePricing(paymentCurrency.name(), () -> {
            Batch batch = new Batch(clock.today(), paymentCurrency, currencyDecimals());
            return receivables.stream().map(batch::price).toList();
        });
    }

    private Map<CurrencyCode, Integer> currencyDecimals() {
        return currencies.findAll().stream()
                .collect(Collectors.toMap(
                        Currency::getCode,
                        Currency::getDecimals,
                        (a, b) -> a,
                        () -> new EnumMap<>(CurrencyCode.class)));
    }

    /** State shared by the receivables of one pricing call. */
    private final class Batch {

        private final LocalDate operationDate;
        private final CurrencyCode paymentCurrency;
        private final Map<CurrencyCode, Integer> decimals;
        private final Map<CurrencyCode, CurrencyConversion> conversions = new EnumMap<>(CurrencyCode.class);

        Batch(LocalDate operationDate, CurrencyCode paymentCurrency, Map<CurrencyCode, Integer> decimals) {
            this.operationDate = operationDate;
            this.paymentCurrency = paymentCurrency;
            this.decimals = decimals;
        }

        PricedReceivable price(ReceivableTerms terms) {
            int termDays = termDays(terms.dueDate());
            BigDecimal termMonths = BigDecimal.valueOf(termDays).divide(DAYS_PER_MONTH, PRECISION);
            BigDecimal faceValue =
                    terms.faceValue().setScale(decimalsOf(terms.faceCurrency()), RoundingMode.UNNECESSARY);

            PricingContext context = new PricingContext(
                    terms.type(),
                    faceValue,
                    terms.faceCurrency(),
                    operationDate,
                    terms.dueDate(),
                    termDays,
                    termMonths);
            BigDecimal baseRate = baseRates.get(terms.faceCurrency());
            // rounded to the stored scale first, so the spread used is exactly the one persisted for audit
            BigDecimal spread = strategies
                    .resolve(terms.type())
                    .monthlySpread(context)
                    .setScale(RATE_SCALE, RoundingMode.HALF_EVEN);
            BigDecimal discountRate = baseRate.add(spread);

            BigDecimal discountFactor = BigDecimalMath.pow(BigDecimal.ONE.add(discountRate), termMonths, PRECISION);
            BigDecimal exactPresentValue = faceValue.divide(discountFactor, PRECISION);
            BigDecimal presentValue = round(exactPresentValue, terms.faceCurrency());
            BigDecimal discount = faceValue.subtract(presentValue);

            CurrencyConversion conversion = conversionFor(terms.faceCurrency());
            BigDecimal netAmount = round(convert(exactPresentValue, conversion), paymentCurrency);
            BigDecimal faceInPaymentCurrency = round(convert(faceValue, conversion), paymentCurrency);

            return new PricedReceivable(
                    terms.type(),
                    faceValue,
                    terms.faceCurrency(),
                    paymentCurrency,
                    operationDate,
                    terms.dueDate(),
                    termDays,
                    termMonths.setScale(RATE_SCALE, RoundingMode.HALF_EVEN),
                    baseRate,
                    spread,
                    discountRate,
                    presentValue,
                    discount,
                    conversion,
                    netAmount,
                    faceInPaymentCurrency);
        }

        private int termDays(LocalDate dueDate) {
            long days = ChronoUnit.DAYS.between(operationDate, dueDate);
            if (days <= 0) {
                throw new InvalidDueDateException(
                        "A data de vencimento (%s) deve ser posterior à data da operação (%s)."
                                .formatted(dueDate, operationDate));
            }
            if (days > maxTermDays) {
                throw new InvalidDueDateException("O prazo máximo é de %d dias; o vencimento %s resulta em %d dias."
                        .formatted(maxTermDays, dueDate, days));
            }
            return (int) days;
        }

        private @Nullable CurrencyConversion conversionFor(CurrencyCode faceCurrency) {
            if (faceCurrency == paymentCurrency) {
                return null;
            }
            return conversions.computeIfAbsent(
                    faceCurrency, from -> exchangeRates.requireFreshConversion(from, paymentCurrency));
        }

        private BigDecimal convert(BigDecimal amount, @Nullable CurrencyConversion conversion) {
            return conversion == null ? amount : conversion.convert(amount);
        }

        private BigDecimal round(BigDecimal amount, CurrencyCode currency) {
            return amount.setScale(decimalsOf(currency), RoundingMode.HALF_EVEN);
        }

        private int decimalsOf(CurrencyCode currency) {
            Integer minorUnits = decimals.get(currency);
            if (minorUnits == null) {
                throw new IllegalStateException("currency %s is not registered".formatted(currency));
            }
            return minorUnits;
        }
    }
}
