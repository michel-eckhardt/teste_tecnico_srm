package com.srm.creditengine.integration.fx;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.creditengine.domain.currency.ExchangeRateSyncService;
import com.srm.creditengine.domain.currency.FxProviderUnavailableException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

class ExchangeRateSyncSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-09-23T12:00:00Z");

    private final ExchangeRateSyncService syncService = mock(ExchangeRateSyncService.class);
    private final TaskScheduler taskScheduler = mock(TaskScheduler.class);
    private final ExchangeRateSyncScheduler scheduler =
            new ExchangeRateSyncScheduler(syncService, taskScheduler, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void startupSyncRunsOnTheSchedulerThreadInsteadOfBlockingReadiness() {
        scheduler.synchronizeOnStartup();

        verify(taskScheduler).schedule(any(Runnable.class), eq(NOW));
    }

    @Test
    void providerOutageIsLoggedAndTheLastPersistedRateIsKept() {
        when(syncService.synchronize()).thenThrow(new FxProviderUnavailableException("timeout", null));

        assertThatNoException().isThrownBy(scheduler::synchronizeOnSchedule);
        verify(syncService).synchronize();
    }

    @Test
    void unexpectedFailuresNeverKillTheScheduler() {
        when(syncService.synchronize()).thenThrow(new IllegalStateException("boom"));

        assertThatNoException().isThrownBy(scheduler::synchronize);
        assertThatNoException().isThrownBy(scheduler::synchronize);
        verify(syncService, times(2)).synchronize();
    }
}
