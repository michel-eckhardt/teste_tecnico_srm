package com.srm.creditengine.domain.currency;

import com.srm.creditengine.domain.common.BusinessClock;
import com.srm.creditengine.domain.common.ResourceNotFoundException;
import com.srm.creditengine.persistence.CurrencyRepository;
import com.srm.creditengine.persistence.ExchangeRateRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Currency engine: stores and serves exchange rates. Pricing only ever reads rates persisted here;
 * external providers feed this history asynchronously and are never called on the pricing path.
 */
@Service
@Transactional(readOnly = true)
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);
    private static final Comparator<ExchangeRate> RECENCY =
            Comparator.comparing(ExchangeRate::getReferenceDate).thenComparing(ExchangeRate::getCreatedAt);
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "referenceDate", "createdAt");

    private final ExchangeRateRepository rates;
    private final CurrencyRepository currencies;
    private final BusinessClock clock;
    private final FxProperties properties;

    public ExchangeRateService(
            ExchangeRateRepository rates, CurrencyRepository currencies, BusinessClock clock, FxProperties properties) {
        this.rates = rates;
        this.currencies = currencies;
        this.clock = clock;
        this.properties = properties;
    }

    public List<Currency> currencies() {
        return currencies.findAll(Sort.by("code"));
    }

    /** Latest rate between two currencies, published directly or derived from the inverse pair. */
    public ExchangeRateView latest(CurrencyCode base, CurrencyCode quote) {
        return latestConversion(base, quote)
                .map(conversion -> ExchangeRateView.of(conversion, isStale(conversion.rate())))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma taxa de câmbio %s/%s cadastrada.".formatted(base, quote)));
    }

    public ExchangeRateView get(UUID id) {
        return rates.findById(id)
                .map(rate -> ExchangeRateView.of(rate, isStale(rate)))
                .orElseThrow(() -> new ResourceNotFoundException("Taxa de câmbio", id));
    }

    /** History of a published pair, newest first. */
    public Page<ExchangeRateView> history(CurrencyCode base, CurrencyCode quote, int page, int size) {
        return rates.findByBaseCurrencyAndQuoteCurrency(base, quote, PageRequest.of(page, size, NEWEST_FIRST))
                .map(rate -> ExchangeRateView.of(rate, isStale(rate)));
    }

    /** Registers a rate informed by an operator; the reference date defaults to the business date. */
    @Transactional
    public ExchangeRateView registerManual(
            CurrencyCode base, CurrencyCode quote, BigDecimal rate, @Nullable LocalDate referenceDate) {
        LocalDate date = referenceDate != null ? referenceDate : clock.today();
        ExchangeRate saved =
                rates.save(ExchangeRate.of(base, quote, rate, ExchangeRateSource.MANUAL, date, clock.now()));
        log.info(
                "Manual exchange rate registered: pair={}/{} rate={} referenceDate={} id={}",
                base,
                quote,
                saved.getRate(),
                date,
                saved.getId());
        return ExchangeRateView.of(saved, isStale(saved));
    }

    /**
     * Conversion used to price a cross-currency operation: the latest rate of the pair (either
     * direction), which must exist and be younger than {@code srm.fx.max-rate-age}.
     *
     * @throws ExchangeRateUnavailableException if the pair has no rate at all
     * @throws ExchangeRateStaleException if the latest rate is too old
     */
    public CurrencyConversion requireFreshConversion(CurrencyCode from, CurrencyCode to) {
        CurrencyConversion conversion =
                latestConversion(from, to).orElseThrow(() -> new ExchangeRateUnavailableException(from, to));
        if (isStale(conversion.rate())) {
            throw new ExchangeRateStaleException(conversion.rate(), properties.maxRateAge());
        }
        return conversion;
    }

    /** A rate is stale once {@code maxRateAge} has elapsed since the start of its reference date. */
    public boolean isStale(ExchangeRate rate) {
        return clock.startOf(rate.getReferenceDate())
                .plus(properties.maxRateAge())
                .isBefore(clock.now());
    }

    /**
     * Latest rate between two currencies (direct or inverse pair). Observed rates (provider or
     * manual) always prevail over the bootstrap {@code SEED} rate, whatever their reference dates:
     * the seed is dated on the deploy day while the ECB publishes the previous business day until
     * the afternoon, so ranking by date alone would let the placeholder shadow the real quote.
     */
    private Optional<CurrencyConversion> latestConversion(CurrencyCode from, CurrencyCode to) {
        if (from == to) {
            throw new IllegalArgumentException("conversion requires two distinct currencies");
        }
        return mostRecent(from, to, this::latestObserved).or(() -> mostRecent(from, to, this::latestOfAnySource));
    }

    private Optional<ExchangeRate> latestObserved(CurrencyCode base, CurrencyCode quote) {
        return rates.findFirstByBaseCurrencyAndQuoteCurrencyAndSourceNotOrderByReferenceDateDescCreatedAtDesc(
                base, quote, ExchangeRateSource.SEED);
    }

    private Optional<ExchangeRate> latestOfAnySource(CurrencyCode base, CurrencyCode quote) {
        return rates.findFirstByBaseCurrencyAndQuoteCurrencyOrderByReferenceDateDescCreatedAtDesc(base, quote);
    }

    private static Optional<CurrencyConversion> mostRecent(
            CurrencyCode from,
            CurrencyCode to,
            BiFunction<CurrencyCode, CurrencyCode, Optional<ExchangeRate>> latestOfPair) {
        return Stream.concat(latestOfPair.apply(from, to).stream(), latestOfPair.apply(to, from).stream())
                .max(RECENCY)
                .map(rate -> new CurrencyConversion(rate, from, to));
    }
}
