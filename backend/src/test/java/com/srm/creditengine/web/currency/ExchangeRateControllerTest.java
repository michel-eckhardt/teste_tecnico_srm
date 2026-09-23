package com.srm.creditengine.web.currency;

import static com.srm.creditengine.domain.currency.CurrencyCode.BRL;
import static com.srm.creditengine.domain.currency.CurrencyCode.USD;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srm.creditengine.domain.common.ResourceNotFoundException;
import com.srm.creditengine.domain.currency.Currency;
import com.srm.creditengine.domain.currency.ExchangeRateService;
import com.srm.creditengine.domain.currency.ExchangeRateSource;
import com.srm.creditengine.domain.currency.ExchangeRateSyncService;
import com.srm.creditengine.domain.currency.ExchangeRateView;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {ExchangeRateController.class, CurrencyController.class})
class ExchangeRateControllerTest {

    private static final UUID RATE_ID = UUID.fromString("01996f3c-0000-7000-8000-000000000001");
    private static final ExchangeRateView USD_BRL = new ExchangeRateView(
            RATE_ID,
            USD,
            BRL,
            new BigDecimal("5.13220000"),
            ExchangeRateSource.FRANKFURTER,
            LocalDate.of(2026, 9, 23),
            Instant.parse("2026-09-23T14:05:00Z"),
            false,
            false);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ExchangeRateService service;

    @MockitoBean
    private ExchangeRateSyncService syncService;

    @Test
    void listsCurrencies() throws Exception {
        when(service.currencies()).thenReturn(List.of(new Currency(BRL, "Real brasileiro", 2)));

        mvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("BRL"))
                .andExpect(jsonPath("$[0].name").value("Real brasileiro"))
                .andExpect(jsonPath("$[0].decimals").value(2));
    }

    @Test
    void latestReturnsTheRateAsADecimalString() throws Exception {
        when(service.latest(USD, BRL)).thenReturn(USD_BRL);

        mvc.perform(get("/api/v1/exchange-rates/latest").param("base", "USD").param("quote", "BRL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RATE_ID.toString()))
                .andExpect(jsonPath("$.rate").value("5.13220000"))
                .andExpect(jsonPath("$.source").value("FRANKFURTER"))
                .andExpect(jsonPath("$.referenceDate").value("2026-09-23"))
                .andExpect(jsonPath("$.createdAt").value("2026-09-23T14:05:00Z"))
                .andExpect(jsonPath("$.stale").value(false))
                .andExpect(jsonPath("$.derived").value(false));
    }

    @Test
    void latestIsNotFoundWhenThePairHasNoRate() throws Exception {
        when(service.latest(USD, BRL)).thenThrow(new ResourceNotFoundException("Nenhuma taxa de câmbio USD/BRL."));

        mvc.perform(get("/api/v1/exchange-rates/latest").param("base", "USD").param("quote", "BRL"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void latestRejectsTheSameCurrencyOnBothSides() throws Exception {
        mvc.perform(get("/api/v1/exchange-rates/latest").param("base", "USD").param("quote", "USD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].message").value("base e quote devem ser moedas diferentes"));
        verifyNoInteractions(service);
    }

    @Test
    void latestRejectsUnsupportedCurrenciesWithoutFrameworkMessages() throws Exception {
        mvc.perform(get("/api/v1/exchange-rates/latest").param("base", "EUR").param("quote", "BRL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("base"))
                .andExpect(jsonPath("$.errors[0].message").value("valor 'EUR' possui formato inválido"));
    }

    @Test
    void historyIsPaginated() throws Exception {
        when(service.history(USD, BRL, 1, 5)).thenReturn(new PageImpl<>(List.of(USD_BRL), PageRequest.of(1, 5), 6));

        mvc.perform(get("/api/v1/exchange-rates")
                        .param("base", "USD")
                        .param("quote", "BRL")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rate").value("5.13220000"))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(5))
                .andExpect(jsonPath("$.page.totalElements").value(6))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void historyRejectsPagesLargerThanTheLimit() throws Exception {
        mvc.perform(get("/api/v1/exchange-rates")
                        .param("base", "USD")
                        .param("quote", "BRL")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    @Test
    void manualRateIsCreatedWithLocation() throws Exception {
        when(service.registerManual(eq(USD), eq(BRL), any(BigDecimal.class), isNull()))
                .thenReturn(USD_BRL);

        mvc.perform(post("/api/v1/exchange-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"base\":\"USD\",\"quote\":\"BRL\",\"rate\":\"5.1322\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/exchange-rates/" + RATE_ID)))
                .andExpect(jsonPath("$.rate").value("5.13220000"));
    }

    @Test
    void manualRateValidatesEveryField() throws Exception {
        mvc.perform(post("/api/v1/exchange-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"base\":\"USD\",\"quote\":\"USD\",\"rate\":\"-1.123456789\","
                                + "\"referenceDate\":\"2999-01-01\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'rate')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'referenceDate')]").exists())
                .andExpect(
                        jsonPath("$.errors[?(@.field == 'distinctCurrencies')]").exists());
        verifyNoInteractions(service);
    }

    @Test
    void manualRateRequiresTheMandatoryFields() throws Exception {
        mvc.perform(post("/api/v1/exchange-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(3));
    }
}
