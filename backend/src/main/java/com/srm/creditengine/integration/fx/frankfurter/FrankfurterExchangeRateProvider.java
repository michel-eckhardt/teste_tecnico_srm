package com.srm.creditengine.integration.fx.frankfurter;

import com.srm.creditengine.domain.currency.CurrencyCode;
import com.srm.creditengine.domain.currency.ExchangeRateProvider;
import com.srm.creditengine.domain.currency.FxProviderUnavailableException;
import com.srm.creditengine.domain.currency.ProvidedRate;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * Adapter from the Frankfurter API to the domain {@link ExchangeRateProvider} port. Every failure
 * (transport, HTTP status, open circuit or unusable payload) becomes a
 * {@link FxProviderUnavailableException}; nothing from the HTTP client leaks into the domain.
 */
@Component
class FrankfurterExchangeRateProvider implements ExchangeRateProvider {

    private static final Logger log = LoggerFactory.getLogger(FrankfurterExchangeRateProvider.class);

    private final FrankfurterClient client;

    FrankfurterExchangeRateProvider(FrankfurterClient client) {
        this.client = client;
    }

    @Override
    public List<ProvidedRate> fetchLatest(CurrencyCode base, Set<CurrencyCode> quotes) {
        List<String> symbols = quotes.stream().map(Enum::name).sorted().toList();
        FrankfurterLatestResponse response;
        try {
            response = client.latest(base.name(), symbols);
        } catch (CallNotPermittedException open) {
            log.warn("Frankfurter call rejected: circuit breaker is open");
            throw new FxProviderUnavailableException("circuit breaker aberto", open);
        } catch (RestClientException failure) {
            log.warn("Frankfurter call failed: {}", failure.getMessage());
            throw new FxProviderUnavailableException("falha na chamada HTTP", failure);
        }
        return toProvidedRates(response, base, quotes);
    }

    private static List<ProvidedRate> toProvidedRates(
            FrankfurterLatestResponse response, CurrencyCode base, Set<CurrencyCode> quotes) {
        if (response == null || response.date() == null || response.rates() == null) {
            throw invalidPayload("resposta sem data ou cotações");
        }
        if (!base.name().equals(response.base())) {
            throw invalidPayload("moeda base divergente: " + response.base());
        }
        List<ProvidedRate> rates = new ArrayList<>(quotes.size());
        for (CurrencyCode quote : quotes) {
            BigDecimal rate = response.rates().get(quote.name());
            if (rate == null || rate.signum() <= 0) {
                throw invalidPayload("cotação ausente ou inválida para " + quote);
            }
            rates.add(new ProvidedRate(base, quote, rate, response.date()));
        }
        return rates;
    }

    private static FxProviderUnavailableException invalidPayload(String reason) {
        log.warn("Frankfurter returned an unusable payload: {}", reason);
        return new FxProviderUnavailableException("resposta inválida: " + reason, null);
    }
}
