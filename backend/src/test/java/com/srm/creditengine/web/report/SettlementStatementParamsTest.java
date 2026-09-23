package com.srm.creditengine.web.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.persistence.report.SettlementStatementSort;
import java.time.LocalDate;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class SettlementStatementParamsTest {

    private static final Pattern SORT = Pattern.compile(SettlementStatementParams.SORT_PATTERN);

    @Test
    void theValidationPatternAcceptsExactlyTheWhitelistedFields() {
        for (SettlementStatementSort.Field field : SettlementStatementSort.Field.values()) {
            for (String sort : new String[] {field.apiName(), field.apiName() + ",asc", field.apiName() + ",DESC"}) {
                assertThat(SORT.matcher(sort).matches()).as(sort).isTrue();
                assertThat(SettlementStatementSort.parse(sort)).as(sort).isPresent();
            }
        }
        assertThat(SORT.matcher("id,asc").matches()).isFalse();
    }

    @Test
    void appliesDefaults() {
        SettlementStatementParams params =
                new SettlementStatementParams(null, null, null, null, null, null, null, null);

        assertThat(params.pageOrDefault()).isZero();
        assertThat(params.sizeOrDefault()).isEqualTo(20);
        assertThat(params.sortOrDefault()).isEqualTo(SettlementStatementSort.DEFAULT);
        assertThat(params.isPeriodValid()).isTrue();
    }

    @Test
    void aPeriodMayStartAndEndOnTheSameDayButNotBackwards() {
        LocalDate day = LocalDate.of(2026, 1, 10);

        assertThat(new SettlementStatementParams(day, day, null, null, null, null, null, null).isPeriodValid())
                .isTrue();
        assertThat(new SettlementStatementParams(day, day.minusDays(1), null, null, null, null, null, null)
                        .isPeriodValid())
                .isFalse();
    }
}
