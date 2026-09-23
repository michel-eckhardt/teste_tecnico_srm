package com.srm.creditengine.domain.assignment;

import static com.srm.creditengine.domain.currency.CurrencyCode.BRL;
import static com.srm.creditengine.domain.currency.CurrencyCode.USD;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NewCreditAssignmentTest {

    private static final UUID ASSIGNOR = UUID.fromString("01996f3c-0000-7000-8000-000000000001");
    private static final LocalDate DUE = LocalDate.of(2026, 12, 22);

    private static NewCreditAssignment command(String face, LocalDate due) {
        return new NewCreditAssignment(
                ASSIGNOR,
                BRL,
                List.of(
                        new NewCreditAssignment.Item("DUPLICATA_MERCANTIL", new BigDecimal(face), BRL, due),
                        new NewCreditAssignment.Item("CHEQUE_PRE_DATADO", new BigDecimal("2500.00"), USD, due)));
    }

    @Test
    void fingerprintIsAHexSha256() {
        assertThat(command("10000.00", DUE).fingerprint()).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void numericallyEqualAmountsHaveTheSameFingerprint() {
        assertThat(command("10000.00", DUE).fingerprint())
                .isEqualTo(command("10000", DUE).fingerprint());
    }

    @Test
    void anyBusinessDifferenceChangesTheFingerprint() {
        String original = command("10000.00", DUE).fingerprint();

        assertThat(command("10000.01", DUE).fingerprint()).isNotEqualTo(original);
        assertThat(command("10000.00", DUE.plusDays(1)).fingerprint()).isNotEqualTo(original);
        assertThat(new NewCreditAssignment(
                                ASSIGNOR, USD, command("10000.00", DUE).receivables())
                        .fingerprint())
                .isNotEqualTo(original);
    }
}
