package com.srm.creditengine.persistence;

import com.srm.creditengine.domain.pricing.ReceivableType;
import com.srm.creditengine.domain.pricing.ReceivableTypeDefinition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableTypeRepository extends JpaRepository<ReceivableTypeDefinition, ReceivableType> {

    List<ReceivableTypeDefinition> findByActiveTrueOrderByCode();
}
