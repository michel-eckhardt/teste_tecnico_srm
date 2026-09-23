package com.srm.creditengine.persistence;

import com.srm.creditengine.domain.currency.CurrencyCode;
import com.srm.creditengine.domain.treasury.FundCashAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundCashAccountRepository extends JpaRepository<FundCashAccount, CurrencyCode> {}
