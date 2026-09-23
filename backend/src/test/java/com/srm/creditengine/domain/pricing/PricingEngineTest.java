package com.srm.creditengine.domain.pricing;

import static com.srm.creditengine.domain.currency.CurrencyCode.BRL;
import static com.srm.creditengine.domain.currency.CurrencyCode.USD;
import static com.srm.creditengine.domain.pricing.ReceivableType.CHEQUE_PRE_DATADO;
import static com.srm.creditengine.domain.pricing.ReceivableType.DUPLICATA_MERCANTIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.srm.creditengine.domain.common.BusinessClock;
import com.srm.creditengine.domain.common.BusinessMetrics;
import com.srm.creditengine.domain.common.BusinessProperties;
import com.srm.creditengine.domain.currency.Currency;
import com.srm.creditengine.domain.currency.CurrencyCode;
import com.srm.creditengine.domain.currency.CurrencyConversion;
import com.srm.creditengine.domain.currency.ExchangeRate;
import com.srm.creditengine.domain.currency.ExchangeRateService;
import com.srm.creditengine.domain.currency.ExchangeRateSource;
import com.srm.creditengine.domain.currency.ExchangeRateStaleException;
import com.srm.creditengine.persistence.CurrencyRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Exact expectations of the pricing rules. Reference values were computed independently with
 * 50-digit decimal arithmetic: {@code PV = face / (1 + baseRate + spread) ^ (days / 30)}, rounded
 * HALF_EVEN to cents. Base rates: BRL 1% a.m., USD 0.5% a.m.; spreads: duplicata 1.5%, cheque 2.5%.
 */
class PricingEngineTest {

