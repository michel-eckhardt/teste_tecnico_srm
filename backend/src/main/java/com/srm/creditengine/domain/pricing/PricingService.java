package com.srm.creditengine.domain.pricing;

import com.srm.creditengine.domain.currency.CurrencyCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Pricing use cases that do not persist anything (operator simulation). */
@Service
public class PricingService {

    private final ReceivableTypeCatalog catalog;
    private final PricingEngine engine;

    public PricingService(ReceivableTypeCatalog catalog, PricingEngine engine) {
        this.catalog = catalog;
        this.engine = engine;
    }

    /** Prices a single receivable exactly as a credit assignment would, without storing it. */
    @Transactional(readOnly = true)
    public PricedReceivable simulate(
            String receivableType,
            BigDecimal faceValue,
            CurrencyCode faceCurrency,
            LocalDate dueDate,
            CurrencyCode paymentCurrency) {
        ReceivableType type = catalog.requireActive(receivableType);
        return engine.price(List.of(new ReceivableTerms(type, faceValue, faceCurrency, dueDate)), paymentCurrency)
                .getFirst();
    }
}
