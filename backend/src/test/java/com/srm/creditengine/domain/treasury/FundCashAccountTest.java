package com.srm.creditengine.domain.treasury;

import static com.srm.creditengine.domain.currency.CurrencyCode.USD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import com.srm.creditengine.domain.common.ErrorCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FundCashAccountTest {

    private static final Instant OPENED = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant NOW = Instant.parse("2026-09-23T12:00:00Z");

    private final FundCashAccount account = new FundCashAccount(USD, new BigDecimal("1000.00"), OPENED);

    @Test
    void debitReducesTheBalance() {
        account.debit(new BigDecimal("250.25"), NOW);

        assertThat(account.getBalance()).isEqualTo(new BigDecimal("749.75"));
        assertThat(account.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void theWholeBalanceCanBeUsed() {
        account.debit(new BigDecimal("1000.00"), NOW);

        assertThat(account.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void refusesToOverdrawAndKeepsTheBalance() {
        assertThatExceptionOfType(InsufficientFundsException.class)
                .isThrownBy(() -> account.debit(new BigDecimal("1000.01"), NOW))
                .withMessageContaining("USD")
                .withMessageContaining("1000.01")
                .satisfies(ex -> assertThat(ex.code()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS));
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(account.getUpdatedAt()).isEqualTo(OPENED);
    }

    @Test
    void refusesNonPositiveDebits() {
        assertThatIllegalArgumentException().isThrownBy(() -> account.debit(BigDecimal.ZERO, NOW));
        assertThatIllegalArgumentException().isThrownBy(() -> account.debit(new BigDecimal("-1"), NOW));
    }

    @Test
    void settlementDebitIsALedgerEntryOfTheOperation() {
        UUID operation = UUID.randomUUID();

        CashMovement movement = CashMovement.settlementDebit(USD, operation, new BigDecimal("10.00"), NOW);

        assertThat(movement.getDirection()).isEqualTo(MovementDirection.DEBIT);
        assertThat(movement.getCreditAssignmentId()).isEqualTo(operation);
        assertThat(movement.getAmount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(movement.getCurrency()).isEqualTo(USD);
        assertThat(movement.getCreatedAt()).isEqualTo(NOW);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> CashMovement.settlementDebit(USD, operation, BigDecimal.ZERO, NOW));
    }
}
