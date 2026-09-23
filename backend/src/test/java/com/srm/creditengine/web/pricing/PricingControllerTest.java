package com.srm.creditengine.web.pricing;

import static com.srm.creditengine.domain.currency.CurrencyCode.BRL;
import static com.srm.creditengine.domain.currency.CurrencyCode.USD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srm.creditengine.domain.currency.CurrencyConversion;
import com.srm.creditengine.domain.currency.ExchangeRate;
import com.srm.creditengine.domain.currency.ExchangeRateSource;
import com.srm.creditengine.domain.pricing.InvalidDueDateException;
import com.srm.creditengine.domain.pricing.PricedReceivable;
import com.srm.creditengine.domain.pricing.PricingService;
import com.srm.creditengine.domain.pricing.ReceivableType;
import com.srm.creditengine.domain.pricing.ReceivableTypeCatalog;
import com.srm.creditengine.domain.pricing.ReceivableTypeInfo;
import com.srm.creditengine.domain.pricing.UnsupportedReceivableTypeException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PricingController.class)
class PricingControllerTest {

    private static final String VALID_REQUEST = """
            {"receivableType":"DUPLICATA_MERCANTIL","faceValue":"10000.00","faceCurrency":"BRL",
             "dueDate":"2026-12-22","paymentCurrency":"USD"}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PricingService pricingService;

    @MockitoBean
    private ReceivableTypeCatalog catalog;

    private static PricedReceivable pricedInUsd() {
        ExchangeRate usdBrl = ExchangeRate.of(
                USD,
                BRL,
                new BigDecimal("5.1322"),
                ExchangeRateSource.FRANKFURTER,
                LocalDate.of(2026, 9, 23),
                Instant.parse("2026-09-23T14:05:00Z"));
        return new PricedReceivable(
                ReceivableType.DUPLICATA_MERCANTIL,
                new BigDecimal("10000.00"),
                BRL,
                USD,
                LocalDate.of(2026, 9, 23),
                LocalDate.of(2026, 12, 22),
                90,
                new BigDecimal("3.00000000"),
                new BigDecimal("0.01000000"),
                new BigDecimal("0.01500000"),
                new BigDecimal("0.02500000"),
                new BigDecimal("9285.99"),
                new BigDecimal("714.01"),
                new CurrencyConversion(usdBrl, BRL, USD),
                new BigDecimal("1809.36"),
                new BigDecimal("1948.48"));
    }

    private org.springframework.test.web.servlet.ResultActions simulate(String json) throws Exception {
        return mvc.perform(post("/api/v1/pricing/simulations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void returnsTheContractRepresentationOfASimulation() throws Exception {
        when(pricingService.simulate(
                        eq("DUPLICATA_MERCANTIL"),
                        eq(new BigDecimal("10000.00")),
                        eq(BRL),
                        eq(LocalDate.of(2026, 12, 22)),
                        eq(USD)))
                .thenReturn(pricedInUsd());

        simulate(VALID_REQUEST)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receivableType").value("DUPLICATA_MERCANTIL"))
                .andExpect(jsonPath("$.faceValue").value("10000.00"))
                .andExpect(jsonPath("$.operationDate").value("2026-09-23"))
                .andExpect(jsonPath("$.termDays").value(90))
                .andExpect(jsonPath("$.termMonths").value("3.00000000"))
                .andExpect(jsonPath("$.baseRate").value("0.01000000"))
                .andExpect(jsonPath("$.spread").value("0.01500000"))
                .andExpect(jsonPath("$.discountRate").value("0.02500000"))
                .andExpect(jsonPath("$.presentValue").value("9285.99"))
                .andExpect(jsonPath("$.discount").value("714.01"))
                .andExpect(jsonPath("$.exchangeRate.base").value("USD"))
                .andExpect(jsonPath("$.exchangeRate.quote").value("BRL"))
                .andExpect(jsonPath("$.exchangeRate.rate").value("5.13220000"))
                .andExpect(jsonPath("$.exchangeRate.referenceDate").value("2026-09-23"))
                .andExpect(jsonPath("$.netAmount").value("1809.36"));
    }

    @ParameterizedTest(name = "faceValue {0} is rejected")
    @CsvSource({"\"0\"", "\"-10.00\"", "\"10.001\"", "\"1000000000.01\"", "null"})
    void rejectsInvalidFaceValues(String faceValue) throws Exception {
        simulate(VALID_REQUEST.replace("\"10000.00\"", faceValue))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("faceValue"));
        verifyNoInteractions(pricingService);
    }

    @Test
    void rejectsUnsupportedCurrenciesAsValidationErrors() throws Exception {
        simulate(VALID_REQUEST.replace("\"USD\"", "\"EUR\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("paymentCurrency"));
    }

    @Test
    void requiresEveryField() throws Exception {
        simulate("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(5));
    }

    @Test
    void unknownReceivableTypeIsABusinessRuleViolation() throws Exception {
        when(pricingService.simulate(anyString(), any(), any(), any(), any()))
                .thenThrow(new UnsupportedReceivableTypeException("NOTA_PROMISSORIA"));

        simulate(VALID_REQUEST.replace("DUPLICATA_MERCANTIL", "NOTA_PROMISSORIA"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_RECEIVABLE_TYPE"));
    }

    @Test
    void invalidDueDateIsABusinessRuleViolation() throws Exception {
        when(pricingService.simulate(anyString(), any(), any(), any(), any()))
                .thenThrow(new InvalidDueDateException("vencimento no passado"));

        simulate(VALID_REQUEST)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_DUE_DATE"))
                .andExpect(jsonPath("$.type").value("https://srm.com.br/problems/invalid-due-date"));
    }

    @Test
    void listsReceivableTypesWithTheirSpread() throws Exception {
        when(catalog.activeTypes())
                .thenReturn(List.of(new ReceivableTypeInfo(
                        ReceivableType.CHEQUE_PRE_DATADO, "Cheque Pré-datado", new BigDecimal("0.02500000"))));

        mvc.perform(get("/api/v1/receivable-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("CHEQUE_PRE_DATADO"))
                .andExpect(jsonPath("$[0].description").value("Cheque Pré-datado"))
                .andExpect(jsonPath("$[0].monthlySpread").value("0.02500000"));
    }
}
