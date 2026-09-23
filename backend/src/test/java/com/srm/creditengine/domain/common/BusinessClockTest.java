package com.srm.creditengine.domain.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class BusinessClockTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private static BusinessClock clockAt(String instant) {
        return new BusinessClock(
                Clock.fixed(Instant.parse(instant), ZoneOffset.UTC), new BusinessProperties(SAO_PAULO));
    }

    @Test
    void businessDateFollowsTheBusinessZoneNotUtc() {
        // 01:30 UTC on the 24th is still 22:30 of the 23rd in Sao Paulo (UTC-3)
        BusinessClock clock = clockAt("2026-09-24T01:30:00Z");

        assertThat(clock.today()).isEqualTo(LocalDate.of(2026, 9, 23));
    }

    @Test
    void startOfDayIsMidnightInTheBusinessZone() {
        BusinessClock clock = clockAt("2026-09-23T12:00:00Z");

        assertThat(clock.startOf(LocalDate.of(2026, 9, 23))).isEqualTo(Instant.parse("2026-09-23T03:00:00Z"));
        assertThat(clock.zone()).isEqualTo(SAO_PAULO);
    }

    @Test
    void nowIsTruncatedToDatabasePrecision() {
        BusinessClock clock = clockAt("2026-09-23T12:00:00.123456789Z");

        assertThat(clock.now()).isEqualTo(Instant.parse("2026-09-23T12:00:00.123456Z"));
    }
}
