package com.srm.creditengine.domain.assignor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.creditengine.domain.common.BusinessClock;
import com.srm.creditengine.domain.common.BusinessProperties;
import com.srm.creditengine.domain.common.ErrorCode;
import com.srm.creditengine.domain.common.ResourceNotFoundException;
import com.srm.creditengine.persistence.AssignorRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class AssignorServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-23T12:00:00Z");
    private static final String CNPJ = "11222333000181";

    private final AssignorRepository repository = mock(AssignorRepository.class);
    private final AssignorService service = new AssignorService(
            repository,
            new BusinessClock(
                    Clock.fixed(NOW, ZoneOffset.UTC), new BusinessProperties(ZoneId.of("America/Sao_Paulo"))));

    @Test
    void registersANewAssignor() {
        when(repository.saveAndFlush(any(Assignor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Assignor assignor = service.register("  ACME Indústria Ltda ", CNPJ);

        assertThat(assignor.getName()).isEqualTo("ACME Indústria Ltda");
        assertThat(assignor.getDocument()).isEqualTo(CNPJ);
        assertThat(assignor.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsAnAlreadyRegisteredCnpj() {
        when(repository.existsByDocument(CNPJ)).thenReturn(true);

        assertThatExceptionOfType(DuplicateAssignorException.class)
                .isThrownBy(() -> service.register("ACME", CNPJ))
                .satisfies(ex -> assertThat(ex.code()).isEqualTo(ErrorCode.DUPLICATE_ASSIGNOR));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void translatesTheUniqueConstraintOfAConcurrentRegistration() {
        when(repository.saveAndFlush(any(Assignor.class)))
                .thenThrow(new DataIntegrityViolationException("uk_assignor_document"));

        assertThatExceptionOfType(DuplicateAssignorException.class).isThrownBy(() -> service.register("ACME", CNPJ));
    }

    @Test
    void refusesMalformedDocumentsEvenIfTheWebLayerIsBypassed() {
        assertThatIllegalArgumentException().isThrownBy(() -> Assignor.register("ACME", "11.222.333/0001-81", NOW));
        assertThatIllegalArgumentException().isThrownBy(() -> Assignor.register(" ", CNPJ, NOW));
    }

    @Test
    void searchesByNameOnlyWhenTheTermHasNoDigits() {
        when(repository.findByNameContainingIgnoreCase(eq("acme"), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.search(" acme ", 0, 20);

        verify(repository).findByNameContainingIgnoreCase(eq("acme"), any(Pageable.class));
    }

    @Test
    void searchesByNameOrCnpjPrefixWhenTheTermHasDigits() {
        when(repository.findByNameContainingIgnoreCaseOrDocumentStartingWith(
                        eq("11.222"), eq("11222"), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.search("11.222", 0, 20);

        verify(repository)
                .findByNameContainingIgnoreCaseOrDocumentStartingWith(eq("11.222"), eq("11222"), any(Pageable.class));
    }

    @Test
    void listsEveryAssignorWithoutATerm() {
        when(repository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        service.search(null, 0, 20);

        verify(repository).findAll(any(Pageable.class));
    }

    @Test
    void getFailsWithNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> service.get(id));
    }
}
