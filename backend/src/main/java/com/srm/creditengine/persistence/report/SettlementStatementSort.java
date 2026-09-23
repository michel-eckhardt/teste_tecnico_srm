package com.srm.creditengine.persistence.report;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Whitelisted ordering of the settlement statement. Each API field maps to a fixed SQL expression:
 * user input selects one of these constants and is never concatenated into the query.
 *
 * @param field column to order by
 * @param ascending direction; ties are always broken by operation id in the same direction
 */
public record SettlementStatementSort(Field field, boolean ascending) {

    public static final SettlementStatementSort DEFAULT = new SettlementStatementSort(Field.CREATED_AT, false);

    public enum Field {
        CREATED_AT("createdAt", "ca.created_at", false),
        SETTLED_AT("settledAt", "ca.settled_at", true),
        TOTAL_NET_AMOUNT("totalNetAmount", "ca.total_net_amount", false),
        ASSIGNOR_NAME("assignorName", "a.name", false);

        private final String apiName;
        private final String column;
        private final boolean nullable;

        Field(String apiName, String column, boolean nullable) {
            this.apiName = apiName;
            this.column = column;
            this.nullable = nullable;
        }

        public String apiName() {
            return apiName;
        }

        static Optional<Field> fromApiName(String name) {
            return Arrays.stream(values())
                    .filter(field -> field.apiName.equals(name))
                    .findFirst();
        }
    }

    public SettlementStatementSort {
        Objects.requireNonNull(field, "field");
    }

    /**
     * Parses {@code field[,asc|desc]} (direction defaults to {@code desc}).
     *
     * @return empty for anything outside the whitelist
     */
    public static Optional<SettlementStatementSort> parse(String value) {
        String[] parts = value.split(",", -1);
        if (parts.length > 2) {
            return Optional.empty();
        }
        String direction = parts.length == 2 ? parts[1].strip().toLowerCase(Locale.ROOT) : "desc";
        if (!direction.equals("asc") && !direction.equals("desc")) {
            return Optional.empty();
        }
        return Field.fromApiName(parts[0].strip())
                .map(field -> new SettlementStatementSort(field, direction.equals("asc")));
    }

    /** {@code ORDER BY} clause built only from constants. */
    String toSql() {
        String direction = ascending ? "ASC" : "DESC";
        String nulls = field.nullable ? " NULLS LAST" : "";
        return "ORDER BY " + field.column + " " + direction + nulls + ", ca.id " + direction;
    }
}