    /** 2026-09-23 09:00 in Sao Paulo: the operation date of every scenario. */
    private static final Instant NOW = Instant.parse("2026-09-23T12:00:00Z");

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 23);
    private static final ExchangeRate USD_BRL =
            ExchangeRate.of(USD, BRL, new BigDecimal("5.1322"), ExchangeRateSource.FRANKFURTER, TODAY, NOW);

    private final ExchangeRateService exchangeRates = mock(ExchangeRateService.class);
    private final CurrencyRepository currencies = mock(CurrencyRepository.class);

    PricingEngineTest() {
        when(currencies.findAll())
                .thenReturn(List.of(new Currency(BRL, "Real brasileiro", 2), new Currency(USD, "Dólar americano", 2)));
        when(exchangeRates.requireFreshConversion(USD, BRL)).thenReturn(new CurrencyConversion(USD_BRL, USD, BRL));
        when(exchangeRates.requireFreshConversion(BRL, USD)).thenReturn(new CurrencyConversion(USD_BRL, BRL, USD));
    }

    private PricingEngine engine(
            Instant now, List<PricingStrategy> strategies, Map<CurrencyCode, BigDecimal> baseRates) {
        BusinessClock clock = new BusinessClock(
                Clock.fixed(now, ZoneOffset.UTC), new BusinessProperties(ZoneId.of("America/Sao_Paulo")));
        return new PricingEngine(
                new PricingStrategyResolver(strategies),
                exchangeRates,
                currencies,
                clock,
                new BusinessMetrics(new SimpleMeterRegistry()),
                new PricingProperties(1825, baseRates));
    }

    private PricingEngine defaultEngine() {
        return engine(
                NOW,
                List.of(
                        new DuplicataMercantilPricingStrategy(new BigDecimal("0.015")),
                        new ChequePreDatadoPricingStrategy(new BigDecimal("0.025"))),
                Map.of(BRL, new BigDecimal("0.01"), USD, new BigDecimal("0.005")));
    }

    private PricedReceivable priceOne(
            PricingEngine engine,
            ReceivableType type,
            String face,
            CurrencyCode faceCurrency,
            int days,
            CurrencyCode pay) {
        return engine.price(
                        List.of(new ReceivableTerms(type, new BigDecimal(face), faceCurrency, TODAY.plusDays(days))),
                        pay)
                .getFirst();
    }

    @ParameterizedTest(name = "{0} {1} BRL in {2} days -> PV {4}, discount {5}")
    @CsvSource({
        // type,              face,       days, termMonths,  presentValue, discount
        "DUPLICATA_MERCANTIL, 10000.00,   90,   3.00000000,  9285.99,      714.01",
        "CHEQUE_PRE_DATADO,   10000.00,   90,   3.00000000,  9019.43,      980.57",
        "DUPLICATA_MERCANTIL, 10000.00,   45,   1.50000000,  9636.39,      363.61",
        "CHEQUE_PRE_DATADO,   10000.00,   45,   1.50000000,  9497.07,      502.93",
        "DUPLICATA_MERCANTIL, 10000.00,   100,  3.33333333,  9209.88,      790.12",
        "DUPLICATA_MERCANTIL, 150000.00,  1,    0.03333333,  149876.59,    123.41",
        "DUPLICATA_MERCANTIL, 1000000.00, 1825, 60.83333333, 222654.52,    777345.48"
    })
    void pricesSameCurrencyReceivablesWithTheStrategySpread(
            ReceivableType type, String face, int days, String termMonths, String presentValue, String discount) {
        PricedReceivable priced = priceOne(defaultEngine(), type, face, BRL, days, BRL);

        assertThat(priced.termDays()).isEqualTo(days);
        assertThat(priced.termMonths()).isEqualTo(new BigDecimal(termMonths));
        assertThat(priced.presentValue()).isEqualTo(new BigDecimal(presentValue));
        assertThat(priced.discount()).isEqualTo(new BigDecimal(discount));
        assertThat(priced.netAmount()).isEqualTo(new BigDecimal(presentValue));
        assertThat(priced.faceValueInPaymentCurrency()).isEqualTo(new BigDecimal(face));
        assertThat(priced.presentValue().add(priced.discount())).isEqualTo(new BigDecimal(face));
        assertThat(priced.conversion()).isNull();
        verifyNoInteractions(exchangeRates);
    }

    @Test
    void exposesEveryRateUsedWithEightDecimalPlaces() {
        PricedReceivable priced = priceOne(defaultEngine(), CHEQUE_PRE_DATADO, "10000.00", BRL, 90, BRL);

        assertThat(priced.baseRate()).isEqualTo(new BigDecimal("0.01000000"));
        assertThat(priced.spread()).isEqualTo(new BigDecimal("0.02500000"));
        assertThat(priced.discountRate()).isEqualTo(new BigDecimal("0.03500000"));
        assertThat(priced.operationDate()).isEqualTo(TODAY);
        assertThat(priced.dueDate()).isEqualTo(LocalDate.of(2026, 12, 22));
    }

    @Test
    void usdReceivablePaidInBrlUsesTheUsdBaseRateAndConvertsAtTheEnd() {
        // 2500 / 1.03 ^ 1.6 = 2384.5171... USD; x 5.1322 = 12237.8188... BRL
        PricedReceivable priced = priceOne(defaultEngine(), CHEQUE_PRE_DATADO, "2500.00", USD, 48, BRL);

        assertThat(priced.discountRate()).isEqualTo(new BigDecimal("0.03000000"));
        assertThat(priced.presentValue()).isEqualTo(new BigDecimal("2384.52"));
        assertThat(priced.discount()).isEqualTo(new BigDecimal("115.48"));
        assertThat(priced.netAmount()).isEqualTo(new BigDecimal("12237.82"));
        assertThat(priced.faceValueInPaymentCurrency()).isEqualTo(new BigDecimal("12830.50"));
        assertThat(priced.conversion().rate()).isSameAs(USD_BRL);
        assertThat(priced.conversion().inverse()).isFalse();
    }

    @Test
    void brlReceivablePaidInUsdDividesByTheStoredRate() {
        // contract example: 9285.99410... BRL / 5.1322 = 1809.3593... USD
        PricedReceivable priced = priceOne(defaultEngine(), DUPLICATA_MERCANTIL, "10000.00", BRL, 90, USD);

        assertThat(priced.presentValue()).isEqualTo(new BigDecimal("9285.99"));
        assertThat(priced.discount()).isEqualTo(new BigDecimal("714.01"));
        assertThat(priced.netAmount()).isEqualTo(new BigDecimal("1809.36"));
        assertThat(priced.faceValueInPaymentCurrency()).isEqualTo(new BigDecimal("1948.48"));
        assertThat(priced.conversion().inverse()).isTrue();
    }

    @Test
    void aBatchIsPricedWithASingleOperationDateAndFxSnapshot() {
        List<PricedReceivable> priced = defaultEngine()
                .price(
                        List.of(
                                new ReceivableTerms(
                                        DUPLICATA_MERCANTIL, new BigDecimal("100.00"), USD, TODAY.plusDays(30)),
                                new ReceivableTerms(
                                        CHEQUE_PRE_DATADO, new BigDecimal("200.00"), USD, TODAY.plusDays(60)),
                                new ReceivableTerms(
                                        CHEQUE_PRE_DATADO, new BigDecimal("300.00"), BRL, TODAY.plusDays(60))),
                        BRL);

        assertThat(priced).hasSize(3).allSatisfy(item -> assertThat(item.operationDate())
                .isEqualTo(TODAY));
        verify(exchangeRates, times(1)).requireFreshConversion(USD, BRL);
    }

    @Test
    void operationDateIsTheBusinessDateNotTheUtcDate() {
        // 01:30 UTC on the 24th is still the 23rd in Sao Paulo
        PricingEngine lateEvening = engine(
                Instant.parse("2026-09-24T01:30:00Z"),
                List.of(
                        new DuplicataMercantilPricingStrategy(new BigDecimal("0.015")),
                        new ChequePreDatadoPricingStrategy(new BigDecimal("0.025"))),
                Map.of(BRL, new BigDecimal("0.01"), USD, new BigDecimal("0.005")));

        PricedReceivable priced = priceOne(lateEvening, DUPLICATA_MERCANTIL, "10000.00", BRL, 90, BRL);

        assertThat(priced.operationDate()).isEqualTo(TODAY);
        assertThat(priced.termDays()).isEqualTo(90);
    }

    @Nested
    class Rounding {

        /** Rates chosen so that exact results fall on half a cent. */
        private final PricingEngine engine = engine(
                NOW,
                List.of(
                        new FixedSpreadPricingStrategy(DUPLICATA_MERCANTIL, new BigDecimal("0.6")) {},
                        new FixedSpreadPricingStrategy(CHEQUE_PRE_DATADO, new BigDecimal("0.25")) {}),
                Map.of(BRL, BigDecimal.ZERO, USD, BigDecimal.ZERO));

        @ParameterizedTest(name = "face {0} / 1.6 = {1} (exact) -> {2}")
        @CsvSource({"1.00, 0.625, 0.62", "3.00, 1.875, 1.88", "5.00, 3.125, 3.12"})
        void presentValueUsesBankersRounding(String face, String exact, String rounded) {
            PricedReceivable priced = priceOne(engine, DUPLICATA_MERCANTIL, face, BRL, 30, BRL);

            assertThat(new BigDecimal(face).divide(new BigDecimal("1.6"))).isEqualByComparingTo(exact);
            assertThat(priced.presentValue()).isEqualTo(new BigDecimal(rounded));
            assertThat(priced.discount()).isEqualTo(new BigDecimal(face).subtract(new BigDecimal(rounded)));
        }

        @ParameterizedTest(name = "100.00 USD x {0} = {1} BRL")
        @CsvSource({"1.00005, 100.00", "1.00015, 100.02"})
        void conversionIsRoundedOnceWithBankersRounding(String rate, String netAmount) {
            ExchangeRate halfCentRate =
                    ExchangeRate.of(USD, BRL, new BigDecimal(rate), ExchangeRateSource.MANUAL, TODAY, NOW);
            when(exchangeRates.requireFreshConversion(USD, BRL))
                    .thenReturn(new CurrencyConversion(halfCentRate, USD, BRL));

            // 125.00 / 1.25 = 100.00 USD exactly, then converted
            PricedReceivable priced = priceOne(engine, CHEQUE_PRE_DATADO, "125.00", USD, 30, BRL);

            assertThat(priced.presentValue()).isEqualTo(new BigDecimal("100.00"));
            assertThat(priced.netAmount()).isEqualTo(new BigDecimal(netAmount));
        }
    }

    @Nested
    class DueDateRules {

        @Test
        void rejectsADueDateOnTheOperationDate() {
            assertThatExceptionOfType(InvalidDueDateException.class)
                    .isThrownBy(() -> priceOne(defaultEngine(), DUPLICATA_MERCANTIL, "100.00", BRL, 0, BRL))
                    .withMessageContaining("posterior à data da operação");
        }

        @Test
        void rejectsADueDateInThePast() {
            assertThatExceptionOfType(InvalidDueDateException.class)
                    .isThrownBy(() -> priceOne(defaultEngine(), DUPLICATA_MERCANTIL, "100.00", BRL, -5, BRL));
        }

        @Test
        void acceptsTheMaximumTermAndRejectsOneDayMore() {
            assertThat(priceOne(defaultEngine(), DUPLICATA_MERCANTIL, "100.00", BRL, 1825, BRL)
                            .termDays())
                    .isEqualTo(1825);
            assertThatExceptionOfType(InvalidDueDateException.class)
                    .isThrownBy(() -> priceOne(defaultEngine(), DUPLICATA_MERCANTIL, "100.00", BRL, 1826, BRL))
                    .withMessageContaining("1825");
        }
    }

    @Test
    void propagatesAStaleRateInsteadOfPricingWithIt() {
        when(exchangeRates.requireFreshConversion(USD, BRL))
                .thenThrow(new ExchangeRateStaleException(USD_BRL, java.time.Duration.ofDays(5)));

        assertThatExceptionOfType(ExchangeRateStaleException.class)
                .isThrownBy(() -> priceOne(defaultEngine(), DUPLICATA_MERCANTIL, "100.00", USD, 30, BRL));
    }

    @Test
    void failsFastWhenACurrencyHasNoBaseRate() {
        assertThatIllegalStateException()
                .isThrownBy(() -> engine(
                        NOW,
                        List.of(
                                new DuplicataMercantilPricingStrategy(new BigDecimal("0.015")),
                                new ChequePreDatadoPricingStrategy(new BigDecimal("0.025"))),
                        Map.of(BRL, new BigDecimal("0.01"))))
                .withMessageContaining("USD");
    }
}
