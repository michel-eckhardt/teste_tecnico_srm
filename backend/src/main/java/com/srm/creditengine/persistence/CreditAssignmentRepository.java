package com.srm.creditengine.persistence;

import com.srm.creditengine.domain.assignment.CreditAssignment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CreditAssignmentRepository extends JpaRepository<CreditAssignment, UUID> {

    /** Loads the whole aggregate (assignor, receivables and their FX snapshot) in one round trip. */
    @EntityGraph(attributePaths = {"assignor", "receivables", "receivables.exchangeRate"})
    @Query("select a from CreditAssignment a where a.id = :id")
    Optional<CreditAssignment> findAggregateById(UUID id);

    @EntityGraph(attributePaths = {"assignor", "receivables", "receivables.exchangeRate"})
    Optional<CreditAssignment> findByIdempotencyKey(String idempotencyKey);
}
