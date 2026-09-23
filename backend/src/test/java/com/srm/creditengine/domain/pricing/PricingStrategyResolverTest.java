package com.srm.creditengine.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricingStrategyResolverTest {

    private static final DuplicataMercantilPricingStrategy DUPLICATA =
            new DuplicataMercantilPricingStrategy(new BigDecimal("0.015"));
    private static final ChequePreDatadoPricingStrategy CHEQUE =
            new ChequePreDatadoPricingStrategy(new BigDecimal("0.025"));

    @Test
    void resolvesTheStrategyOfEachReceivableType() {
        PricingStrategyResolver resolver = new PricingStrategyResolver(List.of(CHEQUE, DUPLICATA));

        assertThat(resolver.resolve(ReceivableType.DUPLICATA_MERCANTIL)).isSameAs(DUPLICATA);
        assertThat(resolver.resolve(ReceivableType.CHEQUE_PRE_DATADO)).isSameAs(CHEQUE);
    }

    @Test
    void failsFastWhenATypeHasNoStrategy() {
        assertThatIllegalStateException()
                .isThrownBy(() -> new PricingStrategyResolver(List.of(DUPLICATA)))
                .withMessageContaining("CHEQUE_PRE_DATADO");
    }

    @Test
    void failsFastWhenATypeHasTwoStrategies() {
        PricingStrategy anotherDuplicata =
                new FixedSpreadPricingStrategy(ReceivableType.DUPLICATA_MERCANTIL, new BigDecimal("0.02")) {};

        assertThatIllegalStateException()
                .isThrownBy(() -> new PricingStrategyResolver(List.of(DUPLICATA, CHEQUE, anotherDuplicata)))
                .withMessageContaining("DUPLICATA_MERCANTIL has two pricing strategies");
    }

    @Test
    void spreadsAreExposedWithEightDecimalPlaces() {
        assertThat(DUPLICATA.nominalMonthlySpread()).isEqualTo(new BigDecimal("0.01500000"));
        assertThat(CHEQUE.monthlySpread(null)).isEqualTo(new BigDecimal("0.02500000"));
        assertThat(CHEQUE.type()).isEqualTo(ReceivableType.CHEQUE_PRE_DATADO);
    }

    @Test
    void rejectsNonsensicalSpreadConfiguration() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new DuplicataMercantilPricingStrategy(new BigDecimal("-0.01")));
        assertThatIllegalArgumentException().isThrownBy(() -> new ChequePreDatadoPricingStrategy(BigDecimal.ONE));
    }

    @Test
    void parsesReceivableTypeCodesStrictly() {
        assertThat(ReceivableType.fromCode("CHEQUE_PRE_DATADO")).contains(ReceivableType.CHEQUE_PRE_DATADO);
        assertThat(ReceivableType.fromCode("cheque_pre_datado")).isEmpty();
        assertThat(ReceivableType.fromCode("NOTA_PROMISSORIA")).isEmpty();
    }
}
