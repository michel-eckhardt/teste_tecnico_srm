package com.srm.creditengine.persistence;

import com.srm.creditengine.domain.assignor.Assignor;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignorRepository extends JpaRepository<Assignor, UUID> {

    boolean existsByDocument(String document);

    /** Wildcards typed by the user are escaped by Spring Data, so "%" and "_" match literally. */
    Page<Assignor> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Assignor> findByNameContainingIgnoreCaseOrDocumentStartingWith(
            String name, String documentPrefix, Pageable pageable);
}
