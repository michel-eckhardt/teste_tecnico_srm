package com.srm.creditengine.persistence;

import com.srm.creditengine.domain.treasury.CashMovement;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashMovementRepository extends JpaRepository<CashMovement, UUID> {}
