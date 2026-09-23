package com.srm.creditengine.domain.assignor;

import com.srm.creditengine.domain.common.BusinessClock;
import com.srm.creditengine.domain.common.ResourceNotFoundException;
import com.srm.creditengine.persistence.AssignorRepository;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssignorService {

    private static final Logger log = LoggerFactory.getLogger(AssignorService.class);

    private final AssignorRepository assignors;
    private final BusinessClock clock;

    public AssignorService(AssignorRepository assignors, BusinessClock clock) {
        this.assignors = assignors;
        this.clock = clock;
    }

    /**
     * @param document CNPJ digits only
     * @throws DuplicateAssignorException if the CNPJ is already registered (also under concurrency,
     *     thanks to the unique constraint)
     */
    @Transactional
    public Assignor register(String name, String document) {
        if (assignors.existsByDocument(document)) {
            throw new DuplicateAssignorException(document);
        }
        try {
            Assignor assignor = assignors.saveAndFlush(Assignor.register(name, document, clock.now()));
            log.info("Assignor registered: id={}", assignor.getId());
            return assignor;
        } catch (DataIntegrityViolationException concurrentDuplicate) {
            throw new DuplicateAssignorException(document);
        }
    }

    public Assignor get(UUID id) {
        return assignors.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cedente", id));
    }

    /**
     * Case-insensitive search by name fragment; when the term contains digits it also matches the
     * beginning of the CNPJ (typed with or without punctuation).
     */
    public Page<Assignor> search(@Nullable String term, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").and(Sort.by("id")));
        if (term == null || term.isBlank()) {
            return assignors.findAll(pageable);
        }
        String name = term.strip();
        String documentPrefix = name.replaceAll("\\D", "");
        return documentPrefix.isEmpty()
                ? assignors.findByNameContainingIgnoreCase(name, pageable)
                : assignors.findByNameContainingIgnoreCaseOrDocumentStartingWith(name, documentPrefix, pageable);
    }
}
