package com.srm.creditengine.integration.fx;

import com.srm.creditengine.domain.currency.ExchangeRateSyncService;
import com.srm.creditengine.domain.currency.FxProviderUnavailableException;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps the rate history fresh when {@code srm.fx.sync.enabled=true}: once right after startup and
 * then on {@code srm.fx.sync.cron}. A provider outage is only logged: pricing keeps using the last
 * persisted rate until it becomes stale, so the application never depends on the provider being up.
 */
@Component
@ConditionalOnBooleanProperty("srm.fx.sync.enabled")
class ExchangeRateSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateSyncScheduler.class);

    private final ExchangeRateSyncService syncService;
    private final TaskScheduler taskScheduler;
    private final Clock clock;

    ExchangeRateSyncScheduler(ExchangeRateSyncService syncService, TaskScheduler taskScheduler, Clock clock) {
        this.syncService = syncService;
        this.taskScheduler = taskScheduler;
        this.clock = clock;
    }

    /** Runs on the scheduler thread so a slow provider never delays application readiness. */
    @EventListener(ApplicationReadyEvent.class)
    void synchronizeOnStartup() {
        taskScheduler.schedule(this::synchronize, clock.instant());
    }

    @Scheduled(cron = "${srm.fx.sync.cron}", zone = "${srm.business.zone-id}")
    void synchronizeOnSchedule() {
        synchronize();
    }

    void synchronize() {
        try {
            syncService.synchronize();
        } catch (FxProviderUnavailableException unavailable) {
            log.warn("FX sync skipped, keeping the last persisted rates: {}", unavailable.getMessage());
        } catch (RuntimeException unexpected) {
            log.error("FX sync failed unexpectedly, keeping the last persisted rates", unexpected);
        }
    }
}
