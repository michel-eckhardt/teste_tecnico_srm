package com.srm.creditengine.domain.currency;

import static com.srm.creditengine.domain.currency.CurrencyCode.BRL;
import static com.srm.creditengine.domain.currency.CurrencyCode.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.srm.creditengine.domain.common.BusinessClock;
import com.srm.creditengine.domain.common.BusinessProperties;
import com.srm.creditengine.persistence.CurrencyRepository;
import com.srm.creditengine.persistence.ExchangeRateRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class ExchangeRateSyncServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-23T20:00:00Z");
    private static final LocalDate ECB_DATE = LocalDate.of(2026, 9, 23);

    private final ExchangeRateProvider provider = mock(ExchangeRateProvider.class);
    private final ExchangeRateRepository rates = mock(ExchangeRateRepository.class);
    private final TransactionTemplate transaction = mock(TransactionTemplate.class);
    private final BusinessClock clock =
            new BusinessClock(Clock.fixed(NOW, ZoneOffset.UTC), new BusinessProperties(ZoneId.of("America/Sao_Paulo")));
    private final FxProperties properties =
            new FxProperties(Duration.ofDays(5), new FxProperties.Sync(true, "0 5 * * * *", USD, Set.of(BRL)));
    private final ExchangeRateSyncService service = new ExchangeRateSyncService(
            provider,
            rates,
            new ExchangeRateService(rates, mock(CurrencyRepository.class), clock, properties),
            clock,
            properties,
            transaction);

    ExchangeRateSyncServiceTest() {
        when(transaction.execute(any())).thenAnswer(invocation -> invocation
                .<TransactionCallback<?>>getArgument(0)
                .doInTransaction(new SimpleTransactionStatus()));
    }

    @Test
    void storesNewProviderRatesAsFrankfurterObservations() {
        when(provider.fetchLatest(USD, Set.of(BRL)))
                .thenReturn(List.of(new ProvidedRate(USD, BRL, new BigDecimal("5.1322"), ECB_DATE)));
        when(rates.findFirstByBaseCurrencyAndQuoteCurrencyAndReferenceDateAndSourceAndRate(
                        USD, BRL, ECB_DATE, ExchangeRateSource.FRANKFURTER, new BigDecimal("5.1322")))
                .thenReturn(Optional.empty());
        when(rates.save(any(ExchangeRate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ExchangeRateView> synced = service.synchronize();

        ArgumentCaptor<ExchangeRate> saved = ArgumentCaptor.forClass(ExchangeRate.class);
        verify(rates).save(saved.capture());
        assertThat(saved.getValue().getSource()).isEqualTo(ExchangeRateSource.FRANKFURTER);
        assertThat(saved.getValue().getReferenceDate()).isEqualTo(ECB_DATE);
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(synced).singleElement().satisfies(view -> {
            assertThat(view.rate()).isEqualTo(new BigDecimal("5.13220000"));
            assertThat(view.stale()).isFalse();
        });
    }

    @Test
    void isIdempotentForAnIdenticalRateOfTheSameReferenceDate() {
        ExchangeRate existing =
                ExchangeRate.of(USD, BRL, new BigDecimal("5.1322"), ExchangeRateSource.FRANKFURTER, ECB_DATE, NOW);
        when(provider.fetchLatest(USD, Set.of(BRL)))
                .thenReturn(List.of(new ProvidedRate(USD, BRL, new BigDecimal("5.1322"), ECB_DATE)));
        when(rates.findFirstByBaseCurrencyAndQuoteCurrencyAndReferenceDateAndSourceAndRate(
                        USD, BRL, ECB_DATE, ExchangeRateSource.FRANKFURTER, new BigDecimal("5.1322")))
                .thenReturn(Optional.of(existing));

        List<ExchangeRateView> synced = service.synchronize();

        verify(rates, never()).save(any());
        assertThat(synced).extracting(ExchangeRateView::rate).containsExactly(new BigDecimal("5.13220000"));
    }

    @Test
    void storesNothingWhenTheProviderIsUnavailable() {
        when(provider.fetchLatest(USD, Set.of(BRL)))
                .thenThrow(new FxProviderUnavailableException("circuit breaker aberto", null));

        assertThatExceptionOfType(FxProviderUnavailableException.class).isThrownBy(service::synchronize);
        verifyNoInteractions(rates, transaction);
    }
}
