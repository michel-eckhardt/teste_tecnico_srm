package com.srm.creditengine.domain.common;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Business view of time: instants are UTC (as stored in TIMESTAMPTZ columns) while business dates
 * are evaluated in the configured business time zone (America/Sao_Paulo by default).
 */
@Component
public class BusinessClock {

    private final Clock clock;
    private final ZoneId zone;

    public BusinessClock(Clock clock, BusinessProperties properties) {
        this.clock = clock;
        this.zone = properties.zoneId();
    }

    /** Current instant truncated to the database precision (microseconds). */
    public Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    /** Current business date. */
    public LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), zone);
    }

    /** First instant of the given business date. */
    public Instant startOf(LocalDate date) {
        return date.atStartOfDay(zone).toInstant();
    }

    public ZoneId zone() {
        return zone;
    }
}
