package com.srm.creditengine.domain.treasury;

import com.srm.creditengine.persistence.FundCashAccountRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TreasuryService {

    private final FundCashAccountRepository accounts;

    public TreasuryService(FundCashAccountRepository accounts) {
        this.accounts = accounts;
    }

    public List<FundCashAccount> accounts() {
        return accounts.findAll(Sort.by("currency"));
    }
}
