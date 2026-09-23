package com.srm.creditengine.domain.pricing;

import com.srm.creditengine.persistence.ReceivableTypeRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Receivable types accepted for new operations: a code must match a {@link ReceivableType} (hence a
 * pricing strategy) and be active in the {@code receivable_type} table.
 */
@Service
@Transactional(readOnly = true)
public class ReceivableTypeCatalog {

    private final ReceivableTypeRepository repository;
    private final PricingStrategyResolver strategies;

    public ReceivableTypeCatalog(ReceivableTypeRepository repository, PricingStrategyResolver strategies) {
        this.repository = repository;
        this.strategies = strategies;
    }

    public List<ReceivableTypeInfo> activeTypes() {
        return repository.findByActiveTrueOrderByCode().stream()
                .map(definition -> new ReceivableTypeInfo(
                        definition.getCode(),
                        definition.getDescription(),
                        strategies.resolve(definition.getCode()).nominalMonthlySpread()))
                .toList();
    }

    public ReceivableType requireActive(String code) {
        return requireActive(List.of(code)).getFirst();
    }

    /**
     * Resolves the codes of a batch with a single query, preserving their order.
     *
     * @throws UnsupportedReceivableTypeException for the first unknown or inactive code
     */
    public List<ReceivableType> requireActive(List<String> codes) {
        Set<ReceivableType> active = repository.findByActiveTrueOrderByCode().stream()
                .map(ReceivableTypeDefinition::getCode)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ReceivableType.class)));
        return codes.stream()
                .map(code -> ReceivableType.fromCode(code)
                        .filter(active::contains)
                        .orElseThrow(() -> new UnsupportedReceivableTypeException(code)))
                .toList();
    }
}
