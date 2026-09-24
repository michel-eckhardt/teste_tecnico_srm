package com.srm.creditengine.persistence;

import com.srm.creditengine.domain.currency.CurrencyCode;
import com.srm.creditengine.domain.currency.ExchangeRate;
import com.srm.creditengine.domain.currency.ExchangeRateSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {

    /** Most recent observation of a pair (served by {@code ix_exchange_rate_pair_latest}). */
    Optional<ExchangeRate> findFirstByBaseCurrencyAndQuoteCurrencyOrderByReferenceDateDescCreatedAtDesc(
            CurrencyCode base, CurrencyCode quote);

    /** Most recent observation of a pair ignoring one source (e.g. the bootstrap {@code SEED} rate). */
    Optional<ExchangeRate> findFirstByBaseCurrencyAndQuoteCurrencyAndSourceNotOrderByReferenceDateDescCreatedAtDesc(
            CurrencyCode base, CurrencyCode quote, ExchangeRateSource excluded);

    /** Identical observation already stored (used to keep provider synchronization idempotent). */
    Optional<ExchangeRate> findFirstByBaseCurrencyAndQuoteCurrencyAndReferenceDateAndSourceAndRate(
            CurrencyCode base, CurrencyCode quote, LocalDate referenceDate, ExchangeRateSource source, BigDecimal rate);

    Page<ExchangeRate> findByBaseCurrencyAndQuoteCurrency(CurrencyCode base, CurrencyCode quote, Pageable pageable);
}
