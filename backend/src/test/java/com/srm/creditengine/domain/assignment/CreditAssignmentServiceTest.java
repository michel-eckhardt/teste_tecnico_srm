package com.srm.creditengine.domain.assignment;

import static com.srm.creditengine.domain.currency.CurrencyCode.BRL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.creditengine.domain.assignor.Assignor;
import com.srm.creditengine.domain.common.BusinessMetrics;
import com.srm.creditengine.domain.common.ResourceNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class CreditAssignmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-23T12:00:00Z");
    private static final NewCreditAssignment COMMAND = new NewCreditAssignment(
            UUID.randomUUID(),
            BRL,
            List.of(new NewCreditAssignment.Item(
                    "DUPLICATA_MERCANTIL", new BigDecimal("10000.00"), BRL, LocalDate.of(2026, 12, 22))));

    private final CreditAssignmentTransactions transactions = mock(CreditAssignmentTransactions.class);
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final CreditAssignmentService service =
            new CreditAssignmentService(transactions, new BusinessMetrics(meters));

    private static CreditAssignment storedWith(String requestHash) {
        return CreditAssignment.open(
                Assignor.register("ACME", "11222333000181", NOW),
                BRL,
                CreditAssignmentTest.contractExample(),
                new IdempotencyKey("key-1", requestHash),
                NOW);
    }

    @Test
    void createsWithoutIdempotencyKey() {
        CreditAssignment created = storedWith("x");
        when(transactions.open(COMMAND, null)).thenReturn(created);

        CreationResult result = service.create(COMMAND, null);

        assertThat(result.created()).isTrue();
        assertThat(result.assignment()).isSameAs(created);
        assertThat(meters.counter(BusinessMetrics.ASSIGNMENTS_CREATED, "payment_currency", "BRL")
                        .count())
                .isEqualTo(1);
    }

    @Test
    void storesTheKeyWithTheRequestFingerprint() {
        when(transactions.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(transactions.open(eq(COMMAND), any(IdempotencyKey.class))).thenReturn(storedWith(COMMAND.fingerprint()));

        CreationResult result = service.create(COMMAND, "key-1");

        assertThat(result.created()).isTrue();
        verify(transactions).open(COMMAND, new IdempotencyKey("key-1", COMMAND.fingerprint()));
    }

    @Test
    void replaysTheOriginalOperationForTheSameKeyAndPayload() {
        CreditAssignment original = storedWith(COMMAND.fingerprint());
        when(transactions.findByIdempotencyKey("key-1")).thenReturn(Optional.of(original));

        CreationResult result = service.create(COMMAND, "key-1");

        assertThat(result.created()).isFalse();
        assertThat(result.assignment()).isSameAs(original);
        verify(transactions, never()).open(any(), any());
        assertThat(meters.find(BusinessMetrics.ASSIGNMENTS_CREATED).counter()).isNull();
    }

    @Test
    void rejectsTheSameKeyWithAnotherPayload() {
        when(transactions.findByIdempotencyKey("key-1")).thenReturn(Optional.of(storedWith("another-payload")));

        assertThatExceptionOfType(IdempotencyKeyReusedException.class)
                .isThrownBy(() -> service.create(COMMAND, "key-1"))
                .withMessageContaining("key-1");
        verify(transactions, never()).open(any(), any());
    }

    @Test
    void aConcurrentRequestThatLostTheRaceAnswersWithTheWinner() {
        CreditAssignment winner = storedWith(COMMAND.fingerprint());
        when(transactions.findByIdempotencyKey("key-1")).thenReturn(Optional.empty(), Optional.of(winner));
        when(transactions.open(eq(COMMAND), any(IdempotencyKey.class)))
                .thenThrow(new DataIntegrityViolationException("uk_credit_assignment_idempotency_key"));

        CreationResult result = service.create(COMMAND, "key-1");

        assertThat(result.created()).isFalse();
        assertThat(result.assignment()).isSameAs(winner);
    }

    @Test
    void otherIntegrityViolationsAreNotMistakenForAReplay() {
        when(transactions.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(transactions.open(eq(COMMAND), any(IdempotencyKey.class)))
                .thenThrow(new DataIntegrityViolationException("ck_receivable_amounts"));

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> service.create(COMMAND, "key-1"));
    }

    @Test
    void getFailsWithNotFound() {
        UUID id = UUID.randomUUID();
        when(transactions.findAggregate(id)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> service.get(id));
        verify(transactions, never()).open(any(), isNull());
    }
}
