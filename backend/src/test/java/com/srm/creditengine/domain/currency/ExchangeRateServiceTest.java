package com.srm.creditengine.domain.currency;

import static com.srm.creditengine.domain.currency.CurrencyCode.BRL;
import static com.srm.creditengine.domain.currency.CurrencyCode.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.creditengine.domain.common.BusinessClock;
import com.srm.creditengine.domain.common.BusinessProperties;
import com.srm.creditengine.domain.common.ErrorCode;
import com.srm.creditengine.domain.common.ResourceNotFoundException;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class ExchangeRateServiceTest {

    /** Wednesday 2026-09-23 09:00 in Sao Paulo. */
    private static final Instant NOW = Instant.parse("2026-09-23T12:00:00Z");

    private final ExchangeRateRepository rates = mock(ExchangeRateRepository.class);
    private final CurrencyRepository currencies = mock(CurrencyRepository.class);
    private final BusinessClock clock =
            new BusinessClock(Clock.fixed(NOW, ZoneOffset.UTC), new BusinessProperties(ZoneId.of("America/Sao_Paulo")));
    private final ExchangeRateService service =
            new ExchangeRateService(rates, currencies, clock, new FxProperties(Duration.ofDays(5)));

    private static ExchangeRate rate(CurrencyCode base, CurrencyCode quote, String value, LocalDate date) {
        return ExchangeRate.of(
                base,
                quote,
                new BigDecimal(value),
                ExchangeRateSource.FRANKFURTER,
                date,
                date.atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    private void stubLatest(CurrencyCode base, CurrencyCode quote, ExchangeRate rate) {
        when(rates.findFirstByBaseCurrencyAndQuoteCurrencyOrderByReferenceDateDescCreatedAtDesc(base, quote))
                .thenReturn(Optional.ofNullable(rate));
    }

    @Test
    void latestReturnsThePublishedPair() {
        stubLatest(USD, BRL, rate(USD, BRL, "5.1322", LocalDate.of(2026, 9, 23)));
        stubLatest(BRL, USD, null);

        ExchangeRateView view = service.latest(USD, BRL);

        assertThat(view.base()).isEqualTo(USD);
        assertThat(view.quote()).isEqualTo(BRL);
        assertThat(view.rate()).isEqualTo(new BigDecimal("5.13220000"));
        assertThat(view.derived()).isFalse();
        assertThat(view.stale()).isFalse();
    }

    @Test
    void latestDerivesTheInverseWhenOnlyTheOppositePairExists() {
        stubLatest(BRL, USD, null);
        stubLatest(USD, BRL, rate(USD, BRL, "5.1322", LocalDate.of(2026, 9, 23)));

        ExchangeRateView view = service.latest(BRL, USD);

        assertThat(view.base()).isEqualTo(BRL);
        assertThat(view.quote()).isEqualTo(USD);
        assertThat(view.rate()).isEqualTo(new BigDecimal("0.19484821"));
        assertThat(view.derived()).isTrue();
    }

    @Test
    void latestPicksTheMostRecentObservationAcrossBothDirections() {
        stubLatest(USD, BRL, rate(USD, BRL, "5.1322", LocalDate.of(2026, 9, 21)));
        stubLatest(BRL, USD, rate(BRL, USD, "0.2", LocalDate.of(2026, 9, 22)));

        ExchangeRateView view = service.latest(USD, BRL);

        assertThat(view.derived()).isTrue();
        assertThat(view.rate()).isEqualTo(new BigDecimal("5.00000000"));
        assertThat(view.referenceDate()).isEqualTo(LocalDate.of(2026, 9, 22));
    }

    @Test
    void latestFailsWithNotFoundWhenThePairWasNeverRegistered() {
        stubLatest(USD, BRL, null);
        stubLatest(BRL, USD, null);

        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> service.latest(USD, BRL))
                .withMessageContaining("USD/BRL");
    }

    @Test
    void conversionRequiresTwoDistinctCurrencies() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.requireFreshConversion(BRL, BRL));
    }

    @Test
    void pricingConversionIsUnavailableWithoutAnyRate() {
        stubLatest(BRL, USD, null);
        stubLatest(USD, BRL, null);

        assertThatExceptionOfType(ExchangeRateUnavailableException.class)
                .isThrownBy(() -> service.requireFreshConversion(BRL, USD))
                .satisfies(ex -> assertThat(ex.code()).isEqualTo(ErrorCode.EXCHANGE_RATE_UNAVAILABLE));
    }

    @Test
    void pricingConversionRejectsARateOlderThanTheMaximumAge() {
        // 2026-09-18 00:00 BRT + 5 days = 2026-09-23 00:00 BRT, which is before "now" (09:00 BRT)
        stubLatest(BRL, USD, null);
        stubLatest(USD, BRL, rate(USD, BRL, "5.1322", LocalDate.of(2026, 9, 18)));

        assertThatExceptionOfType(ExchangeRateStaleException.class)
                .isThrownBy(() -> service.requireFreshConversion(BRL, USD))
                .withMessageContaining("2026-09-18")
                .withMessageContaining("5 dia(s)")
                .satisfies(ex -> assertThat(ex.code()).isEqualTo(ErrorCode.EXCHANGE_RATE_STALE));
    }

    @Test
    void pricingConversionAcceptsARateWithinTheMaximumAge() {
        stubLatest(BRL, USD, null);
        stubLatest(USD, BRL, rate(USD, BRL, "5.1322", LocalDate.of(2026, 9, 19)));

        CurrencyConversion conversion = service.requireFreshConversion(BRL, USD);

        assertThat(conversion.inverse()).isTrue();
        assertThat(conversion.rate().getReferenceDate()).isEqualTo(LocalDate.of(2026, 9, 19));
    }

    @Test
    void manualRateDefaultsToTheBusinessDate() {
        when(rates.save(any(ExchangeRate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExchangeRateView view = service.registerManual(USD, BRL, new BigDecimal("5.2"), null);

        ArgumentCaptor<ExchangeRate> saved = ArgumentCaptor.forClass(ExchangeRate.class);
        verify(rates).save(saved.capture());
        assertThat(saved.getValue().getSource()).isEqualTo(ExchangeRateSource.MANUAL);
        assertThat(saved.getValue().getReferenceDate()).isEqualTo(LocalDate.of(2026, 9, 23));
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(view.rate()).isEqualTo(new BigDecimal("5.20000000"));
        assertThat(view.stale()).isFalse();
    }

    @Test
    void manualRateKeepsAnExplicitReferenceDate() {
        when(rates.save(any(ExchangeRate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ExchangeRateView view = service.registerManual(USD, BRL, new BigDecimal("5.2"), LocalDate.of(2026, 9, 1));

        assertThat(view.referenceDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(view.stale()).isTrue();
    }

    @Test
    void historyIsSortedNewestFirst() {
        ExchangeRate observation = rate(USD, BRL, "5.1322", LocalDate.of(2026, 9, 23));
        when(rates.findByBaseCurrencyAndQuoteCurrency(eq(USD), eq(BRL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(observation)));

        Page<ExchangeRateView> page = service.history(USD, BRL, 0, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(rates).findByBaseCurrencyAndQuoteCurrency(eq(USD), eq(BRL), pageable.capture());
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "referenceDate", "createdAt"));
        assertThat(page.getContent()).extracting(ExchangeRateView::rate).containsExactly(new BigDecimal("5.13220000"));
    }

    @Test
    void getFailsWithNotFoundForUnknownIds() {
        UUID id = UUID.randomUUID();
        when(rates.findById(id)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> service.get(id));
    }

    @Test
    void currenciesAreListedByCode() {
        when(currencies.findAll(Sort.by("code")))
                .thenReturn(List.of(new Currency(BRL, "Real brasileiro", 2), new Currency(USD, "Dólar americano", 2)));

        assertThat(service.currencies())
                .extracting(Currency::getCode, Currency::getName, Currency::getDecimals)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(BRL, "Real brasileiro", 2),
                        org.assertj.core.groups.Tuple.tuple(USD, "Dólar americano", 2));
    }
}
