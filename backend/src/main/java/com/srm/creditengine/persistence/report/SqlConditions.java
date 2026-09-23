package com.srm.creditengine.persistence.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Minimal query builder for optional filters: each condition is a constant SQL fragment bound to a
 * named parameter, added only when the filter value is present. Values always travel as bind
 * parameters, so no user input ever becomes SQL text.
 */
final class SqlConditions {

    private final List<String> conditions = new ArrayList<>();
    private final Map<String, Object> parameters = new LinkedHashMap<>();

    /**
     * @param fragment constant SQL fragment referencing {@code :parameter}
     */
    SqlConditions whenPresent(String fragment, String parameter, @Nullable Object value) {
        if (value != null) {
            conditions.add(fragment);
            parameters.put(parameter, value);
        }
        return this;
    }

    String where() {
        return conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions);
    }

    Map<String, Object> parameters() {
        return Collections.unmodifiableMap(parameters);
    }
}
