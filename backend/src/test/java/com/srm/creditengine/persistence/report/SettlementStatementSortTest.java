package com.srm.creditengine.persistence.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SettlementStatementSortTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource(
            delimiter = '|',
            value = {
                "createdAt,desc      | ORDER BY ca.created_at DESC, ca.id DESC",
                "createdAt           | ORDER BY ca.created_at DESC, ca.id DESC",
                "createdAt,ASC       | ORDER BY ca.created_at ASC, ca.id ASC",
                "settledAt,asc       | ORDER BY ca.settled_at ASC NULLS LAST, ca.id ASC",
                "settledAt,desc      | ORDER BY ca.settled_at DESC NULLS LAST, ca.id DESC",
                "totalNetAmount,asc  | ORDER BY ca.total_net_amount ASC, ca.id ASC",
                "assignorName,desc   | ORDER BY a.name DESC, ca.id DESC"
            })
    void mapsWhitelistedFieldsToFixedSql(String parameter, String sql) {
        assertThat(SettlementStatementSort.parse(parameter))
                .get()
                .extracting(SettlementStatementSort::toSql)
                .isEqualTo(sql);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "password",
                "created_at",
                "createdAt,sideways",
                "createdAt,desc,extra",
                "createdAt;DROP TABLE credit_assignment",
                "a.name) --",
                ""
            })
    void rejectsAnythingOutsideTheWhitelist(String parameter) {
        assertThat(SettlementStatementSort.parse(parameter)).isEmpty();
    }

    @Test
    void defaultIsNewestFirst() {
        assertThat(SettlementStatementSort.DEFAULT.toSql()).isEqualTo("ORDER BY ca.created_at DESC, ca.id DESC");
        assertThat(Arrays.stream(SettlementStatementSort.Field.values()).map(SettlementStatementSort.Field::apiName))
                .containsExactly("createdAt", "settledAt", "totalNetAmount", "assignorName");
    }
}
