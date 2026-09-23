package com.srm.creditengine.persistence;

import com.srm.creditengine.domain.currency.Currency;
import com.srm.creditengine.domain.currency.CurrencyCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, CurrencyCode> {}
