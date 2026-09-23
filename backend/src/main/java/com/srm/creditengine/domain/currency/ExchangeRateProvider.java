package com.srm.creditengine.domain.currency;

import java.util.List;
import java.util.Set;

/**
 * Port to an external source of reference exchange rates. Implementations live in the integration
 * layer; the domain depends only on this contract (dependency inversion).
 */
public interface ExchangeRateProvider {

    /**
     * Latest published rates of {@code base} against each of {@code quotes}.
     *
     * @throws FxProviderUnavailableException when the provider cannot be reached, answers with an
     *     error or with an unusable payload, or its circuit breaker is open
     */
    List<ProvidedRate> fetchLatest(CurrencyCode base, Set<CurrencyCode> quotes);
}
