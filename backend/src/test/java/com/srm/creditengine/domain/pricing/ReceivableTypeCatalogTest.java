package com.srm.creditengine.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.creditengine.domain.common.ErrorCode;
import com.srm.creditengine.persistence.ReceivableTypeRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReceivableTypeCatalogTest {

    private final ReceivableTypeRepository repository = mock(ReceivableTypeRepository.class);
    private final ReceivableTypeCatalog catalog = new ReceivableTypeCatalog(
            repository,
            new PricingStrategyResolver(List.of(
                    new DuplicataMercantilPricingStrategy(new BigDecimal("0.015")),
                    new ChequePreDatadoPricingStrategy(new BigDecimal("0.025")))));

    ReceivableTypeCatalogTest() {
        // cheque is deactivated in this catalog
        when(repository.findByActiveTrueOrderByCode())
                .thenReturn(List.of(
                        new ReceivableTypeDefinition(ReceivableType.DUPLICATA_MERCANTIL, "Duplicata Mercantil", true)));
    }

    @Test
    void listsActiveTypesWithTheSpreadOfTheirStrategy() {
        assertThat(catalog.activeTypes())
                .containsExactly(new ReceivableTypeInfo(
                        ReceivableType.DUPLICATA_MERCANTIL, "Duplicata Mercantil", new BigDecimal("0.01500000")));
    }

    @Test
    void resolvesActiveCodesInOrderWithASingleQuery() {
        assertThat(catalog.requireActive(List.of("DUPLICATA_MERCANTIL", "DUPLICATA_MERCANTIL")))
                .containsExactly(ReceivableType.DUPLICATA_MERCANTIL, ReceivableType.DUPLICATA_MERCANTIL);
        verify(repository, times(1)).findByActiveTrueOrderByCode();
    }

    @Test
    void rejectsUnknownCodes() {
        assertThatExceptionOfType(UnsupportedReceivableTypeException.class)
                .isThrownBy(() -> catalog.requireActive("NOTA_PROMISSORIA"))
                .withMessageContaining("NOTA_PROMISSORIA")
                .satisfies(ex -> assertThat(ex.code()).isEqualTo(ErrorCode.UNSUPPORTED_RECEIVABLE_TYPE));
    }

    @Test
    void rejectsKnownButInactiveTypes() {
        assertThatExceptionOfType(UnsupportedReceivableTypeException.class)
                .isThrownBy(() -> catalog.requireActive("CHEQUE_PRE_DATADO"));
    }
}
