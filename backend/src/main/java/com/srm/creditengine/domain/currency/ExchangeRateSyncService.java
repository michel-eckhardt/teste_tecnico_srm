package com.srm.creditengine.domain.currency;

import com.srm.creditengine.domain.common.BusinessClock;
import com.srm.creditengine.persistence.ExchangeRateRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pulls the latest reference rates from the external provider into the append-only history.
 *
 * <p>The provider is called <em>outside</em> any transaction, so no database connection is held
 * while waiting on the network; the results are then stored in one short transaction. The
 * synchronization is idempotent: a rate already stored for the same pair, reference date and value
 * is not inserted again.
 */
@Service
public class ExchangeRateSyncService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateSyncService.class);

    private final ExchangeRateProvider provider;
    private final ExchangeRateRepository rates;
    private final ExchangeRateService exchangeRateService;
    private final BusinessClock clock;
    private final FxProperties.Sync settings;
    private final TransactionTemplate transaction;

    public ExchangeRateSyncService(
            ExchangeRateProvider provider,
            ExchangeRateRepository rates,
            ExchangeRateService exchangeRateService,
            BusinessClock clock,
            FxProperties properties,
            TransactionTemplate transaction) {
        this.provider = provider;
        this.rates = rates;
        this.exchangeRateService = exchangeRateService;
        this.clock = clock;
        this.settings = properties.sync();
        this.transaction = transaction;
    }

    /**
     * @return the provider rates as persisted (newly inserted or already present)
     * @throws FxProviderUnavailableException when the provider cannot deliver rates; nothing is stored
     */
    public List<ExchangeRateView> synchronize() {
        List<ProvidedRate> provided = provider.fetchLatest(settings.base(), settings.quotes());
        List<StoredRate> stored = transaction.execute(
                status -> provided.stream().map(this::storeIfNew).toList());
        long created = stored.stream().filter(StoredRate::created).count();
        log.info("FX sync finished: provider=FRANKFURTER received={} created={}", stored.size(), created);
        return stored.stream()
                .map(StoredRate::rate)
                .map(rate -> ExchangeRateView.of(rate, exchangeRateService.isStale(rate)))
                .toList();
    }

    private StoredRate storeIfNew(ProvidedRate provided) {
        Optional<ExchangeRate> existing = rates.findFirstByBaseCurrencyAndQuoteCurrencyAndReferenceDateAndSourceAndRate(
                provided.base(),
                provided.quote(),
                provided.referenceDate(),
                ExchangeRateSource.FRANKFURTER,
                provided.rate());
        if (existing.isPresent()) {
            return new StoredRate(existing.get(), false);
        }
        ExchangeRate rate = rates.save(ExchangeRate.of(
                provided.base(),
                provided.quote(),
                provided.rate(),
                ExchangeRateSource.FRANKFURTER,
                provided.referenceDate(),
                clock.now()));
        log.info(
                "Exchange rate stored: pair={}/{} rate={} referenceDate={} source=FRANKFURTER",
                rate.getBaseCurrency(),
                rate.getQuoteCurrency(),
                rate.getRate(),
                rate.getReferenceDate());
        return new StoredRate(rate, true);
    }

    private record StoredRate(ExchangeRate rate, boolean created) {}
}
