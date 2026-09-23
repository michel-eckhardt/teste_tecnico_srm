package com.srm.creditengine.domain.pricing;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Indexes every {@link PricingStrategy} bean by receivable type. The application fails to start if
 * a type has no strategy or more than one, instead of failing on the first request that needs it.
 */
@Component
public class PricingStrategyResolver {

    private final Map<ReceivableType, PricingStrategy> strategies;

    public PricingStrategyResolver(List<PricingStrategy> strategies) {
        Map<ReceivableType, PricingStrategy> byType = new EnumMap<>(ReceivableType.class);
        for (PricingStrategy strategy : strategies) {
            PricingStrategy previous = byType.putIfAbsent(strategy.type(), strategy);
            if (previous != null) {
                throw new IllegalStateException("Receivable type %s has two pricing strategies: %s and %s"
                        .formatted(
                                strategy.type(),
                                previous.getClass().getSimpleName(),
                                strategy.getClass().getSimpleName()));
            }
        }
        List<ReceivableType> missing = Arrays.stream(ReceivableType.values())
                .filter(type -> !byType.containsKey(type))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Receivable types without a pricing strategy: " + missing);
        }
        this.strategies = Collections.unmodifiableMap(byType);
    }

    public PricingStrategy resolve(ReceivableType type) {
        return strategies.get(type);
    }
}
